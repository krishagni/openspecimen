package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.PlusTransactional;

@Configurable
public class RefreshCprSpecimenStats implements ScheduledTask {

	private static final int DEFAULT_CHUNK_SIZE = 10000;

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun)
	throws Exception {
		int chunkSize = getChunkSize(jobRun);
		long lastId = 0L;
		long prevLastId = -1L;
		while (prevLastId != lastId) {
			prevLastId = lastId;
			lastId = invokeRefreshProc(prevLastId, chunkSize);
		}
	}

	@PlusTransactional
	private long invokeRefreshProc(long lastId, int chunkSize) {
		AtomicLong updatedLastId = new AtomicLong(lastId);
		sessionFactory.getCurrentSession().doWork(
			connection -> {
				try (CallableStatement stmt = connection.prepareCall("{call refresh_cpr_spmn_stats(?, ?, ?)}")) {
					stmt.setLong(1, lastId);
					stmt.setInt(2, chunkSize);
					stmt.registerOutParameter(3, Types.BIGINT);
					stmt.execute();

					long value = stmt.getLong(3);
					updatedLastId.set(stmt.wasNull() ? lastId : value);
				}
			}
		);

		return updatedLastId.get();
	}

	private int getChunkSize(ScheduledJobRun jobRun) {
		String args = jobRun.getScheduledJob().getFixedArgs();
		if (StringUtils.isBlank(args)) {
			return DEFAULT_CHUNK_SIZE;
		}

		try {
			int value = Integer.parseInt(args.trim());
			return value > 0 ? value : DEFAULT_CHUNK_SIZE;
		} catch (Exception e) {
			return DEFAULT_CHUNK_SIZE;
		}
	}
}
