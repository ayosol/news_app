package com.example.newsapp.helper;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

public class NotifyJobService extends JobService {

    private static final String TAG = "NotifyJobService";
    private boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob: Job Starter");
        doBackgroundTask(params);

        return false;
    }


    private void doBackgroundTask(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    Log.d(TAG, "run: " + i);
                    if (jobCancelled)
                        return;

                    try {
                        Thread.sleep(10000);
                        Intent serviceIntent = new Intent(NotifyJobService.this, NotificationService.class);
                        startService(serviceIntent);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "Job finished");
                jobFinished(params, true);
            }
        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        jobCancelled = true;
        Log.d(TAG, "Job Cancelled before Completion");
        return true;
    }
}
