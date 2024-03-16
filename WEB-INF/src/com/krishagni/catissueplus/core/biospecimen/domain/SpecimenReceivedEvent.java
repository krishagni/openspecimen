package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.services.impl.CpWorkflowTxnCache;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class SpecimenReceivedEvent extends SpecimenEvent {
	public static final String FORM_NAME = "SpecimenReceivedEvent";

	public static final String TO_BE_RECEIVED = "To be Received";

	private PermissibleValue quality;

	public SpecimenReceivedEvent(Specimen specimen) {
		super(specimen);
	}
	
	public PermissibleValue getQuality() {
		loadRecordIfNotLoaded();
		return quality;
	}

	public void setQuality(PermissibleValue quality) {
		loadRecordIfNotLoaded();
		this.quality = quality;
	}

	@Override
	public String getFormName() {
		return FORM_NAME;
	}
	
	@Override
	public Map<String, Object> getEventAttrs() {		
		return Collections.singletonMap("quality", quality != null ? quality.getId().toString() : null);
	}

	@Override
	public void setEventAttrs(Map<String, Object> attrValues) {
		String qualityIdStr = (String) attrValues.get("quality");
		if (StringUtils.isNotBlank(qualityIdStr)) {
			if (StringUtils.isNumeric(qualityIdStr)) {
				this.quality = daoFactory.getPermissibleValueDao().getById(Long.parseLong(qualityIdStr));
			} else {
				this.quality = daoFactory.getPermissibleValueDao().getByValue("receive_quality", qualityIdStr);
			}
		}
	}
	
	@Override
	public void update(SpecimenEvent other) {
		update((SpecimenReceivedEvent)other);
	}

	public boolean isReceived() {
		return isReceived(getQuality());
	}

	public static boolean isReceived(PermissibleValue recvQuality) {
		return recvQuality != null && !recvQuality.getValue().equals(TO_BE_RECEIVED);
	}
	
	public static SpecimenReceivedEvent getFor(Specimen specimen) {
		if (specimen.getId() == null) {
			return createFromSr(specimen);
		}

		List<Long> recIds = new SpecimenReceivedEvent(specimen).getRecordIds();		
		if (CollectionUtils.isEmpty(recIds)) {
			return createFromSr(specimen);
		}
		
		SpecimenReceivedEvent event = new SpecimenReceivedEvent(specimen);
		event.setId(recIds.iterator().next());
		return event;		
	}
	
	public static SpecimenReceivedEvent createFromSr(Specimen specimen) {
		SpecimenReceivedEvent event = new SpecimenReceivedEvent(specimen);

		String defRecvQuality = null;
		if (specimen.getCollectionProtocol() != null) {
			defRecvQuality = CpWorkflowTxnCache.getInstance()
				.getValue(specimen.getCpId(), "specimenCollection", "defReceiveQuality");
		}

		if (StringUtils.isBlank(defRecvQuality)) {
			defRecvQuality = Specimen.ACCEPTABLE;
		}

		event.setQuality(event.daoFactory.getPermissibleValueDao().getPv(PvAttributes.RECV_QUALITY, defRecvQuality));
		if (!isReceived(event.getQuality())) {
			return event;
		}

		SpecimenRequirement sr = specimen.getSpecimenRequirement();
		if (sr != null) {
			event.setUser(sr.getReceiver());
		}

		Date receivedTime;
		if (!specimen.getCollectionProtocol().isSpecimenCentric() &&
			specimen.getVisit() != null && specimen.getVisit().getVisitDate() != null) {
			receivedTime = specimen.getVisit().getVisitDate();
		} else {
			receivedTime = Calendar.getInstance().getTime();
		}

		event.setTime(receivedTime);
		if (event.getUser() == null) {
			event.setUser(AuthUtil.getCurrentUser());
		}		
		
		return event;
	}	
	
	private void update(SpecimenReceivedEvent other) {
		super.update(other);
		setQuality(other.getQuality());
		if (isReceived()) {
			if (getUser() == null) {
				setUser(AuthUtil.getCurrentUser());
			}

			if (getTime() == null) {
				setTime(Calendar.getInstance().getTime());
			}
		}
	}
}
