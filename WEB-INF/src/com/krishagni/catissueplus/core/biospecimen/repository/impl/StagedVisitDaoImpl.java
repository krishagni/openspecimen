package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedVisit;
import com.krishagni.catissueplus.core.biospecimen.repository.StagedVisitDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class StagedVisitDaoImpl extends AbstractDao<StagedVisit> implements StagedVisitDao {
	@Override
	public Class<StagedVisit> getType() {
		return StagedVisit.class;
	}

	@Override
	public StagedVisit getByName(String name) {
		return createNamedQuery(GET_BY_NAME, StagedVisit.class)
			.setParameter("name", name.toLowerCase())
			.uniqueResult();
	}

	@Override
	public StagedVisit getBySprNo(String sprNo) {
		return createNamedQuery(GET_BY_SPR_NO, StagedVisit.class)
			.setParameter("sprNo", sprNo.toLowerCase())
			.uniqueResult();
	}

	@Override
	public List<StagedVisit> getByEmpiOrMrn(String empiOrMrn) {
		return createNamedQuery(GET_BY_EMPI_OR_MRN, StagedVisit.class)
			.setParameter("empiOrMrn", empiOrMrn.toLowerCase())
			.list();
	}

	@Override
	public int deleteOldVisits(int olderThanDays) {
		Date olderThanDt = Date.from(Instant.now().minus(Duration.ofDays(olderThanDays)));
		deleteOldVisitRecords(DEL_OLD_VISIT_DIAG, olderThanDt);
		return deleteOldVisitRecords(DEL_OLD_VISITS, olderThanDt);
	}

	private int deleteOldVisitRecords(String query, Date olderThanDt) {
		return createNamedQuery(query, Integer.class)
			.setParameter("olderThanDt", olderThanDt)
			.executeUpdate();
	}

	private static final String FQN = StagedVisit.class.getName();

	private static final String GET_BY_NAME = FQN + ".getByName";

	private static final String GET_BY_SPR_NO = FQN + ".getBySprNo";

	private static final String GET_BY_EMPI_OR_MRN = FQN + ".getByEmpiOrMrn";

	private static final String DEL_OLD_VISITS = FQN + ".deleteOldVisits";

	private static final String DEL_OLD_VISIT_DIAG = FQN + ".deleteOldVisitDiagnoses";
}
