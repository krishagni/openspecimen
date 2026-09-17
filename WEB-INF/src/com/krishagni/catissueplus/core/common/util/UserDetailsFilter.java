package com.krishagni.catissueplus.core.common.util;

import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.events.UserSummary;

public class UserDetailsFilter extends SimpleBeanPropertyFilter {

	private static final Set<String> BASIC_FIELDS = Set.of(
		"id", "firstName", "lastName", "emailAddress",
		"type", "loginName", "domain", "domainName",
		"instituteId", "instituteName"
	);

	private static final Object USER_MANAGEMENT_PERMISSION = new Object();

	public static SimpleBeanPropertyFilter basicFieldsOnly() {
		return SimpleBeanPropertyFilter.filterOutAllExcept(BASIC_FIELDS);
	}

	@Override
	public void serializeAsField(Object value, JsonGenerator generator, SerializerProvider provider, PropertyWriter writer)
	throws Exception {
		boolean allowed = BASIC_FIELDS.contains(writer.getName()) ||
			(
				value instanceof UserDetail detail && detail.isSignupResponse() &&
				"activityStatus".equals(writer.getName())
			);

		if (!allowed) {
			if (value instanceof UserDetail detail) {
				allowed = canReadFullUserDetails(detail.getId(), detail.getInstituteId(), detail.getType(), provider);
			} else if (value instanceof UserSummary summary) {
				allowed = canReadFullUserDetails(summary.getId(), summary.getInstituteId(), summary.getType(), provider);
			} else {
				allowed = true;
			}
		}

		if (allowed) {
			writer.serializeAsField(value, generator, provider);
		} else if (!generator.canOmitFields()) {
			writer.serializeAsOmittedField(value, generator, provider);
		}
	}

	private boolean canReadFullUserDetails(Long userId, Long instituteId, String userType, SerializerProvider provider) {
		AccessCtrlMgr access = AccessCtrlMgr.getInstance();
		return access.canReadFullUserDetails(userId, instituteId, userType, () -> {
			// Resolve lazily and cache both grants and denials for this response only.
			Boolean allowed = (Boolean) provider.getAttribute(USER_MANAGEMENT_PERMISSION);
			if (allowed == null) {
				allowed = access.canCreateOrUpdateUsers();
				provider.setAttribute(USER_MANAGEMENT_PERMISSION, allowed);
			}
			return allowed;
		});
	}
}
