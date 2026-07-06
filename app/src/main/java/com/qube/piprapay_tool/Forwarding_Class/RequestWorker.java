package com.qube.piprapay_tool.Forwarding_Class;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.qube.piprapay_tool.Class.AppLogger;
import com.qube.piprapay_tool.Class.HistoryDatabaseHelper;
import com.qube.piprapay_tool.Forwarding_Class.SmsReceiverService;

public class RequestWorker extends Worker {

    public final static String DATA_URL = "URL";
    public final static String DATA_TEXT = "TEXT";
    public final static String DATA_HEADERS = "HEADERS";
    public final static String DATA_IGNORE_SSL = "IGNORE_SSL";
    public final static String DATA_MAX_RETRIES = "MAX_RETRIES";
    public final static String DATA_CHUNKED_MODE = "CHUNKED_MODE";
    public final static String DATA_HISTORY_ID = "HISTORY_ID";

    public RequestWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int maxRetries = getInputData().getInt(DATA_MAX_RETRIES, 10);

        if (getRunAttemptCount() > maxRetries) {
            return Result.failure();
        }

        String url = getInputData().getString(DATA_URL);
        String text = getInputData().getString(DATA_TEXT);
        String headers = getInputData().getString(DATA_HEADERS);
        boolean ignoreSsl = getInputData().getBoolean(DATA_IGNORE_SSL, false);
        boolean useChunkedMode = getInputData().getBoolean(DATA_CHUNKED_MODE, true);

        Context ctx = getApplicationContext();
        long historyId = getInputData().getLong(DATA_HISTORY_ID, -1);
        HistoryDatabaseHelper db = new HistoryDatabaseHelper(ctx);

        AppLogger.log(ctx, "RequestWorker", "Starting HTTP POST to " + url);
        AppLogger.log(ctx, "RequestWorker", "Payload: " + text);

        Request request = new Request(url, text);
        request.setJsonHeaders(headers);
        request.setIgnoreSsl(ignoreSsl);
        request.setUseChunkedMode(useChunkedMode);

        String result = request.execute();

        if (result.equals(Request.RESULT_RETRY)) {
            AppLogger.log(ctx, "RequestWorker", "Result: RETRY (attempt " + getRunAttemptCount() + ")");
            if (historyId != -1) db.updateStatus(historyId, HistoryDatabaseHelper.STATUS_PENDING, "Retry attempt " + getRunAttemptCount());
            return Result.retry();
        }

        if (result.equals(Request.RESULT_ERROR)) {
            AppLogger.log(ctx, "RequestWorker", "Result: ERROR (attempt " + getRunAttemptCount() + ")");
            if (historyId != -1) db.updateStatus(historyId, HistoryDatabaseHelper.STATUS_FAILED, "Failed after " + getRunAttemptCount() + " attempts");
            SmsReceiverService.updateStatus(ctx, "Forwarding Failed", "Error sending SMS to server.");
            return Result.failure();
        }

        AppLogger.log(ctx, "RequestWorker", "Result: SUCCESS. HTTP Response: " + result);
        if (historyId != -1) db.updateStatus(historyId, HistoryDatabaseHelper.STATUS_SUCCESS, null);
        SmsReceiverService.updateStatus(ctx, "Forwarded Successfully", "SMS data sent to server.");
        return Result.success();
    }
}
