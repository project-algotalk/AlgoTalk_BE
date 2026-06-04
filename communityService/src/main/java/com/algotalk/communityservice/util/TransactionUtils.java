package com.algotalk.communityservice.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionUtils {

    private TransactionUtils() {
    }

    public static void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new AfterCommitSynchronization(runnable));
            return;
        }

        runnable.run();
    }

    private static class AfterCommitSynchronization implements TransactionSynchronization {

        private final Runnable runnable;

        private AfterCommitSynchronization(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void afterCommit() {
            runnable.run();
        }
    }
}
