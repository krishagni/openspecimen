package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenKit;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenKitDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenKitListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class SpecimenKitDaoImpl extends AbstractDao<SpecimenKit> implements SpecimenKitDao {

	@Override
	public Class<SpecimenKit> getType() {
		return SpecimenKit.class;
	}

	@Override
	public List<SpecimenKitSummary> getSpecimenKits(SpecimenKitListCriteria listCrit) {
		Criteria<SpecimenKit> query = createCriteria(SpecimenKit.class, "k");
		query.addOrder(query.desc("k.id"));
		addCpRestriction(query, listCrit);
		List<SpecimenKitSummary> kits =  getSpecimenKitSummary(query.list(listCrit.startAt(), listCrit.maxResults()));
		if (kits.isEmpty() || !listCrit.includeStat()) {
			return kits;
		}

		Map<Long, SpecimenKitSummary> kitsMap = kits.stream().collect(Collectors.toMap(SpecimenKitSummary::getId, kit -> kit));
		List<Object[]> rows = createNamedQuery(GET_PARTICIPANT_N_SPECIMEN_CNT, Object[].class)
			.setParameterList("kitIds", kitsMap.keySet())
			.list();

		for (Object[] row :rows) {
			Long kitId = (Long)row[0];
			SpecimenKitSummary kit = kitsMap.get(kitId);
			kit.setParticipantCount((Long) row[1]);
			kit.setSpecimenCount((Long) row[2]);
		}

		return kits;
	}

	@Override
	public Map<String, Object> getCpIds(String key, Object value) {
		Criteria<Object[]> query = createCriteria(SpecimenKit.class, Object[].class, "k");
		List<Object[]> rows = query.join("k.collectionProtocol", "cp")
			.select("cp.id", "k.id")
			.add(query.eq(key, value))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return Collections.emptyMap();
		}

		Map<String, Object> result = new HashMap<>();
		Object[] row = rows.iterator().next();
		result.put("cpId", row[0]);
		result.put("kitId", row[1]);
		return result;
	}

	private void addCpRestriction(Criteria<SpecimenKit> query, SpecimenKitListCriteria listCrit) {
		if (listCrit.cpId() == null && StringUtils.isBlank(listCrit.cpShortTitle()) && StringUtils.isBlank(listCrit.cpTitle())) {
			return;
		}

		query.join("k.collectionProtocol", "cp");
		if (listCrit.cpId() != null) {
			query.add(query.eq("cp.id", listCrit.cpId()));
		} else if (StringUtils.isNotBlank(listCrit.cpShortTitle())) {
			query.add(query.eq("cp.shortTitle", listCrit.cpShortTitle()));
		} else if (StringUtils.isNotBlank(listCrit.cpTitle())) {
			query.add(query.eq("cp.title", listCrit.cpTitle()));
		}
	}

	private List<SpecimenKitSummary> getSpecimenKitSummary(List<SpecimenKit> kits) {
		return kits.stream().map(SpecimenKitSummary::from).collect(Collectors.toList());
	}

	private static final String FQN = SpecimenKit.class.getName();

	private static final String GET_PARTICIPANT_N_SPECIMEN_CNT = FQN + ".getParticipantAndSpecimenCount";
}
