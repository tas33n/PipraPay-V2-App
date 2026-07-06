package com.qube.piprapay_tool.Activity;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qube.piprapay_tool.Class.HistoryDatabaseHelper;
import com.qube.piprapay_tool.R;

import org.apache.commons.text.StringEscapeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryDatabaseHelper dbHelper;
    private HistoryAdapter adapter;
    private ImageView btnBack, btnDelete, btnClearAll;

    private boolean isMultiSelectMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvHistory = findViewById(R.id.rvHistory);
        btnBack = findViewById(R.id.btnBack);
        btnDelete = findViewById(R.id.btnDelete);
        btnClearAll = findViewById(R.id.btnClearAll);

        dbHelper = new HistoryDatabaseHelper(this);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        
        loadHistory();

        btnBack.setOnClickListener(v -> finish());

        btnClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to delete all transaction history?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    dbHelper.clearAllHistory();
                    loadHistory();
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        btnDelete.setOnClickListener(v -> {
            List<Long> selectedIds = adapter.getSelectedIds();
            if (selectedIds.isEmpty()) {
                isMultiSelectMode = false;
                btnDelete.setVisibility(View.GONE);
                adapter.setMultiSelectMode(false);
                return;
            }

            dbHelper.deleteHistory(selectedIds);
            isMultiSelectMode = false;
            btnDelete.setVisibility(View.GONE);
            loadHistory();
            Toast.makeText(this, selectedIds.size() + " items deleted", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadHistory() {
        Cursor cursor = dbHelper.getAllHistory();
        List<HistoryItem> items = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HistoryItem item = new HistoryItem();
                item.id = cursor.getLong(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_ID));
                item.sender = cursor.getString(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_SENDER));
                item.message = cursor.getString(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_MESSAGE));
                item.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_TIMESTAMP));
                item.status = cursor.getString(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_STATUS));
                item.errorReason = cursor.getString(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COL_ERROR));
                items.add(item);
            }
            cursor.close();
        }

        adapter = new HistoryAdapter(items);
        rvHistory.setAdapter(adapter);
    }

    class HistoryItem {
        long id;
        String sender;
        String message;
        long timestamp;
        String status;
        String errorReason;
        boolean isSelected = false;
    }

    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryItem> items;
        private boolean multiSelectMode = false;

        HistoryAdapter(List<HistoryItem> items) {
            this.items = items;
        }

        public void setMultiSelectMode(boolean mode) {
            this.multiSelectMode = mode;
            for (HistoryItem item : items) item.isSelected = false;
            notifyDataSetChanged();
        }

        public List<Long> getSelectedIds() {
            List<Long> ids = new ArrayList<>();
            for (HistoryItem item : items) {
                if (item.isSelected) ids.add(item.id);
            }
            return ids;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = items.get(position);
            
            holder.tvSender.setText(item.sender);
            
            // Unescape message for Bangla
            holder.tvMessage.setText(StringEscapeUtils.unescapeJava(item.message));
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(item.timestamp)));
            
            holder.tvStatus.setText(item.status);
            
            if (item.errorReason != null && !item.errorReason.isEmpty()) {
                holder.tvError.setVisibility(View.VISIBLE);
                holder.tvError.setText(item.errorReason);
            } else {
                holder.tvError.setVisibility(View.GONE);
            }

            int color = Color.parseColor("#FFC107"); // Pending yellow
            if (HistoryDatabaseHelper.STATUS_SUCCESS.equals(item.status)) {
                color = Color.parseColor("#4CAF50"); // Success green
            } else if (HistoryDatabaseHelper.STATUS_FAILED.equals(item.status)) {
                color = Color.parseColor("#D32F2F"); // Failed red
            } else if (HistoryDatabaseHelper.STATUS_IGNORED.equals(item.status)) {
                color = Color.parseColor("#9E9E9E"); // Ignored grey
            }

            holder.statusIndicator.setBackgroundColor(color);
            holder.tvStatus.setTextColor(color);

            if (multiSelectMode) {
                holder.cbSelect.setVisibility(View.VISIBLE);
                holder.cbSelect.setChecked(item.isSelected);
            } else {
                holder.cbSelect.setVisibility(View.GONE);
            }

            holder.itemView.setOnLongClickListener(v -> {
                if (!multiSelectMode) {
                    multiSelectMode = true;
                    isMultiSelectMode = true;
                    item.isSelected = true;
                    btnDelete.setVisibility(View.VISIBLE);
                    notifyDataSetChanged();
                }
                return true;
            });

            holder.itemView.setOnClickListener(v -> {
                if (multiSelectMode) {
                    item.isSelected = !item.isSelected;
                    holder.cbSelect.setChecked(item.isSelected);
                }
            });

            holder.cbSelect.setOnClickListener(v -> {
                item.isSelected = holder.cbSelect.isChecked();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View statusIndicator;
            CheckBox cbSelect;
            TextView tvSender, tvTime, tvMessage, tvStatus, tvError;

            ViewHolder(View itemView) {
                super(itemView);
                statusIndicator = itemView.findViewById(R.id.statusIndicator);
                cbSelect = itemView.findViewById(R.id.cbSelect);
                tvSender = itemView.findViewById(R.id.tvSender);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvError = itemView.findViewById(R.id.tvError);
            }
        }
    }
}
