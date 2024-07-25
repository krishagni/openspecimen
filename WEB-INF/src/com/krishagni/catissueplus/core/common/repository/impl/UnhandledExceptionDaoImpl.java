package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.UnhandledException;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.UnhandledExceptionDao;
import com.krishagni.catissueplus.core.common.repository.UnhandledExceptionListCriteria;

public class UnhandledExceptionDaoImpl extends AbstractDao<UnhandledException> implements UnhandledExceptionDao {
	
	@Override
	public Class<?> getType() {
		return UnhandledException.class;
	}
	
	@Override
	public List<UnhandledException> getUnhandledExceptions(UnhandledExceptionListCriteria listCrit) {
		Criteria<UnhandledException> query = getUnhandledExceptionListQuery(listCrit);
		return query.orderBy(query.desc("e.timestamp"))
			.list(listCrit.startAt(), listCrit.maxResults());
	}

	@Override
	public Long getUnhandledExceptionsCount(UnhandledExceptionListCriteria listCrit) {
		return getUnhandledExceptionListQuery(listCrit).getCount("e.id");
	}

	private Criteria<UnhandledException> getUnhandledExceptionListQuery(UnhandledExceptionListCriteria listCrit) {
		return addSearchCondition(createCriteria(UnhandledException.class, "e"), listCrit);
	}
	
	private Criteria<UnhandledException> addSearchCondition(Criteria<UnhandledException> query, UnhandledExceptionListCriteria listCrit) {
		addInstituteRestriction(query, listCrit);
		return addDateRestriction(query, listCrit);
	}
	
	private Criteria<UnhandledException> addInstituteRestriction(Criteria<UnhandledException> query, UnhandledExceptionListCriteria listCrit) {
		if (listCrit.instituteId() != null) {
			query.join("e.user", "user")
				.join("user.institute", "institute")
				.add(query.eq("institute.id", listCrit.instituteId()));
		}
		
		return query;
	}
	
	private Criteria<UnhandledException> addDateRestriction(Criteria<UnhandledException> query, UnhandledExceptionListCriteria listCrit) {
		if (listCrit.fromDate() != null) {
			query.add(query.ge("e.timestamp", listCrit.fromDate()));
		}
		
		if (listCrit.toDate() != null) {
			query.add(query.le("e.timestamp", listCrit.toDate()));
		}
		
		return query;
	}
}
