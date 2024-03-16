package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.services.impl.CpWorkflowTxnCache;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

@Configurable
public class SpecimenCollectionEvent extends SpecimenEvent {
	private PermissibleValue procedure;
	
	private PermissibleValue container;

	public SpecimenCollectionEvent(Specimen specimen) {
		super(specimen);
	}
	
	public PermissibleValue getProcedure() {
		loadRecordIfNotLoaded();
		return procedure;
	}

	public void setProcedure(PermissibleValue procedure) {
		loadRecordIfNotLoaded();
		this.procedure = procedure;
	}

	public PermissibleValue getContainer() {
		loadRecordIfNotLoaded();
		return container;
	}

	public void setContainer(PermissibleValue container) {
		loadRecordIfNotLoaded();
		this.container = container;
	}

	@Override
	public String getFormName() {
		return "SpecimenCollectionEvent"; 
	}
	
	@Override
	public Map<String, Object> getEventAttrs() {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("procedure", procedure != null ? procedure.getId().toString() : null);
		attrs.put("container", container != null ? container.getId().toString() : null);
		return attrs;
	}
	
	@Override
	public void setEventAttrs(Map<String, Object> attrValues) {
		//
		// TODO: load both values in a single SQL
		//
		String procIdStr = (String)attrValues.get("procedure");
		if (StringUtils.isNotBlank(procIdStr)) {
			if (StringUtils.isNumeric(procIdStr)) {
				this.procedure = daoFactory.getPermissibleValueDao().getById(Long.parseLong(procIdStr));
			} else {
				this.procedure = daoFactory.getPermissibleValueDao().getByValue("collection_procedure", procIdStr);
			}
		}

		String contIdStr = (String)attrValues.get("container");
		if (StringUtils.isNotBlank(contIdStr)) {
			if (StringUtils.isNumeric(contIdStr)) {
				this.container = daoFactory.getPermissibleValueDao().getById(Long.parseLong(contIdStr));
			} else {
				this.container = daoFactory.getPermissibleValueDao().getByValue("collection_container", contIdStr);
			}
		}
	}
	
	@Override
	public void update(SpecimenEvent other) {
		update((SpecimenCollectionEvent)other);
	}
	
	public static SpecimenCollectionEvent getFor(Specimen specimen) {
		if (specimen.getId() == null) {
			return createFromSr(specimen);
		}
		
		List<Long> recIds = new SpecimenCollectionEvent(specimen).getRecordIds();		
		if (CollectionUtils.isEmpty(recIds)) {
			return createFromSr(specimen);
		}
		
		SpecimenCollectionEvent event = new SpecimenCollectionEvent(specimen);
		event.setId(recIds.iterator().next());
		return event;		
	}
	
	public static SpecimenCollectionEvent createFromSr(Specimen specimen) {
		SpecimenCollectionEvent event = new SpecimenCollectionEvent(specimen);
		SpecimenRequirement sr = specimen.getSpecimenRequirement();
		if (sr != null) {
			event.setContainer(sr.getCollectionContainer());
			event.setProcedure(sr.getCollectionProcedure());
			event.setUser(sr.getCollector());
		} else {
			// TODO: load both values in a single SQL
			event.setContainer(event.daoFactory.getPermissibleValueDao().getPv(PvAttributes.CONTAINER, Specimen.NOT_SPECIFIED));
			event.setProcedure(event.daoFactory.getPermissibleValueDao().getPv(PvAttributes.COLL_PROC, Specimen.NOT_SPECIFIED));
		}

		String defCollectionDate = null;
		if (specimen.getCollectionProtocol() != null) {
			defCollectionDate = CpWorkflowTxnCache.getInstance()
				.getValue(specimen.getCpId(), "specimenCollection", "defCollectionDate");
		}

		Date collectionTime = null;
		if (StringUtils.equals(defCollectionDate, "current_date")) {
			collectionTime = Calendar.getInstance().getTime();
		} else if (!specimen.getCollectionProtocol().isSpecimenCentric() && specimen.getVisit() != null) {
			collectionTime = specimen.getVisit().getVisitDate();
		}

		if (collectionTime == null) {
			collectionTime = Calendar.getInstance().getTime();
		}

		event.setTime(collectionTime);
		if (event.getUser() == null) {
			event.setUser(AuthUtil.getCurrentUser());
		}		
		
		return event;
	}
	
	private void update(SpecimenCollectionEvent other) {
		super.update(other);
		setContainer(other.getContainer());
		setProcedure(other.getProcedure());
	}
}
