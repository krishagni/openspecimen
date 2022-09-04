package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.repository.LabelPrintJobDao;
import com.krishagni.catissueplus.core.common.domain.LabelPrintFileItem;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
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
	public void savePrintFileItem(LabelPrintFileItem item) {
		getCurrentSession().saveOrUpdate(item);
	}

	@Override
	public List<LabelPrintFileItem> getPrintFileItems(Long jobId, int startAt, int maxItems) {
		Criteria<LabelPrintFileItem> query = createCriteria(LabelPrintFileItem.class, "item");
		return query.join("item.job", "job")
			.add(query.eq("job.id", jobId))
			.orderBy(query.asc("item.id"))
			.list(startAt, maxItems);
	}

	@Override
	public void deletePrintFileItems(Long jobId) {
		createNamedQuery(DEL_PRINT_FILE_ITEMS)
			.setParameter("jobId", jobId)
			.executeUpdate();
	}

	@Override
	public void deletePrintFileItemsOlderThan(Date time) {
		createNamedQuery(DEL_OLD_PRINT_FILE_ITEMS)
			.setParameter("olderThan", time)
			.executeUpdate();
	}

	private static final String FQN = LabelPrintJob.class.getName();

	private static final String GET_SPMN_PRINT_STATS = FQN + ".getSpecimenPrintStats";

	private static final String PRINT_FILE_ITEMS_FQN = LabelPrintFileItem.class.getName();

	private static final String DEL_PRINT_FILE_ITEMS = PRINT_FILE_ITEMS_FQN + ".deleteItems";

	private static final String DEL_OLD_PRINT_FILE_ITEMS = PRINT_FILE_ITEMS_FQN + ".deleteItemsOlderThan";
}
