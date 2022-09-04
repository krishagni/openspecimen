package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.DpRequirement;
import com.krishagni.catissueplus.core.administrative.events.DprStat;
import com.krishagni.catissueplus.core.administrative.repository.DpRequirementDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class DpRequirementDaoImpl extends AbstractDao<DpRequirement> implements DpRequirementDao {
	
	@Override
	public Class<DpRequirement> getType() {
		return DpRequirement.class;
	}
	
	@Override
	public Map<Long, DprStat> getDistributionStatByDp(Long dpId) {
		List<Object[]> rows = createNamedQuery(GET_DISTRIBUTION_STAT, Object[].class)
			.setParameter("dpId", dpId)
			.list();
		
		Map<Long, DprStat> result = new HashMap<>();
		for (Object[] row : rows) {
			DprStat stat = new DprStat();
			stat.setDistributedCnt((Long) row[1]);
			stat.setDistributedQty((BigDecimal) row[2]);
			result.put((Long)row[0], stat);
		}

		return result;
	}
	
	private static final String FQN = DpRequirement.class.getName();
	
	private static final String GET_DISTRIBUTION_STAT = FQN + ".getDistributionStat";
	
}
