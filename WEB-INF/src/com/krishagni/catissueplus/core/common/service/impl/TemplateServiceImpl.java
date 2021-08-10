package com.krishagni.catissueplus.core.common.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.context.MessageSource;
import org.springframework.ui.velocity.VelocityEngineUtils;

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
			props.put("barcodeGenerator", barcodeGenerator);
			return VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, templateName, props);
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
			VelocityEngineUtils.mergeTemplate(velocityEngine, templateName, props, writer);
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
