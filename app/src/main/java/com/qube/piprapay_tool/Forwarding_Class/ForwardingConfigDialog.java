package com.qube.piprapay_tool.Forwarding_Class;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.qube.piprapay_tool.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ForwardingConfigDialog {

    static final public String BROADCAST_KEY = "TEST_RESULT";

    final private Context context;
    final private LayoutInflater layoutInflater;
    final private ListAdapter listAdapter;
    final Toast[] currentToast = new Toast[1];

    public ForwardingConfigDialog(Context context, LayoutInflater layoutInflater, ListAdapter listAdapter) {
        this.context = context;
        this.layoutInflater = layoutInflater;
        this.listAdapter = listAdapter;

        IntentFilter filter = new IntentFilter(BROADCAST_KEY);
        BroadcastReceiver testResult = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String result = intent.getStringExtra(BROADCAST_KEY);
                Toast.makeText(context.getApplicationContext(), result, Toast.LENGTH_LONG).show();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(testResult, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(testResult, filter);
        }
    }


    public void showNew() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_config_edit_form);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);


        final EditText templateInput = dialog.findViewById(R.id.input_json_template);
        templateInput.setText(ForwardingConfig.getDefaultJsonTemplate());

        final EditText urlInput = dialog.findViewById(R.id.input_url);

        final EditText headersInput = dialog.findViewById(R.id.input_json_headers);
        headersInput.setText(ForwardingConfig.getDefaultJsonHeaders());

        final EditText retriesNumInput = dialog.findViewById(R.id.input_number_retries);
        retriesNumInput.setText(String.valueOf(ForwardingConfig.getDefaultRetriesNumber()));

        final CheckBox chunkedModeCheckbox = dialog.findViewById(R.id.input_chunked_mode);
        chunkedModeCheckbox.setChecked(true);

        prepareSimSelector(context, dialog.findViewById(R.id.dialog_root), 0); // Update as needed

        ImageView txtAdd = dialog.findViewById(R.id.add_button);       // Replace with actual ID
        ImageView txtCancel = dialog.findViewById(R.id.cancel_button); // Replace with actual ID

        txtCancel.setOnClickListener(v -> dialog.dismiss());

        txtAdd.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            ForwardingConfig config = populateConfig(dialog.findViewById(R.id.dialog_root), context, new ForwardingConfig(context));
            if (config == null) {
                return;
            }

            StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, url,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.optString("status");
                            if ("true".equalsIgnoreCase(status)) {
                                config.save();
                                listAdapter.addItem(config);
                                String message = jsonResponse.optString("message");
                                show_custom_tost(message);
                            }else{
                                String message = jsonResponse.optString("message");
                                show_custom_tost(message);
                            }
                            dialog.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            show_custom_tost("❗ Unable to connect");
                            dialog.dismiss();
                        }
                    },
                    error -> {
                        show_custom_tost("❗Unable to connect");
                        dialog.dismiss();
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("d_model", Build.MODEL);
                    params.put("d_brand", Build.BRAND);
                    params.put("d_version", Build.VERSION.RELEASE);
                    params.put("d_api_level", Build.VERSION.SDK);
                    params.put("connection_status", "Connected");
                    return params;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(stringRequest);
        });



        dialog.show();
    }


    public void showEdit(ForwardingConfig config) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_config_edit_form);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final EditText phoneInput = dialog.findViewById(R.id.input_phone);
        phoneInput.setText(config.getSender());

        final EditText urlInput = dialog.findViewById(R.id.input_url);
        urlInput.setText(config.getUrl());

        final EditText templateInput = dialog.findViewById(R.id.input_json_template);
        templateInput.setText(config.getTemplate());

        final EditText headersInput = dialog.findViewById(R.id.input_json_headers);
        headersInput.setText(config.getHeaders());

        final EditText retriesNumInput = dialog.findViewById(R.id.input_number_retries);
        retriesNumInput.setText(String.valueOf(config.getRetriesNumber()));

        final CheckBox ignoreSslCheckbox = dialog.findViewById(R.id.input_ignore_ssl);
        ignoreSslCheckbox.setChecked(config.getIgnoreSsl());

        final CheckBox chunkedModeCheckbox = dialog.findViewById(R.id.input_chunked_mode);
        chunkedModeCheckbox.setChecked(config.getChunkedMode());

        prepareSimSelector(context, dialog.findViewById(R.id.dialog_root), config.getSimSlot());

        ImageView txtAdd = dialog.findViewById(R.id.add_button);       // Replace with your actual save button
        ImageView txtCancel = dialog.findViewById(R.id.cancel_button); // Replace with your actual cancel button

        txtCancel.setOnClickListener(v -> dialog.dismiss());

        txtAdd.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            ForwardingConfig updatedConfig = populateConfig(dialog.findViewById(R.id.dialog_root), context, config);
            if (updatedConfig == null) {
                return;
            }

            StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, url,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.optString("status");
                            if ("true".equalsIgnoreCase(status)) {
                                updatedConfig.save();
                                listAdapter.notifyDataSetChanged(); // Refresh list
                                String message = jsonResponse.optString("message");
                                show_custom_tost(message);
                            }else{
                                String message = jsonResponse.optString("message");
                                show_custom_tost(message);
                            }
                            dialog.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            show_custom_tost("❗Unable to connect");
                            dialog.dismiss();
                        }
                    },
                    error -> {
                        show_custom_tost("❗Unable to connect");
                        dialog.dismiss();
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("d_model", Build.MODEL);
                    params.put("d_brand", Build.BRAND);
                    params.put("d_version", Build.VERSION.RELEASE);
                    params.put("d_api_level", Build.VERSION.SDK);
                    params.put("connection_status", "Connected");
                    return params;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(stringRequest);
        });

        dialog.show();
    }



    public ForwardingConfig populateConfig(View view, Context context, ForwardingConfig config) {
        final EditText senderInput = view.findViewById(R.id.input_phone);
        String sender = senderInput.getText().toString();
        if (TextUtils.isEmpty(sender)) {
            senderInput.setError(context.getString(R.string.error_empty_sender));
            return null;
        }

        final EditText urlInput = view.findViewById(R.id.input_url);
        String url = urlInput.getText().toString();
        if (TextUtils.isEmpty(url)) {
            urlInput.setError(context.getString(R.string.error_empty_url));
            return null;
        }
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            urlInput.setError(context.getString(R.string.error_wrong_url));
            return null;
        }

        Spinner simSlotSelector = (Spinner) view.findViewById(R.id.input_sim_slot);
        int simSlot = (int) simSlotSelector.getSelectedItemId();
        config.setSimSlot(simSlot);

        final EditText templateInput = view.findViewById(R.id.input_json_template);
        String template = templateInput.getText().toString();
        try {
            new JSONObject(template);
        } catch (JSONException e) {
            templateInput.setError(context.getString(R.string.error_wrong_json));
            return null;
        }

        final EditText headersInput = view.findViewById(R.id.input_json_headers);
        String headers = headersInput.getText().toString();
        try {
            new JSONObject(headers);
        } catch (JSONException e) {
            headersInput.setError(context.getString(R.string.error_wrong_json));
            return null;
        }

        final EditText retriesNumInput = view.findViewById(R.id.input_number_retries);
        int retriesNum = Integer.parseInt(retriesNumInput.getText().toString());
        if (retriesNum < 0) {
            retriesNumInput.setError(context.getString(R.string.error_wrong_retries_number));
            return null;
        }

        final CheckBox ignoreSslCheckbox = view.findViewById(R.id.input_ignore_ssl);
        boolean ignoreSsl = ignoreSslCheckbox.isChecked();

        final CheckBox chunkedModeCheckbox = view.findViewById(R.id.input_chunked_mode);
        boolean chunkedMode = chunkedModeCheckbox.isChecked();

        config.setSender(sender);
        config.setUrl(url);
        config.setTemplate(template);
        config.setHeaders(headers);
        config.setRetriesNumber(retriesNum);
        config.setIgnoreSsl(ignoreSsl);
        config.setChunkedMode(chunkedMode);

        return config;
    }
    private void prepareSimSelector(Context context, View view, int selected) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager subscriptionManager =
                    (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            int simSlots = subscriptionManager.getActiveSubscriptionInfoCountMax();
            if (simSlots > 1) {


                Spinner simSlotSelector = (Spinner) view.findViewById(R.id.input_sim_slot);
                simSlotSelector.setVisibility(View.VISIBLE);

                String[] items = new String[simSlots + 1];
                items[0] = "any";
                for (int i = 1; i <= simSlots; i++) {
                    items[i] = "sim" + i;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                        android.R.layout.simple_spinner_dropdown_item, items);
                simSlotSelector.setAdapter(adapter);

                if (selected > simSlots || selected < 0) {
                    selected = 0;
                }

                simSlotSelector.setSelection(selected);
            }
        }
    }


    private void show_custom_tost(String message) {
        View rootView = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay() != null ? ((android.app.Activity) context).findViewById(android.R.id.content) : null;

        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(context.getResources().getColor(R.color.main_color))
                    .setTextColor(Color.WHITE)
                    .setAction("Hide", undo -> {if (currentToast[0] != null) currentToast[0].cancel();})
                    .setActionTextColor(context.getResources().getColor(R.color.white))
                    .show();
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show(); // fallback
        }
        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.tost_sound);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
    }
}
