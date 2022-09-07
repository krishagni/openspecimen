package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.ConsentStatement;
import com.krishagni.catissueplus.core.biospecimen.repository.ConsentStatementDao;
import com.krishagni.catissueplus.core.biospecimen.repository.ConsentStatementListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class ConsentStatementDaoImpl extends AbstractDao<ConsentStatement> implements ConsentStatementDao {
	
	@Override
	public Class<?> getType() {
		return ConsentStatement.class;
	}
	
	@Override
	public List<ConsentStatement> getStatements(ConsentStatementListCriteria listCrit) {
		Criteria<ConsentStatement> query = getStatementListCriteria(listCrit);
		return query.orderBy(query.asc("cs.code")).list(listCrit.startAt(), listCrit.maxResults());
	}
	
	@Override
	public Long getStatementsCount(ConsentStatementListCriteria listCrit) {
		return getStatementListCriteria(listCrit).getCount("cs.id");
	}
	
	@Override
	public ConsentStatement getByCode(String code) {
		return createNamedQuery(GET_BY_CODE, ConsentStatement.class)
			.setParameter("code", code)
			.uniqueResult();
	}

	@Override
	public List<ConsentStatement> getByCodes(Collection<String> codes) {
		Criteria<ConsentStatement> query = createCriteria(ConsentStatement.class, "cs");
		return query.add(query.in("cs.code", codes)).list();
	}

	@Override
	public ConsentStatement getByStatement(String statement) {
		return createNamedQuery(GET_BY_STATEMENT, ConsentStatement.class)
			.setParameter("statement", statement)
			.uniqueResult();
	}

	private Criteria<ConsentStatement> getStatementListCriteria(ConsentStatementListCriteria listCrit) {
		Criteria<ConsentStatement> query = createCriteria(ConsentStatement.class, "cs");

		String searchString = listCrit.query();
		if (StringUtils.isBlank(searchString)) {
			addCodeRestriction(query, listCrit.code());
			addStatementRestriction(query, listCrit.statement());
		} else {
			query.add(
				query.disjunction()
					.add(query.ilike("cs.code",      searchString))
					.add(query.ilike("cs.statement", searchString))
			);
		}

		return query;
	}

	private void addCodeRestriction(Criteria<ConsentStatement> query, String code) {
		if (StringUtils.isNotBlank(code)) {
			query.add(query.ilike("code", code));
		}
	}

	private void addStatementRestriction(Criteria<ConsentStatement> query, String statement) {
		if (StringUtils.isNotBlank(statement)) {
			query.add(query.ilike("statement", statement));
		}
	}

	private static final String FQN = ConsentStatement.class.getName();
	
	private static final String GET_BY_CODE = FQN + ".getByCode";

	private static final String GET_BY_STATEMENT = FQN + ".getByStatement";
}
