package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.repository.LabelPrintJobDao;
import com.krishagni.catissueplus.core.biospecimen.repository.LabelPrintJobItemListCriteria;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.events.LabelPrintStat;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class LabelPrintJobDaoImpl extends AbstractDao<LabelPrintJob> implements LabelPrintJobDao {

	@Override
	public Class<LabelPrintJob> getType() {
		return LabelPrintJob.class;
	}

	@Override
	public List<LabelPrintStat> getPrintStats(String type, Date start, Date end) {
		if (!type.equals("specimen")) {
			throw new IllegalArgumentException("Not supported for type = " + type);
		}

		List<Object[]> rows = createNamedQuery(GET_SPMN_PRINT_STATS, Object[].class)
			.setParameter("startDate", start)
			.setParameter("endDate", end)
			.list();

		List<LabelPrintStat> stats = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			LabelPrintStat stat = new LabelPrintStat();
			stat.setUserFirstName((String) row[++idx]);
			stat.setUserLastName((String)row[++idx]);
			stat.setUserEmailAddress((String)row[++idx]);
			stat.setProtocol((String) row[++idx]);
			stat.setSite((String) row[++idx]);
			stat.setCount((Integer) row[++idx]);
			stats.add(stat);
		}

		return stats;
	}

	@Override
	public List<LabelPrintJobItem> getPrintJobItems(Long jobId, int startAt, int maxItems) {
		Criteria<LabelPrintJobItem> query = createCriteria(LabelPrintJobItem.class, "item");
		return query.join("item.job", "job")
			.add(query.eq("job.id", jobId))
			.orderBy(query.asc("item.id"))
			.list(startAt, maxItems);
	}

	@Override
	public List<LabelPrintJobItem> getPrintJobItems(LabelPrintJobItemListCriteria criteria) {
		Criteria<LabelPrintJobItem> query = createCriteria(LabelPrintJobItem.class, "item");
		query.join("item.job", "job");

		if (criteria.lastId() != null) {
			query.add(query.gt("item.id", criteria.lastId()));
		}

		if (StringUtils.isNotBlank(criteria.printerName())) {
			query.add(query.eq("item.printerName", criteria.printerName()));
		}

		if (Boolean.TRUE.equals(criteria.hasContent())) {
			query.add(query.isNotNull("item.content"));
		}

		return query.list(criteria.startAt(), criteria.maxResults());
	}

	private static final String FQN = LabelPrintJob.class.getName();

	private static final String GET_SPMN_PRINT_STATS = FQN + ".getSpecimenPrintStats";
}
