package com.krishagni.catissueplus.core.common;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.UserDetailsFilter;

public class CustomHttpMessageConverter extends MappingJackson2HttpMessageConverter {

	public CustomHttpMessageConverter() {
		super();

		ObjectMapper mapper = getObjectMapper();
		Jackson2ObjectMapperBuilder.json().filters(
			new SimpleFilterProvider()
				.addFilter("withoutId", SimpleBeanPropertyFilter.serializeAllExcept())
				.addFilter("userDetails", new UserDetailsFilter())
		).configure(mapper);

		//
		// instruct the HTTP response mapper to serialize both user classes
		// through the userDetails filter.
		//
		mapper.addMixIn(UserDetail.class, UserDetailsMixin.class);
		mapper.addMixIn(UserSummary.class, UserDetailsMixin.class);

		SimpleModule module = new SimpleModule();
		module.addDeserializer(String.class, new StdScalarDeserializer<String>(String.class) {
			private static final long serialVersionUID = -4996754298849735407L;

			@Override
			public String deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
				return StringUtils.trim(jp.getValueAsString());
			}
		});
		mapper.registerModule(module);
	}

	@JsonFilter("userDetails")
	private abstract static class UserDetailsMixin {
	}
}
