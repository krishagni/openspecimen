package com.krishagni.catissueplus.core.common;

import org.hibernate.Transaction;

public interface TransactionEventListener {
    void onFinishTransaction();
}
