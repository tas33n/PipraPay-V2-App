package com.qube.piprapay_tool.Class;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sms_history.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_HISTORY = "history";
    public static final String COL_ID = "id";
    public static final String COL_SENDER = "sender";
    public static final String COL_MESSAGE = "message";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_STATUS = "status";
    public static final String COL_ERROR = "error_reason";
    public static final String COL_URL = "url";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_IGNORED = "IGNORED";

    public HistoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_HISTORY + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_SENDER + " TEXT, "
                + COL_MESSAGE + " TEXT, "
                + COL_TIMESTAMP + " INTEGER, "
                + COL_STATUS + " TEXT, "
                + COL_ERROR + " TEXT, "
                + COL_URL + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public long insertHistory(String sender, String message, long timestamp, String status, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SENDER, sender);
        values.put(COL_MESSAGE, message);
        values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_STATUS, status);
        values.put(COL_URL, url);
        long id = db.insert(TABLE_HISTORY, null, values);
        db.close();
        return id;
    }

    public void updateStatus(long id, String status, String errorReason) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        if (errorReason != null) {
            values.put(COL_ERROR, errorReason);
        }
        db.update(TABLE_HISTORY, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteHistory(List<Long> ids) {
        SQLiteDatabase db = this.getWritableDatabase();
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            args.append("?");
            if (i < ids.size() - 1) args.append(",");
        }
        String[] strIds = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            strIds[i] = String.valueOf(ids.get(i));
        }
        db.delete(TABLE_HISTORY, COL_ID + " IN (" + args.toString() + ")", strIds);
        db.close();
    }

    public void clearAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }

    public Cursor getAllHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COL_TIMESTAMP + " DESC", null);
    }
}
