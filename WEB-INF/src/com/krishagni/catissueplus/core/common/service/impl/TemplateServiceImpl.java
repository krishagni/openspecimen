package com.krishagni.catissueplus.core.common.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.context.MessageSource;

import com.krishagni.catissueplus.core.common.barcodes.BarcodeGenerator;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.service.TemplateService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class TemplateServiceImpl implements TemplateService {
	private VelocityEngine velocityEngine;
	
	private MessageSource messageSource;

	private ConfigurationService cfgSvc;

	private BarcodeGenerator barcodeGenerator;
	
	public VelocityEngine getVelocityEngine() {
		return velocityEngine;
	}

	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}
	
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setBarcodeGenerator(BarcodeGenerator barcodeGenerator) {
		this.barcodeGenerator = barcodeGenerator;
	}

	@Override
	public String render(String templateName, Map<String, Object> props) {
		try {
			TimeZone tz = AuthUtil.getUserTimeZone();
			SimpleDateFormat dateFmt = new SimpleDateFormat(getDateFmt());
			if (tz != null) {
				dateFmt.setTimeZone(tz);
			}

			SimpleDateFormat dateOnlyFmt = new SimpleDateFormat(getDateOnlyFormat());
			if (tz != null) {
				dateOnlyFmt.setTimeZone(tz);
			}

			props.put("locale", Locale.getDefault());
			props.put("messageSource", messageSource);
			props.put("dateFmt", dateFmt);
			props.put("dateOnlyFmt", dateOnlyFmt);
			props.put("Integer", Integer.class);
			props.put("Long", Long.class);
			props.put("String", String.class);
			props.put("Date", Date.class);
			props.put("Instant", Instant.class);
			props.put("Calendar", Calendar.class);
			props.put("barcodeGenerator", barcodeGenerator);

			StringWriter result = new StringWriter();
			VelocityContext context = new VelocityContext(props);
			velocityEngine.mergeTemplate(templateName, "UTF-8", context, result);
			return result.toString();
		} catch (VelocityException ex) {
			throw OpenSpecimenException.serverError(ex);
		}
	}

	@Override
	public void render(String templateName, Map<String, Object> props, File output) {
		try (FileWriter writer = new FileWriter(output)) {
			TimeZone tz = AuthUtil.getUserTimeZone();
			SimpleDateFormat dateFmt = new SimpleDateFormat(getDateFmt());
			if (tz != null) {
				dateFmt.setTimeZone(tz);
			}

			SimpleDateFormat dateOnlyFmt = new SimpleDateFormat(getDateOnlyFormat());
			if (tz != null) {
				dateOnlyFmt.setTimeZone(tz);
			}

			props.put("locale", Locale.getDefault());
			props.put("messageSource", messageSource);
			props.put("dateFmt", dateFmt);
			props.put("dateOnlyFmt", dateOnlyFmt);
			props.put("barcodeGenerator", barcodeGenerator);

			VelocityContext context = new VelocityContext(props);
			velocityEngine.mergeTemplate(templateName, "UTF-8", context, writer);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private String getDateFmt() {
		return cfgSvc.getDateFormat() + " " + cfgSvc.getTimeFormat();
	}

	private String getDateOnlyFormat() {
		return cfgSvc.getDateFormat();
	}
}
