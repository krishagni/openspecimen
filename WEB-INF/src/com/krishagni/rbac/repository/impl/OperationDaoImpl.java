package com.krishagni.rbac.repository.impl;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.rbac.domain.Operation;
import com.krishagni.rbac.repository.OperationDao;
import com.krishagni.rbac.repository.OperationListCriteria;

public class OperationDaoImpl extends AbstractDao<Operation> implements OperationDao {
	private static final String FQN = Operation.class.getName();
	
	private static final String GET_OPERATION_BY_NAME = FQN + ".getOperationByName";
	
	@Override
	public Operation getOperationByName(String opName) {
		List<Operation> operations = createNamedQuery(GET_OPERATION_BY_NAME, Operation.class)
			.setParameter("name", opName)
			.list();
		return operations.isEmpty() ? null : operations.get(0);
	}

	@Override
	public Operation getOperation(Long id) {
		return getCurrentSession().get(Operation.class, id);
	}

	@Override
	public List<Operation> getOperations(OperationListCriteria listCriteria) {
		Criteria<Operation> query = createCriteria(Operation.class, "op");
		if (StringUtils.isNotBlank(listCriteria.query())) {
			query.add(query.ilike("op.name", listCriteria.query()));
		}
		
		return query.list(listCriteria.startAt(), listCriteria.maxResults());
	}
}
