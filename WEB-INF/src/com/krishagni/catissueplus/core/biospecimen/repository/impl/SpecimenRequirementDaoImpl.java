package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenRequirementDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class SpecimenRequirementDaoImpl extends AbstractDao<SpecimenRequirement> implements SpecimenRequirementDao {

	@Override
	public Class<SpecimenRequirement> getType() {
		return SpecimenRequirement.class;
	}
	
	@Override
	public SpecimenRequirement getSpecimenRequirement(Long id) {
		return (SpecimenRequirement) getCurrentSession().get(SpecimenRequirement.class, id);
	}
	
	@Override
	public int getSpecimensCount(Long srId) {
		return ((Number) getCurrentSession().getNamedQuery(GET_SPECIMENS_COUNT)
			.setParameter("srId", srId)
			.uniqueResult()).intValue();
	}
	@Override
	public int getAllSpecimensCount(Long srId) {
		Number count = (Number) getCurrentSession().getNamedQuery(GET_ALL_SPECIMENS_COUNT)
			.setParameter("srId", srId)
			.uniqueResult();
		return count != null ? count.intValue() : 0;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public SpecimenRequirement getByCpEventLabelAndSrCode(String cpShortTitle, String eventLabel, String code) {
		return (SpecimenRequirement) getCurrentSession().getNamedQuery(GET_SR_BY_CP_EVENT_AND_SR_CODE)
			.setParameter("cpShortTitle", cpShortTitle)
			.setParameter("eventLabel", eventLabel)
			.setParameter("code", code)
			.uniqueResult();
	}

	@Override
	public SpecimenRequirement getByCpEventLabelAndSrCode(Long cpId, String eventLabel, String code) {
		return (SpecimenRequirement) getCurrentSession().getNamedQuery(GET_SR_BY_CP_ID_EVENT_N_SR_CODE)
			.setParameter("cpId", cpId)
			.setParameter("eventLabel", eventLabel)
			.setParameter("code", code)
			.uniqueResult();
	}

	private static final String FQN = SpecimenRequirement.class.getName();
	
	private static final String GET_SPECIMENS_COUNT = FQN + ".getCollectedSpecimensCount";

	private static final String GET_ALL_SPECIMENS_COUNT = FQN + ".getAllSpecimensCount";
	
	private static final String GET_SR_BY_CP_EVENT_AND_SR_CODE = FQN + ".getByCpEventLabelAndSrCode";

	private static final String GET_SR_BY_CP_ID_EVENT_N_SR_CODE = FQN + ".getByCpIdEventLabelAndSrCode";
}
