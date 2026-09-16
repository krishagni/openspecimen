package com.krishagni.catissueplus.core.common.util;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

public class JsonErrorUtil {
	public static String getMessage(Throwable ex) {
		return getMessage(ex, "The request body is missing, malformed, or contains invalid values.");
	}

	public static String getMessage(Throwable ex, String fallback) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof JsonProcessingException jsonError) {
				String reason = "The request body contains an invalid value.";
				if (jsonError instanceof JsonParseException parseError) {
					reason = "The request body contains malformed JSON.";
					String diagnostic = parseError.getOriginalMessage();
					// Select fixed explanations; never copy parser text, source snippets, or reference chains.
					if (diagnostic != null && diagnostic.contains("was expecting double-quote to start field name")) {
						reason = "Invalid JSON: expected a double-quoted field name.";
					} else if (diagnostic != null && diagnostic.contains("Unexpected end-of-input")) {
						reason = "Invalid JSON: the request body ended before the JSON value was complete.";
					} else if (diagnostic != null && diagnostic.contains("was expecting comma")) {
						reason = "Invalid JSON: expected a comma between values or fields.";
					}
				} else if (jsonError instanceof JsonMappingException mappingError) {
					reason = getMappingMessage(mappingError);
				}

				JsonLocation location = jsonError.getLocation();
				if (location != null && location.getLineNr() > 0 && location.getColumnNr() > 0) {
					reason += " Line " + location.getLineNr() + ", column " + location.getColumnNr() + ".";
				}

				return reason;
			}
		}

		return fallback;
	}

	private static String getMappingMessage(JsonMappingException error) {
		StringBuilder path = new StringBuilder();
		for (JsonMappingException.Reference reference : error.getPath()) {
			// Reference.toString() also contains Java types. Use only JSON field names and indexes.
			if (reference.getFieldName() != null) {
				if (!path.isEmpty()) {
					path.append(".");
				}
				path.append(reference.getFieldName());
			} else if (reference.getIndex() >= 0) {
				path.append("[").append(reference.getIndex()).append("]");
			}
		}

		String subject = path.isEmpty() ? "The JSON value" : "The field \"" + path + "\"";
		if (error instanceof UnrecognizedPropertyException) {
			return subject + " is not recognized. Check the field name.";
		}

		Class<?> type = error instanceof MismatchedInputException mismatch ? mismatch.getTargetType() : null;
		if (type == null) {
			return subject + " contains an invalid value.";
		} else if (type == byte.class || type == Byte.class || type == short.class || type == Short.class ||
			type == int.class || type == Integer.class || type == long.class || type == Long.class || type == BigInteger.class) {
			return subject + " must be an integer within the supported range.";
		} else if (Number.class.isAssignableFrom(type) || type == float.class || type == double.class) {
			return subject + " must be a number.";
		} else if (type == boolean.class || type == Boolean.class) {
			return subject + " must be a boolean (true or false).";
		} else if (type == String.class || type == char.class || type == Character.class) {
			return subject + " must be text.";
		} else if (type.isEnum()) {
			return subject + " must contain a supported value. Check the allowed values for this field.";
		} else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
			return subject + " must be an array.";
		} else if (Map.class.isAssignableFrom(type)) {
			return subject + " must be an object.";
		}

		return subject + " contains an invalid value.";
	}
}
