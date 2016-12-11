package com.jakduk.api.restcontroller.user;

import com.jakduk.api.common.util.JwtTokenUtils;
import com.jakduk.api.common.util.UserUtils;
import com.jakduk.api.common.vo.AttemptSocialUser;
import com.jakduk.api.common.vo.SocialProfile;
import com.jakduk.api.configuration.authentication.JakdukDetailsService;
import com.jakduk.api.configuration.authentication.SocialDetailService;
import com.jakduk.api.restcontroller.EmptyJsonResponse;
import com.jakduk.api.restcontroller.user.vo.LoginEmailUserForm;
import com.jakduk.api.restcontroller.user.vo.LoginSocialUserForm;
import com.jakduk.api.configuration.authentication.user.CommonPrincipal;
import com.jakduk.api.configuration.authentication.user.JakdukUserDetail;
import com.jakduk.api.configuration.authentication.user.SocialUserDetail;
import com.jakduk.core.common.CoreConst;
import com.jakduk.core.exception.ServiceExceptionCode;
import com.jakduk.core.exception.ServiceException;
import com.jakduk.core.model.db.User;
import com.jakduk.core.model.etc.AuthUserProfile;
import com.jakduk.core.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mobile.device.Device;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Objects;

/**
 * @author pyohwan
 * 16. 6. 29 오전 12:27
 */

@Slf4j
@Api(tags = "Authentication", description = "인증 API")
@RestController
@RequestMapping("/api")
public class AuthRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JakdukDetailsService jakdukDetailsService;

    @Autowired
    private SocialDetailService socialDetailService;

    @Value("${jwt.token.header}")
    private String tokenHeader;

    @Value("${jwt.token.attempted.header}")
    private String attemptedTokenHeader;

    @ApiOperation(value = "이메일 기반 로그인", produces = "application/json", response = EmptyJsonResponse.class)
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public EmptyJsonResponse loginSocialUser(@RequestBody LoginEmailUserForm form,
                                             @ApiIgnore Device device,
                                             HttpServletResponse response) {

        // Perform the authentication
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        form.getUsername(),
                        form.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Reload password post-authentication so we can generate token
        JakdukUserDetail userDetails = (JakdukUserDetail) jakdukDetailsService.loadUserByUsername(form.getUsername());

        String token = jwtTokenUtils.generateToken(new CommonPrincipal(userDetails), device);

        response.setHeader(tokenHeader, token);

        return EmptyJsonResponse.newInstance();
    }

    @ApiOperation(value = "JWT 토큰 갱신", produces = "application/json", response = EmptyJsonResponse.class)
    @RequestMapping(value = "/auth/refresh", method = RequestMethod.GET)
    public EmptyJsonResponse refreshAndGetAuthenticationToken(HttpServletRequest request,
                                                              HttpServletResponse response) {

        String token = request.getHeader(tokenHeader);

        if (jwtTokenUtils.canTokenBeRefreshed(token)) {
            String refreshedToken = jwtTokenUtils.refreshToken(token);
            response.setHeader(tokenHeader, refreshedToken);

            return EmptyJsonResponse.newInstance();
        } else {
            throw new ServiceException(ServiceExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    @ApiOperation(value = "SNS 기반 로그인", produces = "application/json", response = EmptyJsonResponse.class)
    @RequestMapping(value = "/login/social/{providerId}", method = RequestMethod.POST)
    public EmptyJsonResponse loginSocialUser(@PathVariable String providerId,
                                             @Valid @RequestBody LoginSocialUserForm form,
                                             Device device,
                                             HttpServletResponse response) {

        CoreConst.ACCOUNT_TYPE convertProviderId = CoreConst.ACCOUNT_TYPE.valueOf(providerId.toUpperCase());
        SocialProfile socialProfile = null;

        switch (convertProviderId) {
            case DAUM:
                socialProfile = userUtils.getDaumProfile(form.getAccessToken());
                break;
            case FACEBOOK:
                socialProfile = userUtils.getFacebookProfile(form.getAccessToken());
                break;
        }

        assert socialProfile != null;
        User existUser = userService.findOneByProviderIdAndProviderUserId(convertProviderId, socialProfile.getId());

        // 로그인 처리.
        if (! ObjectUtils.isEmpty(existUser)) {

            SocialUserDetail userDetails = (SocialUserDetail) socialDetailService.loadUserByUsername(existUser.getEmail());
            String token = jwtTokenUtils.generateToken(new CommonPrincipal(userDetails), device);

            response.setHeader(tokenHeader, token);

            return EmptyJsonResponse.newInstance();
        }

        // 신규 가입.
        AttemptSocialUser attemptSocialUser = new AttemptSocialUser();
        attemptSocialUser.setUsername(socialProfile.getNickname());
        attemptSocialUser.setProviderId(convertProviderId);
        attemptSocialUser.setProviderUserId(socialProfile.getId());

        // Daum은 이메일을 안 알려준다.
        if (! ObjectUtils.isEmpty(socialProfile.getEmail()))
            attemptSocialUser.setEmail(socialProfile.getEmail());

        String attemptedToken = jwtTokenUtils.generateAttemptedToken(attemptSocialUser);

        response.setHeader(attemptedTokenHeader, attemptedToken);

        throw new ServiceException(ServiceExceptionCode.NOT_REGISTER_WITH_SNS);
    }

    @ApiOperation(value = "SNS 기반 회원 가입시 필요한 회원 프로필 정보", produces = "application/json", response = AttemptSocialUser.class)
    @RequestMapping(value = "/social/attempt", method = RequestMethod.GET)
    public AttemptSocialUser getSocialAttemptedUser(@RequestHeader(value = "x-attempt-token") String attemptedToken) {

        if (! jwtTokenUtils.isValidateToken(attemptedToken))
            throw new ServiceException(ServiceExceptionCode.EXPIRATION_TOKEN);

        return jwtTokenUtils.getAttemptedFromToken(attemptedToken);
    }

    @ApiOperation(value = "JWT 토큰 속 프로필 정보", produces = "application/json", response = AuthUserProfile.class)
    @RequestMapping(value = "/auth/user", method = RequestMethod.GET)
    public AuthUserProfile getMyProfile() {

        AuthUserProfile authUserProfile = UserUtils.getAuthUserProfile();

        if (Objects.isNull(authUserProfile))
            throw new ServiceException(ServiceExceptionCode.UNAUTHORIZED_ACCESS);

        return authUserProfile;
    }
}