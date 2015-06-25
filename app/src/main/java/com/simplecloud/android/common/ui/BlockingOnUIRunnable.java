package com.simplecloud.android.common.ui;

import android.app.Activity;

public class BlockingOnUIRunnable {

    private Activity activity;
    private BlockingOnUIRunnableListener listener;
    private Runnable mRunnable;


    public BlockingOnUIRunnable(Activity activity, BlockingOnUIRunnableListener listener) {
        this.activity = activity;
        this.listener = listener;

        mRunnable = new Runnable() {

            @Override
            public void run() {
                if (BlockingOnUIRunnable.this.listener != null)
                    BlockingOnUIRunnable.this.listener.onRunOnUIThread();

                synchronized (this) {
                    this.notify();
                }
            }

        };
    }

    /**
     * Start runnable on UI thread and wait until finished
     */
    public void startOnUiAndWait() {
        synchronized (mRunnable) {
            activity.runOnUiThread(mRunnable);

            try {
                mRunnable.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface BlockingOnUIRunnableListener {

        void onRunOnUIThread();
    }
}
