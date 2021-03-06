package com.jakduk.api.auth;

import com.jakduk.api.WithMockJakdukUser;
import com.jakduk.api.common.Constants;
import com.jakduk.api.common.util.AuthUtils;
import com.jakduk.api.common.util.ObjectMapperUtils;
import com.jakduk.api.configuration.security.JakdukAuthority;
import com.jakduk.api.model.db.User;
import com.jakduk.api.restcontroller.AuthRestController;
import com.jakduk.api.restcontroller.vo.EmptyJsonResponse;
import com.jakduk.api.restcontroller.vo.user.AttemptSocialUser;
import com.jakduk.api.restcontroller.vo.user.LoginSocialUserForm;
import com.jakduk.api.restcontroller.vo.user.SessionUser;
import com.jakduk.api.restcontroller.vo.user.SocialProfile;
import com.jakduk.api.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest({AuthRestController.class, AuthMvcTests.Controller.class})
@AutoConfigureRestDocs(outputDir = "build/snippets")
public class AuthMvcTests {

    @Autowired
    private MockMvc mvc;

    @MockBean private RestTemplateBuilder restTemplateBuilder;
    @MockBean private UserService userService;
    @MockBean private AuthUtils authUtils;
    @MockBean private AuthenticationManager authenticationManager;

    private SocialProfile socialProfile;
    private Constants.ACCOUNT_TYPE providerId;

    @Before
    public void setup() {
        providerId = Constants.ACCOUNT_TYPE.DAUM;

        socialProfile = new SocialProfile();
        socialProfile.setId("abc123");
        socialProfile.setNickname("daumUser01");
        socialProfile.setEmail("test17@test.com");
        socialProfile.setSmallPictureUrl("https://img1.daumcdn.net/thumb/R55x55/?fname=http%3A%2F%2Ftwg.tset.daumcdn.net%2Fprofile%2F6enovyMT1pI0&t=1507478752861");
        socialProfile.setLargePictureUrl("https://img1.daumcdn.net/thumb/R158x158/?fname=http%3A%2F%2Ftwg.tset.daumcdn.net%2Fprofile%2F6enovyMT1pI0&t=1507478752861");
    }

    @Test
    @WithMockUser
    public void formLoginTest() throws Exception {
        mvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("username", "test07@test.com")
                        .param("password", "1111")
                        .param("remember-me", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("JSESSIONID"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(ObjectMapperUtils.writeValueAsString(EmptyJsonResponse.newInstance())))
                .andDo(
                        document("login-jakduk-user",
                                requestParameters(
                                        parameterWithName("username").description("이메일 주소"),
                                        parameterWithName("password").description("비밀번호"),
                                        parameterWithName("remember-me").description("(optional) 로그인 유지 여부. 1 or 0"),
                                        parameterWithName("_csrf").description("CSRF 토큰")
                                ),
                                responseHeaders(
                                        headerWithName("Set-Cookie").description("인증 쿠키. value는 JSESSIONID=키값").optional()
                                )
                        ));
    }

    @Test
    @WithMockUser
    public void snsLoginTest() throws Exception {

        Map<String, Object> requestForm = new HashMap<String, Object>() {{
            put("accessToken", "baada13b7df9af000fa20355bf07b25f808940ab69dd7f32b6c009efdd0f6d29");
        }};

        when(authUtils.getDaumProfile(anyString()))
                .thenReturn(socialProfile);

        Optional<User> optUser = Optional.of(
                new User() {{
                    setId("58b2dbf1d6d83b04bf365277");
                    setEmail("test07@test.com");
                    setUsername("생글이");
                    setProviderId(providerId);
                    setProviderUserId(socialProfile.getId());
                    setRoles(Arrays.asList(JakdukAuthority.ROLE_USER_01.getCode()));
                    setAbout("안녕하세요.");
                }}
        );

        when(userService.findOneByProviderIdAndProviderUserId(any(Constants.ACCOUNT_TYPE.class), anyString()))
                .thenReturn(optUser);

        ConstraintDescriptions loginSocialUserConstraints = new ConstraintDescriptions(LoginSocialUserForm.class);

        mvc.perform(
                post("/api/auth/login/{providerId}", providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(ObjectMapperUtils.writeValueAsString(requestForm))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(ObjectMapperUtils.writeValueAsString(EmptyJsonResponse.newInstance())))
                .andDo(
                        document("login-sns-user",
                                pathParameters(
                                        parameterWithName("providerId").description("SNS 분류 " +
                                                Stream.of(Constants.ACCOUNT_TYPE.values())
                                                        .filter(accountType -> ! accountType.equals(Constants.ACCOUNT_TYPE.JAKDUK))
                                                        .map(Enum::name)
                                                        .map(String::toLowerCase)
                                                        .collect(Collectors.toList())
                                        )
                                ),
                                requestFields(
                                        fieldWithPath("accessToken").type(JsonFieldType.STRING).description("OAuth의 Access Token " +
                                                loginSocialUserConstraints.descriptionsForProperty("accessToken"))
                                ),
                                responseHeaders(
                                        headerWithName("Set-Cookie").description("인증 쿠키. value는 JSESSIONID=키값").optional()
                                )
                        ));
    }

    @Test
    @WithMockUser
    public void getAttemptSocialUserTest() throws Exception {

        AttemptSocialUser expectAttemptSocialUser = new AttemptSocialUser();
        expectAttemptSocialUser.setEmail(socialProfile.getEmail());
        expectAttemptSocialUser.setUsername(socialProfile.getNickname());
        expectAttemptSocialUser.setProviderId(providerId);
        expectAttemptSocialUser.setProviderUserId(socialProfile.getId());
        expectAttemptSocialUser.setExternalLargePictureUrl(socialProfile.getLargePictureUrl());
        expectAttemptSocialUser.setExternalSmallPictureUrl(socialProfile.getSmallPictureUrl());

        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(Constants.PROVIDER_SIGNIN_ATTEMPT_SESSION_ATTRIBUTE, expectAttemptSocialUser);

        mvc.perform(
                get("/api/auth/user/attempt")
                        .header("Cookie", "JSESSIONID=3F0E029648484BEAEF6B5C3578164E99")
                        .sessionAttrs(sessionAttributes)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(ObjectMapperUtils.writeValueAsString(expectAttemptSocialUser)))
                .andDo(
                        document("get-attempt-social-session-user",
                                requestHeaders(
                                        headerWithName("Cookie").description("인증 쿠키. value는 JSESSIONID=키값")
                                ),
                                responseFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("Provider에서 제공한 이메일 주소"),
                                        fieldWithPath("username").type(JsonFieldType.STRING).description("Provider에서 제공한 별명"),
                                        fieldWithPath("providerId").type(JsonFieldType.STRING).description("SNS 분류 " +
                                                        Stream.of(Constants.ACCOUNT_TYPE.values())
                                                                .filter(accountType -> ! accountType.equals(Constants.ACCOUNT_TYPE.JAKDUK))
                                                                .map(Enum::name)
                                                                .collect(Collectors.toList())
                                                ),
                                        fieldWithPath("providerUserId").type(JsonFieldType.STRING).description("Provider에서 제공한 사용자 ID"),
                                        fieldWithPath("externalLargePictureUrl").type(JsonFieldType.STRING).description("Provider에서 제공한 큰 사진 URL"),
                                        fieldWithPath("externalSmallPictureUrl").type(JsonFieldType.STRING).description("Provider에서 제공한 작은 사진 URL")
                                )
                        ));
    }

    @Test
    @WithMockUser
    public void logoutTest() throws Exception {
        mvc.perform(
                post("/api/auth/logout")
                        .cookie(new Cookie("JSESSIONID", "3F0E029648484BEAEF6B5C3578164E99"))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(ObjectMapperUtils.writeValueAsString(EmptyJsonResponse.newInstance())))
                .andDo(
                        document("logout",
                                requestHeaders(
                                        headerWithName("Cookie").optional().description("인증 쿠키. value는 JSESSIONID=키값")
                                )
                        ));
    }

    @Test
    @WithMockJakdukUser
    public void getMySessionProfileTest() throws Exception {

        SessionUser expectResponse = AuthUtils.getAuthUserProfile();

        mvc.perform(
                get("/api/auth/user")
                        .header("Cookie", "JSESSIONID=3F0E029648484BEAEF6B5C3578164E99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(ObjectMapperUtils.writeValueAsString(expectResponse)))
                .andDo(
                        document("get-my-session-user",
                                requestHeaders(
                                        headerWithName("Cookie").description("인증 쿠키. value는 JSESSIONID=키값")
                                ),
                                responseFields(
                                        fieldWithPath("id").type(JsonFieldType.STRING).description("회원 ID"),
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 주소"),
                                        fieldWithPath("username").type(JsonFieldType.STRING).description("별명"),
                                        fieldWithPath("providerId").type(JsonFieldType.STRING).description("계정 분류 " +
                                                Stream.of(Constants.ACCOUNT_TYPE.values()).map(Enum::name).collect(Collectors.toList())),
                                        fieldWithPath("roles").type(JsonFieldType.ARRAY).description("권한 목록 " +
                                                Stream.of(JakdukAuthority.values()).map(Enum::name).collect(Collectors.toList())),
                                        fieldWithPath("picture").type(JsonFieldType.OBJECT).description("회원 사진"),
                                        fieldWithPath("picture.id").type(JsonFieldType.STRING).description("회원 사진 ID"),
                                        fieldWithPath("picture.smallPictureUrl").type(JsonFieldType.STRING).description("회원 작은 사진 URL"),
                                        fieldWithPath("picture.largePictureUrl").type(JsonFieldType.STRING).description("회원 큰 사진 URL")
                                )
                        ));
    }

    @RestController
    static class Controller {

        @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        public EmptyJsonResponse formLogin(HttpServletResponse servletResponse) {
            servletResponse.addCookie(new Cookie("JSESSIONID", "3F0E029648484BEAEF6B5C3578164E99"));
            return EmptyJsonResponse.newInstance();
        }

        @PostMapping(value = "/api/auth/logout")
        public EmptyJsonResponse logout() {
            return EmptyJsonResponse.newInstance();
        }
    }
}
