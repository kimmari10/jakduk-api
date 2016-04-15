package com.jakduk.controller.session;

import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


import com.jakduk.model.web.SocialUserForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.LocaleResolver;

import com.jakduk.service.CommonService;
import com.jakduk.service.UserService;

/**
 * @author <a href="mailto:phjang1983@daum.net">Jang,Pyohwan</a>
 * @company  : http://jakduk.com
 * @date     : 2014. 11. 2.
 * @desc     :
 */

@Controller
@Slf4j
@RequestMapping("/oauth")
@SessionAttributes({"SocialUserForm", "footballClubs"})
public class OAuthProfileUpdateController {
	
	@Autowired
	private CommonService commonService;
	
	@Autowired
	private UserService userService;
	
	@Resource
	LocaleResolver localeResolver;

	@RequestMapping(value = "/profile/update", method = RequestMethod.GET)
	public String profileUpdate(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(required = false) String lang,
			Model model) {
		
		Locale locale = localeResolver.resolveLocale(request);
		String language = commonService.getLanguageCode(locale, lang);
		
		userService.getOAuthProfileUpdate(model, language);
		
		return "oauth/profileUpdate";
	}
	
	@RequestMapping(value = "/profile/update", method = RequestMethod.POST)
	public String profileUpdate(@Valid SocialUserForm socialUserForm, BindingResult result, SessionStatus sessionStatus) {
		
		if (result.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("result=" + result);
			}
			return "oauth/write";
		}
		
		userService.checkOAuthProfileUpdate(socialUserForm, result);
		
		if (result.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("result=" + result);
			}
			return "oauth/write";
		}
		
		userService.oAuthProfileUpdate(socialUserForm);
		sessionStatus.setComplete();
		
		return "redirect:/oauth/profile?status=1";
	}

}
