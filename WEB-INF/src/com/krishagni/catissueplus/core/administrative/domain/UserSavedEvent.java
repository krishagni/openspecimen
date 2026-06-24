package com.krishagni.catissueplus.core.administrative.domain;

import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class UserSavedEvent extends OpenSpecimenEvent<User> {
	public enum Operation {
		CREATE,

		UPDATE,

		STATUS_UPDATE,

		DELETE
	}

	private final Operation operation;

	private Map<String, Object> props = new HashMap<>();

	public UserSavedEvent(User user, Operation operation) {
		super(null, user);
		this.operation = operation;
	}

	public Operation getOperation() {
		return operation;
	}

	public UserSavedEvent prop(String name, Object value) {
		props.put(name, value);
		return this;
	}

	public <T> T prop(String name) {
		return (T) props.get(name);
	}
}
