package com.krishagni.catissueplus.core.administrative.domain;

import com.krishagni.catissueplus.core.common.events.EventCode;
import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class SpecimenRequestEvent extends OpenSpecimenEvent<SpecimenRequest> {
	public enum Type implements EventCode {
		CLOSED;

		@Override
		public String code() {
			return "SPECIMEN_REQUEST_" + name();
		}
	}

	public SpecimenRequestEvent(SpecimenRequestEvent.Type type, SpecimenRequest request) {
		super(type, request);
	}

	public static SpecimenRequestEvent closed(SpecimenRequest request) {
		return new SpecimenRequestEvent(Type.CLOSED, request);
	}
}
