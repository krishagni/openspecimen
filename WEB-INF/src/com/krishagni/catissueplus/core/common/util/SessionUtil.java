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
			sessionFactory.getCurrentSession().flush();
		} catch (Exception e) {
			//
			// Oops, we encountered error. This happens when we've received database errors
			// like data truncation error, unique constraint etc ... We can't do much except
			// log and move forward
			//
			logger.info("Error flushing the database session", e);
			throw e;
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
		sessionFactory.getCurrentSession().flush();
	}
}
