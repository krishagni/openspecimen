package com.krishagni.catissueplus.core.common.util;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;


public class JsonDateDeserializer extends UntypedObjectDeserializer {
	private static final LogUtil logger = LogUtil.getLogger(JsonDateDeserializer.class);

	public JsonDateDeserializer() {
		super((JavaType) null, null);
	}

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt)
	throws IOException {
		Object ret;

		if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
			try {
				if (StringUtils.isBlank(p.getText())) {
					ret = null;
				} else {
					ret = DateUtils.parseDate(p.getText(), "yyyy-MM-dd");
				}
			} catch (Exception e) {
				try {
					ret = Date.from(Instant.parse(p.getText()));
				} catch (Exception e1) {
					try {
						ret = new Date(Long.parseLong(p.getText()));
					} catch (Exception e2) {
						ret = super.deserialize(p, ctxt);
					}
				}
			}
		} else if (p.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
			try {
				ret = new Date(p.getLongValue());
			} catch (Exception e) {
				ret = super.deserialize(p, ctxt);
			}
		} else {
			ret = super.deserialize(p, ctxt);
		}

		logger.info("Input date " + p.getText() + " deserialized to " + ret);
		return ret;
	}
}
