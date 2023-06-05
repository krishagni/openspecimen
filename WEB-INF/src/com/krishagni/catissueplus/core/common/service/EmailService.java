package com.krishagni.catissueplus.core.common.service;

import java.io.File;
import java.util.Map;

import com.krishagni.catissueplus.core.common.domain.Email;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public interface EmailService {
	boolean sendEmail(String emailTmplKey, String[] to, Map<String, Object> props);

	boolean sendEmail(String emailTmplKey, String[] to, File[] attachments, Map<String, Object> props);
	
	boolean sendEmail(String emailTmplKey, String[] to, String[] bcc, File[] attachments, Map<String, Object> props);

	boolean sendEmail(String subjectKey, String emailTmpl, String[] to, Map<String, Object> props);

	boolean sendEmail(String emailTmplKey, String tmplSubj, String tmplContent, String[] to, Map<String, Object> props);
	
	boolean sendEmail(Email mail);

	boolean sendEmail(Email mail, Map<String, Object> props);

	void registerProcessor(EmailProcessor processor);

	void sendTestEmail();

	Email getEmail(String emailTmplKey, String tmplSubj, String tmplContent, String[] to, String[] bcc, File[] attachments, Map<String, Object> props);
}
