package com.krishagni.catissueplus.core.common.service.impl;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.Email;
import com.krishagni.catissueplus.core.common.domain.factory.EmailErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.service.EmailProcessor;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.service.TemplateService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class EmailServiceImpl implements EmailService, ConfigChangeListener, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(EmailServiceImpl.class);
	
	private static final String MODULE = "email";
	
	private static final String TEMPLATE_SOURCE = "email-templates/";
	
	private static final String BASE_TMPL = "baseTemplate";
	
	private static final String FOOTER_TMPL = "footer";

	private JavaMailSender mailSender;
	
	private TemplateService templateService;
	
	private ThreadPoolTaskExecutor taskExecutor;
	
	private ConfigurationService cfgSvc;

	private DaoFactory daoFactory;

	private ImapMailReceiver mailReceiver;

	private ScheduledFuture<?> receiverFuture;

	private List<EmailProcessor> processors = new ArrayList<>();

	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public void onConfigChange(String name, String value) {
		initializeMailSender();

		if (StringUtils.isBlank(name) ||
			name.equals("imap_server_host") ||
			name.equals("imap_server_port") ||
			name.equals("imap_poll_interval")) {
			initializeMailReceiver();
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		initializeMailSender();
		initializeMailReceiver();
		cfgSvc.registerChangeListener(MODULE, this);		
	}
	
	private void initializeMailSender() {
		try {
			JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
			mailSender.setUsername(getAccountId());
			mailSender.setPassword(getAccountPassword());
			mailSender.setHost(getSmtpHost());
			mailSender.setPort(getSmtpPort());

			Properties props = new Properties();
			props.put("mail.smtp.timeout", 10000);
			props.put("mail.smtp.connectiontimeout", 10000);

			String startTlsEnabled = getStartTlsEnabled();
			String authEnabled = getSmtpAuthEnabled();
			if (StringUtils.isNotBlank(startTlsEnabled) && StringUtils.isNotBlank(authEnabled)) {
				props.put("mail.smtp.starttls.enable", startTlsEnabled);
				props.put("mail.smtp.auth", authEnabled);
			}

			mailSender.setJavaMailProperties(props);
			this.mailSender = mailSender;
		} catch (Exception e) {
			logger.error("Error initialising e-mail sender", e);
		}
	}
	
	@Override
	public boolean sendEmail(String emailTmplKey, String[] to, Map<String, Object> props) {
		return sendEmail(emailTmplKey, to, null, props);
	}

	@Override
	public boolean sendEmail(String emailTmplKey, String[] to, File[] attachments, Map<String, Object> props) {
		return sendEmail(emailTmplKey, to, null, attachments, props);
	}

	@Override
	public boolean sendEmail(String emailTmplKey, String[] to, String[] bcc, File[] attachments, Map<String, Object> props) {
		return sendEmail(emailTmplKey, null, null, to, bcc, attachments, props);
	}

	@Override
	public boolean sendEmail(String emailTmplKey, String emailTmpl, String[] to, Map<String, Object> props) {
		return sendEmail(emailTmplKey, null, emailTmpl, to, null, null, props);
	}

	@Override
	public boolean sendEmail(String emailTmplKey, String tmplSubj, String tmplContent, String[] to, Map<String, Object> props) {
		return sendEmail(emailTmplKey, tmplSubj, tmplContent, to, null , null, props);
	}

	@Override
	public boolean sendEmail(Email mail) {
		return sendEmail(mail, null);
	}

	@Override
	public boolean sendEmail(Email mail, Map<String, Object> props) {
		try {
			if (!isEmailNotifEnabled()) {
				logger.debug("Email notification is disabled. Not sending email: " + mail.getSubject());
				return false;
			}

			boolean ignoreDnd = (Boolean) props.getOrDefault("ignoreDnd", false);
			String[] toRcpts = filterEmailIds("To", mail.getToAddress(), ignoreDnd);
			if (toRcpts.length == 0) {
				logger.info("Not sending email, as there are no recipients in the To field");
				return false;
			}

			mail.setToAddress(toRcpts);
			mail.setBccAddress(filterEmailIds("Bcc", mail.getBccAddress(), ignoreDnd));
			mail.setCcAddress(filterEmailIds("Cc", mail.getCcAddress(), ignoreDnd));

			MimeMessage mimeMessage = createMessage(mail, props);
			SendMailTask sendTask = new SendMailTask(mimeMessage);
			boolean result = true;
			if (!(Boolean) props.getOrDefault("$synchronous", false)) {
				logger.info("Invoking task executor to send the e-mail asynchronously: " + mimeMessage.getSubject());
				taskExecutor.submit(sendTask);
				result = true;
			} else {
				logger.warn("Sending e-mail synchronously: " + mimeMessage.getSubject());
				sendTask.run();
				if (StringUtils.isNotBlank(sendTask.getError())) {
					props.put("$error", sendTask.getError());
					result = false;
				}
			}

			return result;
		} catch (Exception e) {
			logger.error("Error sending e-mail", e);
			props.put("$error", e.getMessage());
			return false;
		}
	}

	@Override
	public void registerProcessor(EmailProcessor processor) {
		if (processors.contains(processor)) {
			return;
		}

		processors.add(processor);
		if (processors.size() == 1) {
			initializeMailReceiver();
		}
	}

	@Override
	public void sendTestEmail() {
		if (!AuthUtil.isAdmin()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
		}

		if (!isEmailNotifEnabled()) {
			throw OpenSpecimenException.userError(EmailErrorCode.NOTIFS_ARE_DISABLED);
		}

		if (StringUtils.isEmpty(getAdminEmailId())) {
			throw OpenSpecimenException.userError(EmailErrorCode.ADMIN_EMAIL_REQ);
		}

		String[] adminEmailId = new String[] {getAdminEmailId()};
		Map<String, Object> props = new HashMap<>();
		props.put("$synchronous", true);
		boolean status = sendEmail("test_email", null, null, adminEmailId, null, null, props);
		if (!status) {
			throw OpenSpecimenException.userError(EmailErrorCode.UNABLE_TO_SEND, props.get("$error"));
		}
	}

	@Override
	public Email getEmail(String tmplKey, String tmplSubj, String tmplContent, String[] to, String[] bcc, File[] attachments, Map<String, Object> props) {
		if (props == null) {
			props = new HashMap<>();
		}

		String adminEmailId = getAdminEmailId();
		if (StringUtils.isNotBlank(tmplContent)) {
			props.put("templateContent", tmplContent);
		} else {
			props.put("template", getTemplate(tmplKey));
		}

		props.put("footer", getFooterTmpl());
		props.put("appUrl", getAppUrl());
		props.put("adminEmailAddress", adminEmailId);
		props.put("adminPhone", cfgSvc.getStrSetting("email", "admin_phone_no", "Not Specified"));
		props.put("urlEncoder", URLEncoder.class);
		String subject = StringUtils.isNotBlank(tmplSubj) ? tmplSubj : getSubject(tmplKey, (Object[]) props.get("$subject"));
		String content = templateService.render(getBaseTmpl(), props);

		Email email = new Email();
		email.setSubject(subject);
		email.setBody(content);
		email.setToAddress(to);
		email.setBccAddress(bcc);
		email.setAttachments(attachments);

		boolean ccAdmin = BooleanUtils.toBooleanDefaultIfNull((Boolean) props.get("ccAdmin"), true);
		if (ccAdmin) {
			email.setCcAddress(new String[] { adminEmailId });
		}

		return email;
	}

	private boolean sendEmail(String tmplKey, String tmplSubj, String tmplContent, String[] to, String[] bcc, File[] attachments, Map<String, Object> props) {
		if (!isEmailNotifEnabled()) {
			logger.info("Notifications disabled at the system level");
			return false;
		}

		boolean emailEnabled = cfgSvc.getBoolSetting("notifications", "email_" + tmplKey, true);
		if (!emailEnabled) {
			logger.info("Notifications disabled for: " + tmplKey);
			return false;
		}

		if (props == null) {
			props = new HashMap<>();
		}

		Email email = getEmail(tmplKey, tmplSubj, tmplContent, to, bcc, attachments, props);
		return sendEmail(email, props);
	}

	private String getTemplate(String tmplKey) {
		String localeTmpl = TEMPLATE_SOURCE + Locale.getDefault().toString() + "/" + tmplKey + ".vm";
		URL url = this.getClass().getClassLoader().getResource(localeTmpl);
		if (url == null) {
			localeTmpl = TEMPLATE_SOURCE + "default/" + tmplKey + ".vm";
		}

		return localeTmpl;
	}

	private String getSubject(String subject, Object[] params) {
		String key = subject.toLowerCase() + "_subj";
		String message = MessageUtil.getInstance().getMessage(key, "not_found_subj", params);
		if (!message.equals("not_found_subj")) {
			subject = message;
		}

		return getSubjectPrefix() + subject;
	}

	private String getSubjectPrefix() {
		String subjectPrefix = MessageUtil.getInstance().getMessage("email_subject_prefix");
		String deployEnv = ConfigUtil.getInstance().getStrSetting("common", "deploy_env", "");

		subjectPrefix += " " + StringUtils.substring(deployEnv, 0, 10);
		return "[" + subjectPrefix.trim() + "]: ";
	}

	private MimeMessage createMessage(Email mail, Map<String, Object> props)
	throws MessagingException, UnsupportedEncodingException {
		if (props == null) {
			props = Collections.emptyMap();
		}

		MimeMessage mimeMessage;
		Map<String, String> replyToHeaders = (Map<String, String>) props.get("$replyToHeaders");
		if (replyToHeaders != null && !replyToHeaders.isEmpty()) {
			MimeMessage parent = mailSender.createMimeMessage();
			for (Map.Entry<String, String> header : replyToHeaders.entrySet()) {
				parent.setHeader(header.getKey(), header.getValue());
			}

			mimeMessage = (MimeMessage) parent.reply(false, true);
		} else {
			mimeMessage = mailSender.createMimeMessage();
		}

		MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8"); // true = multipart
		message.setSubject(mail.getSubject());

		message.setTo(mail.getToAddress());
		if (mail.getBccAddress() != null) {
			message.setBcc(mail.getBccAddress());
		}

		if (mail.getCcAddress() != null) {
			message.setCc(mail.getCcAddress());
		}

		message.setText(mail.getBody(), true); // true = isHtml
		message.setFrom(getFromEmailId());

		String fromDisplayName = (String) props.getOrDefault("$fromDisplayName", null);
		String fromId = (String) props.getOrDefault("$fromEmailId", null);
		if (StringUtils.isBlank(fromDisplayName)) {
			fromDisplayName = fromId;
		}

		if (StringUtils.isNotBlank(fromId)) {
			message.setFrom(new InternetAddress(fromId, fromDisplayName));
			message.setReplyTo(new InternetAddress(fromId, fromDisplayName));
		} else if (StringUtils.isNotBlank(fromDisplayName)) {
			message.setFrom(new InternetAddress(getFromEmailId(), fromDisplayName));
			message.setReplyTo(new InternetAddress(getFromEmailId(), fromDisplayName));
		}

		if (mail.getAttachments() != null) {
			Map<String, String> names = Collections.emptyMap();
			if (props != null && props.get("$attachments") instanceof Map) {
				names = (Map<String, String>) props.get("$attachments");
			}

			for (File attachment: mail.getAttachments()) {
				String filename = names.getOrDefault(attachment.getName(), attachment.getName());
				message.addAttachment(filename, new FileSystemResource(attachment));
			}
		}

		return mimeMessage;
	}

	private class SendMailTask implements Runnable {
		private MimeMessage mimeMessage;
		
		private String error;

		public SendMailTask(MimeMessage mimeMessage) {
			this.mimeMessage = mimeMessage;
		}
		
		public void run() {
			try {
				String rcpts = toString(mimeMessage.getAllRecipients());
				logger.info("Sending email '" + mimeMessage.getSubject() + "' to " + rcpts);
				mailSender.send(mimeMessage);
				logger.info("Email '" + mimeMessage.getSubject() + "' sent to " + rcpts);
			} catch (Exception e) {
				logger.error("Error sending e-mail ", e);
				this.error = e.getMessage();
			}
		}

		public String getError() {
			return this.error;
		}

		private String toString(Address[] addresses) {
			return Stream.of(addresses).map(addr -> ((InternetAddress) addr).getAddress()).collect(Collectors.joining(", "));
		}
	}

	private String getBaseTmpl() {
		return getTemplate(BASE_TMPL);
	}
	
	private String getFooterTmpl() {
		return getTemplate(FOOTER_TMPL);
	}

	private String[] filterEmailIds(String field, String[] emailIds, boolean ignoreDnd) {
		String[] validEmailIds = filterInvalidEmails(emailIds);
		if (validEmailIds.length == 0) {
			logger.error("Invalid email IDs in " + field + " : " + toString(emailIds));
			return validEmailIds;
		}

		String[] filteredEmailIds = filterEmailIds(validEmailIds, ignoreDnd);
		String ignoredEmailIds = Stream.of(validEmailIds)
			.filter(emailId -> Stream.of(filteredEmailIds).noneMatch(emailId::equals))
			.collect(Collectors.joining(", "));
		if (!ignoredEmailIds.isEmpty()) {
			logger.info("Not sending email to users who are either archived or have enabled DND: " + ignoredEmailIds);
		}

		return filteredEmailIds;
	}

	private String[] filterEmailIds(String[] emailIds, boolean ignoreDnd) {
		Map<String, Pair<Boolean, String>> settings = getEmailIdDnds(emailIds);
		return Arrays.stream(emailIds)
			.filter(
				emailId -> {
					Pair<Boolean, String> status = settings.getOrDefault(emailId, Pair.make(false, "Active"));
					return (ignoreDnd || !Boolean.TRUE.equals(status.first())) && !Status.isClosedOrDisabledStatus(status.second());
				}
			)
			.toArray(String[]::new);
	}

	private String[] filterInvalidEmails(String[] emailIds) {
		if (emailIds == null) {
			return new String[0];
		}

		return Arrays.stream(emailIds).filter(Utility::isValidEmail).toArray(String[]::new);
	}

	@PlusTransactional
	private Map<String, Pair<Boolean, String>> getEmailIdDnds(String[] validEmailIds) {
		return daoFactory.getUserDao().getEmailIdStatuses(Arrays.asList(validEmailIds));
	}

	private String toString(String[] arr) {
		if (arr == null) {
			return StringUtils.EMPTY;
		}

		return StringUtils.join(arr, ",");
	}

	private void initializeMailReceiver() {
		if (mailReceiver != null) {
			mailReceiver = null;
		}

		if (receiverFuture != null) {
			receiverFuture.cancel(false);
			receiverFuture = null;
		}

		if (processors.isEmpty() || StringUtils.isBlank(getImapHost())) {
			logger.info("IMAP service is not configured. Will not poll for inbound emails.");
			return;
		}

		try {
			String url = isSecuredImap() ? "imaps://" : "imap://";
			url += URLEncoder.encode(getAccountId(), "UTF-8") + ":";
			url += URLEncoder.encode(getAccountPassword(), "UTF-8") + "@";
			url += getImapHost() + ":" + getImapPort() + "/" + getFolder().toUpperCase();

			Properties mailProperties = new Properties();
			mailProperties.setProperty("mail.store.protocol", isSecuredImap() ? "imaps" : "imap");

			ImapMailReceiver receiver = new ImapMailReceiver(url);
			receiver.setShouldMarkMessagesAsRead(true);
			receiver.setShouldDeleteMessages(false);
			receiver.setJavaMailProperties(mailProperties);
			receiver.setBeanFactory(OpenSpecimenAppCtxProvider.getAppCtx());
			receiver.afterPropertiesSet();
			mailReceiver = receiver;

			receiverFuture = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
				new ReceiveEmailTask(), getPollInterval(), getPollInterval(), TimeUnit.MINUTES);
		} catch (Exception e) {
			logger.error("Error initialising IMAP receiver", e);
		}
	}

	private class ReceiveEmailTask implements Runnable {
		@Override
		public void run() {
			try {
				if (mailReceiver == null) {
					return;
				}

				Object[] messages = mailReceiver.receive();
				for (Object message : messages) {
					handleMessage((MimeMessage) message);
				}
			} catch (Throwable t) {
				logger.error("Error receiving e-mail messages", t);
			}
		}
	}

	private void handleMessage(MimeMessage message) {
		try {
			Email email = toEmail(message);
			for (EmailProcessor processor : processors) {
				try {
					processor.process(email);
				} catch (Throwable t) {
					logger.error("Error processing the email by: " + processor.getName(), t);
				}
			}
		} catch (Throwable t) {
			logger.error("Error handling the email message", t);
		}
	}

	private Email toEmail(MimeMessage message)
	throws Exception {
		Map<String, String> headers = new HashMap<>();
		Enumeration<Header> headersIter = message.getAllHeaders();
		while (headersIter.hasMoreElements()) {
			Header header = headersIter.nextElement();
			headers.put(header.getName(), header.getValue());
		}

		Email email = new Email();
		email.setHeaders(headers);
		email.setSubject(message.getSubject());

		for (Address from : message.getFrom()) {
			if (from instanceof InternetAddress) {
				email.setFromAddress(((InternetAddress) from).getAddress());
			}
		}

		String text = getText(message).replaceAll("\r\n", "\n");
		List<String> lines = new ArrayList<>();
		for (String line : text.split("\n")) {
			line = line.trim();
			if (line.trim().startsWith(">")) {
				continue;
			}

			if (line.startsWith("On ")) {
				int idx = line.lastIndexOf(" wrote:");
				if (idx > -1) {
					line = line.substring(idx + " wrote:".length());
				}
			}

			lines.add(line);
		}

		email.setBody(String.join("\n", lines));
		return email;
	}

	private String getText(Message message)
	throws Exception {
		if (message.isMimeType("text/plain")) {
			return message.getContent().toString();
		} else if (message.isMimeType("multipart/*")) {
			return getText((MimeMultipart) message.getContent());
		}

		return "";
	}

	private String getText(MimeMultipart mimeMultipart)
	throws Exception {
		int parts = mimeMultipart.getCount();
		String result = "";
		for (int i = 0; i < parts; ++i) {
			BodyPart part = mimeMultipart.getBodyPart(i);
			if (part.isMimeType("text/plain")) {
				result += "\n" + part.getContent().toString();
			} else if (part.getContent() instanceof MimeMultipart) {
				result += "\n" + getText((MimeMultipart) part.getContent());
			}
		}

		return result;
	}

	/**
	 *  Config helper methods
	 */
	private String getAccountId() {
		return cfgSvc.getStrSetting(MODULE, "account_id");
	}
	
	private String getAccountPassword() {
		return cfgSvc.getStrSetting(MODULE, "account_password");
	}

	private String getFromEmailId() {
		return cfgSvc.getStrSetting(MODULE, "from_email_id", getAccountId());
	}
	
	private String getSmtpHost() {
		return cfgSvc.getStrSetting(MODULE, "smtp_server_host");
	}
	
	private Integer getSmtpPort() {
		return cfgSvc.getIntSetting(MODULE, "smtp_server_port", 25);
	}
	
	private String getStartTlsEnabled() {
		return cfgSvc.getStrSetting(MODULE, "starttls_enabled");
	}
	
	private String getSmtpAuthEnabled() {
		return cfgSvc.getStrSetting(MODULE, "smtp_auth_enabled");
	}
	
	private String getAdminEmailId() {
		return cfgSvc.getStrSetting(MODULE, "admin_email_id");
	}	
	
	private String getAppUrl() {
		return cfgSvc.getStrSetting("common", "app_url");
	}

	private boolean isEmailNotifEnabled() {
		return cfgSvc.getBoolSetting("notifications", "all", true);
	}

	private boolean isSecuredImap() {
		return true;
	}

	private String getImapHost() {
		return cfgSvc.getStrSetting(MODULE, "imap_server_host");
	}

	private Integer getImapPort() {
		return cfgSvc.getIntSetting(MODULE, "imap_server_port", 993);
	}

	private String getFolder() {
		return "INBOX";
	}

	private Integer getPollInterval() {
		return cfgSvc.getIntSetting(MODULE, "imap_poll_interval", 5);
	}
}
