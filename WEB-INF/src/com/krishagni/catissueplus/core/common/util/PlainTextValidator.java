package com.krishagni.catissueplus.core.common.util;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class PlainTextValidator {
	//
	// Reject tag/declaration starts, including unfinished markup, but allow literal angle brackets.
	// This is input validation; HTML output must still be escaped by the renderer.
	//
	private static final Pattern HTML_MARKUP = Pattern.compile("<(?:/?[a-zA-Z]|[!?])");

	/**
	 * Validates the direct string properties of a request bean without changing their values.
	 * Nested objects and collections must be validated separately by the caller.
	 * The named multiline properties may contain carriage returns, newlines and tabs.
	 */
	public static void validate(Object detail, String... multilineProperties) {
		OpenSpecimenException errors = new OpenSpecimenException(ErrorType.USER_ERROR);
		Set<String> multiline = new HashSet<>(Arrays.asList(multilineProperties));

		// Check every string property, including fields supplied directly through the API.
		BeanWrapper bean = new BeanWrapperImpl(detail);
		for (PropertyDescriptor property : bean.getPropertyDescriptors()) {
			if (property.getPropertyType() != String.class || property.getReadMethod() == null) {
				continue;
			}

			String value = (String) bean.getPropertyValue(property.getName());
			if (value != null && !isPlainText(value, multiline.contains(property.getName()))) {
				errors.addError(CommonErrorCode.PLAIN_TEXT_REQUIRED, property.getName());
			}
		}

		errors.checkAndThrow();
	}

	private static boolean isPlainText(String value, boolean multiline) {
		IntPredicate isRejectedChar = ch -> {
			boolean isControl = Character.isISOControl(ch);
			boolean isAllowedWhitespace = multiline && (ch == '\n' || ch == '\r' || ch == '\t');
			return isControl && !isAllowedWhitespace;
		};

		return !HTML_MARKUP.matcher(value).find() && value.codePoints().noneMatch(isRejectedChar);
	}
}
