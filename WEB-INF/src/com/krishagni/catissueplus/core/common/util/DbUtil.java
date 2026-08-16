package com.krishagni.catissueplus.core.common.util;

import java.sql.SQLException;
import java.util.Collections;
import java.util.function.Supplier;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;

import edu.common.dynamicextensions.ndao.DbSettingsFactory;
import edu.common.dynamicextensions.ndao.JdbcDaoFactory;

public class DbUtil {
	private static TransactionTemplate newTxnTmpl;

	public static <T> T newTxn(Supplier<T> call) {
		return getNewTxnTmpl().execute(status -> call.get());
	}

	public static boolean doesColumnExist(String tableName, String columnName) {
		try {
			return newTxn(
				() ->
					JdbcDaoFactory.getJdbcDao().getResultSet(
						String.format(TEST_COLUMN_SQL, columnName, tableName),
						Collections.emptyList(),
						rs -> true
					)
			);
		} catch (Throwable e) {
			if (isMissingColumnError(e)) {
				return false;
			}

			throw e;
		}
	}

	private static TransactionTemplate getNewTxnTmpl() {
		if (newTxnTmpl != null) {
			return newTxnTmpl;
		}

		synchronized (DbUtil.class) {
			if (newTxnTmpl == null) {
				PlatformTransactionManager txnMgr = OpenSpecimenAppCtxProvider.getBean("transactionManager");
				newTxnTmpl = new TransactionTemplate(txnMgr);
				newTxnTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			}
		}

		return newTxnTmpl;
	}

	private static boolean isMissingColumnError(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sqlError) {
				return isMissingColumnError(sqlError, DbSettingsFactory.isMySQL());
			}
		}

		return false;
	}

	private static boolean isMissingColumnError(SQLException sqlError, boolean mysql) {
		if (mysql) {
			return sqlError.getErrorCode() == MYSQL_UNKNOWN_COLUMN_ERROR ||
				MYSQL_UNKNOWN_COLUMN_STATE.equals(sqlError.getSQLState());
		} else {
			return sqlError.getErrorCode() == ORACLE_INVALID_IDENTIFIER_ERROR;
		}
	}

	private static final String TEST_COLUMN_SQL = "select %s from %s where 1 = 0";

	private static final int MYSQL_UNKNOWN_COLUMN_ERROR = 1054;

	private static final String MYSQL_UNKNOWN_COLUMN_STATE = "42S22";

	private static final int ORACLE_INVALID_IDENTIFIER_ERROR = 904;
}
