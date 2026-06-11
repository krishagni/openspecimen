package com.krishagni.catissueplus.core.common.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
			props.put("datePickerFmt", new DatePickerFormatter(dateFmt, dateOnlyFmt));
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
			props.put("datePickerFmt", new DatePickerFormatter(dateFmt, dateOnlyFmt));
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

	public static class DatePickerFormatter {
		private static final DateTimeFormatter ISO_TZ_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

		private static final DateTimeFormatter ISO_TZ_FMT_NO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

		private final SimpleDateFormat dateFmt;

		private final SimpleDateFormat dateOnlyFmt;

		public DatePickerFormatter(SimpleDateFormat dateFmt, SimpleDateFormat dateOnlyFmt) {
			this.dateFmt = dateFmt;
			this.dateOnlyFmt = dateOnlyFmt;
		}

		public String formatDate(Object input) {
			Date date = objToDate(input);
			return date != null ? dateOnlyFmt.format(date) : input.toString();
		}

		public String formatDateTime(Object input) {
			Date date = objToDate(input);
			return date != null ? dateFmt.format(date) : input.toString();
		}

		private Date objToDate(Object input) {
			if (input instanceof Date) {
				return (Date)input;
			} else if (input instanceof Number) {
				return new Date(((Number)input).longValue());
			} else if (input instanceof String) {
				return strToDate((String)input);
			}

			return null;
		}

		private Date strToDate(String input) {
			String trimmedInput = input.trim();
			if (trimmedInput.isEmpty()) {
				return null;
			} else if (trimmedInput.matches("\\d+")) {
				try {
					return new Date(Long.parseLong(trimmedInput));
				} catch (NumberFormatException nfe) {
					return null;
				}
			}

			ZoneId zoneId = dateFmt.getTimeZone().toZoneId();
			try {
				// parse strings of the form: yyyy-MM-ddTHH:mm:ss.SSZ (100th of second)
				return Date.from(Instant.parse(trimmedInput));
			} catch (Exception e) {
				// Try other date/time representations below.
			}

			try {
				// parse strings of the form: yyyy-MM-ddTHH:mm:ssXX (offset format)
				return Date.from(OffsetDateTime.parse(trimmedInput).toInstant());
			} catch (Exception e) {
				// Try other date/time representations below.
			}

			try {
				// parse strings of the form: yyyy-MM-ddTHH:mm:ssXX (offset format)
				return Date.from(ZonedDateTime.parse(trimmedInput).toInstant());
			} catch (Exception e) {
				// Try other date/time representations below.
			}

			try {
				return Date.from(OffsetDateTime.parse(trimmedInput, ISO_TZ_FMT).toInstant());
			} catch (Exception e) {
				// Try other date/time representations below.
			}

			try {
				return Date.from(OffsetDateTime.parse(trimmedInput, ISO_TZ_FMT_NO_MILLIS).toInstant());
			} catch (Exception e) {
				// Try other date/time representations below.
			}

			try {
				// yyyy-MM-ddTHH:mm:ss
				return Date.from(LocalDateTime.parse(trimmedInput).atZone(zoneId).toInstant());
			} catch (Exception e) {
				// Try date-only representation below.
			}

			try {
				// yyyy-MM-dd
				return Date.from(LocalDate.parse(trimmedInput).atStartOfDay(zoneId).toInstant());
			} catch (Exception e) {
				return null;
			}
		}
	}
}
