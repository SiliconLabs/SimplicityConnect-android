package com.siliconlabs.bledemo.Browser.Services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.annotation.Nullable;

import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.Browser.Model.Logs.Log;
import com.siliconlabs.bledemo.Browser.Fragments.LoggerFragment;
import com.siliconlabs.bledemo.Utils.Constants;

import java.util.ConcurrentModificationException;

public class ShareLogServices extends Service {

    private boolean isFiltering = false;
    private String filteringPhrase = "";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String logsText = parseLogsToText();
            if (logsText == null) {
                stopSelf(msg.arg1);
                return;
            }
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, logsText);
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, getResources().getString(R.string.app_name_EFR_Connect) + " LOGS");
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(shareIntent);

            stopSelf(msg.arg1);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, getResources().getString(R.string.Preparing_logs_please_wait), Toast.LENGTH_SHORT).show();

        if (intent.getExtras() != null) {
            isFiltering = intent.getExtras().getBoolean(LoggerFragment.IS_FILTERING_EXTRA);
            if (isFiltering) {
                filteringPhrase = intent.getExtras().getString(LoggerFragment.FILTERING_PHRASE_EXTRA);
                if (filteringPhrase != null) filteringPhrase = filteringPhrase.toLowerCase();
            } else {
                filteringPhrase = "";
            }
        }

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    private String parseLogsToText() {
        StringBuilder sb = new StringBuilder();

        if (isFiltering) {
            try {
                for (Log log : Constants.LOGS) {
                    if (log.getLogTime().toLowerCase().contains(filteringPhrase) || log.getLogInfo().toLowerCase().contains(filteringPhrase)) {
                        sb.append(log.getLogTime());
                        sb.append(log.getLogInfo());
                        sb.append("\n");
                    }
                }
                return sb.toString();
            } catch (ConcurrentModificationException ex) {
                Toast.makeText(this, getResources().getString(R.string.Logs_cannot_be_generated), Toast.LENGTH_SHORT).show();
                return null;
            }
        } else {
            try {
                for (Log log : Constants.LOGS) {
                    sb.append(log.getLogTime());
                    sb.append(log.getLogInfo());
                    sb.append("\n");
                }
                return sb.toString();
            } catch (ConcurrentModificationException ex) {
                Toast.makeText(this, getResources().getString(R.string.Logs_cannot_be_generated), Toast.LENGTH_SHORT).show();
                return null;
            }
        }
    }

}