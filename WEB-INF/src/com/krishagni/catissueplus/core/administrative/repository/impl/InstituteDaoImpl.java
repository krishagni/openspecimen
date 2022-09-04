package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.events.InstituteDetail;
import com.krishagni.catissueplus.core.administrative.repository.InstituteDao;
import com.krishagni.catissueplus.core.administrative.repository.InstituteListCriteria;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class InstituteDaoImpl extends AbstractDao<Institute> implements InstituteDao {
	
	@Override
	public Class<?> getType() {
		return Institute.class;
	}
	
	@Override
	public List<InstituteDetail> getInstitutes(InstituteListCriteria listCrit) {
		Criteria<Object[]> query = addProjectionFields(getInstituteListQuery(listCrit));

		List<Object[]> rows = query.orderBy(query.asc("institute.name")).list();
		List<InstituteDetail> institutes = new ArrayList<>();
		Map<Long, InstituteDetail> instituteMap = new HashMap<>();
		
		for (Object[] row : rows) {
			InstituteDetail institute = new InstituteDetail();
			institute.setId((Long)row[0]);
			institute.setName((String)row[1]);
			institute.setActivityStatus((String)row[2]);
			institutes.add(institute);
			
			if (listCrit.includeStat()) {
				instituteMap.put(institute.getId(), institute);
			}
		}
		
		if (listCrit.includeStat()) {
			addInstituteStats(instituteMap);
		}
		
		return institutes;
	}

	@Override
	public Long getInstitutesCount(InstituteListCriteria listCrit) {
		return getInstituteListQuery(listCrit).getCount("institute.id");
	}

	@Override
	public List<Institute> getInstituteByNames(List<String> names) {
		return createNamedQuery(GET_INSTITUTES_BY_NAME, Institute.class)
			.setParameterList("names", names)
			.list();
	}
	
	@Override
	public Institute getInstituteByName(String name) {
		List<Institute> result = getInstituteByNames(Collections.singletonList(name));
		return CollectionUtils.isEmpty(result) ? null : result.get(0);
	}

	@Override
	public List<DependentEntityDetail> getDependentEntities(Long instituteId) {
		List<Object[]> rows = createNamedQuery(GET_DEPENDENT_ENTITIES, Object[].class)
			.setParameter("instituteId", instituteId)
			.list();

		List<DependentEntityDetail> dependents = new ArrayList<>();
		for (Object[] row : rows) {
			int count = ((Number) row[1]).intValue();
			if (count > 0) {
				dependents.add(DependentEntityDetail.from((String) row[0], count));
			}
		}

		return dependents;
	}
	
	private Criteria<Object[]> getInstituteListQuery(InstituteListCriteria crit) {
		Criteria<Object[]> query = createCriteria(Institute.class, Object[].class, "institute");
		return addSearchConditions(query, crit);
	}

	private Criteria<Object[]> addSearchConditions(Criteria<Object[]> query, InstituteListCriteria listCrit) {
		if (StringUtils.isNotBlank(listCrit.query())) {
			query.add(query.ilike("institute.name", listCrit.query()));
		}

		applyIdsFilter(query, "institute.id", listCrit.ids());
		return query;
	}
	
	private Criteria<Object[]> addProjectionFields(Criteria<Object[]> query) {
		return query.distinct().select("institute.id", "institute.name", "institute.activityStatus");
	}

	private void addInstituteStats(Map<Long, InstituteDetail> institutesMap) {
		if (institutesMap == null || institutesMap.isEmpty()) {
			return;
		}
		
		List<Object[]> stats = createNamedQuery(GET_INSTITUTE_STATS, Object[].class)
			.setParameterList("instituteIds", institutesMap.keySet())
			.list();
		
		for (Object[] stat : stats) {
			InstituteDetail institute = institutesMap.get((Long)stat[0]);
			institute.setUsersCount(((Long)stat[1]).intValue());
		}
	}
	
	
	private static final String FQN = Institute.class.getName();
	
	private static final String GET_INSTITUTES_BY_NAME = FQN + ".getInstitutesByName";
	
	private static final String GET_INSTITUTE_STATS = FQN + ".getInstituteStats";

	private static final String GET_DEPENDENT_ENTITIES = FQN + ".getDependentEntities";
}
