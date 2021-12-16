package com.krishagni.catissueplus.core.common.util;

import java.io.File;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;


import com.krishagni.catissueplus.core.common.service.EmailService;

@Configurable
public class EmailUtil {

	private static final LogUtil logger = LogUtil.getLogger(EmailUtil.class);

	private static EmailUtil instance = null;

	@Autowired
	private EmailService emailSvc;

	public static EmailUtil getInstance() {
		if (instance == null || instance.emailSvc == null) {
			instance = new EmailUtil();
		}

		return instance;
	}


	public boolean sendEmail(String emailTmplKey, String[] to, File[] attachments, Map<String, Object> props) {
		if (emailSvc == null) {
			logger.warn("Email subsystem not yet initialised. Couldn't send email: " + emailTmplKey);
			return false;
		}

		return emailSvc.sendEmail(emailTmplKey, to, attachments, props);
	}

	public boolean sendEmail(String tmplKey, String body, String[] to, Map<String, Object> props) {
		return sendEmail(tmplKey, tmplKey, body, to, props);
	}

	public boolean sendEmail(String tmplKey, String subject, String body, String[] to, Map<String, Object> props) {
		if (emailSvc == null) {
			logger.warn("Email subsystem not yet initialised. Couldn't send email: " + tmplKey);
			return false;
		}

		return emailSvc.sendEmail(tmplKey, subject, body, to, props);
	}
}
