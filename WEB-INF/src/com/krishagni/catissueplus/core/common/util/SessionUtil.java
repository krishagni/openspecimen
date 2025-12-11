package com.krishagni.catissueplus.core.common.util;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class SessionUtil {
	private static final LogUtil logger = LogUtil.getLogger(SessionUtil.class);

	private static SessionUtil instance = null;

	@Autowired
	private SessionFactory sessionFactory;

	public static SessionUtil getInstance() {
		if (instance == null) {
			instance = new SessionUtil();
		}

		return instance;
	}

	public void clearSession() {
		try {
			flush();
		} finally {
			try {
				sessionFactory.getCurrentSession().clear();
			} catch (Exception e) {
				//
				// Something severely wrong...
				//
				logger.error("Error cleaning the database session", e);
			}
		}
	}

	public void flush() {
		try {
			sessionFactory.getCurrentSession().flush();
		} catch (Exception e) {
			//
			// Oops, we encountered error. This happens when we've received database errors
			// like data truncation error, unique constraint etc ... We can't do much except
			// log and move forward
			//
			logger.info("Error flushing the database session", e);
			logger.info("Clearing the cache. This can impact performance a bit. If the cache is not clear subsequent import records will fail.");
			sessionFactory.getCurrentSession().clear();
			throw new RuntimeException(
				"Error flushing the database session. " +
					"This happens when the app has encountered database errors like " +
					"data truncation errors, unique constraint violation errors etc. " +
					"Ensure the data size is within the permissible limits, the auto " +
					"generated labels/names are unique. " +
					"Refer to the first record that generated this error!",
				e
			);
		}
	}
}
