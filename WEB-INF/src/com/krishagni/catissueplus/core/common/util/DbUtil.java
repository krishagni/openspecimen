package com.krishagni.catissueplus.core.common.util;

import java.util.function.Supplier;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;

public class DbUtil {
	private static TransactionTemplate newTxnTmpl;

	public static <T> T newTxn(Supplier<T> call) {
		return getNewTxnTmpl().execute(status -> call.get());
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
}
