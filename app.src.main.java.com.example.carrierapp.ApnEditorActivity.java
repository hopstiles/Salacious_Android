package com.example.carrierapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ApnEditorActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApnAdapter adapter;
    private int subId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apn_editor);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        subId = getIntent().getIntExtra("sub_id", SubscriptionManager.getDefaultDataSubscriptionId());
        loadApns();
    }

    private void loadApns() {
        List<ApnData> apnList = new ArrayList<>();
        Uri apnUri = Uri.parse("content://telephony/carriers");
        Uri subSpecificUri = Uri.withAppendedPath(apnUri, "subId/" + subId);

        try (Cursor cursor = getContentResolver().query(subSpecificUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME));
                    String apn = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE));
                    
                    ApnData data = new ApnData(id, name, apn, type);
                    
                    // Optional fields
                    int proxyIndex = cursor.getColumnIndex(Telephony.Carriers.PROXY);
                    if (proxyIndex != -1) data.proxy = cursor.getString(proxyIndex);
                    
                    int portIndex = cursor.getColumnIndex(Telephony.Carriers.PORT);
                    if (portIndex != -1) data.port = cursor.getString(portIndex);

                    apnList.add(data);
                } while (cursor.moveToNext());
            } else {
                 Toast.makeText(this, "No APNs found.", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
             new AlertDialog.Builder(this)
                .setTitle("Access Denied")
                .setMessage("Cannot read APN settings.\n\nCarrier Privileges missing.\nVerify your SIM certificate hash.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
            return;
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        adapter = new ApnAdapter(apnList, this::showEditDialog);
        recyclerView.setAdapter(adapter);
    }

    private void showEditDialog(ApnData apnData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit APN: " + apnData.name);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputName = addField(layout, "Name", apnData.name);
        final EditText inputApn = addField(layout, "APN", apnData.apn);
        final EditText inputProxy = addField(layout, "Proxy", apnData.proxy);
        final EditText inputPort = addField(layout, "Port", apnData.port);
        final EditText inputType = addField(layout, "Type", apnData.type);

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            updateApn(apnData.id, 
                      inputName.getText().toString(),
                      inputApn.getText().toString(),
                      inputProxy.getText().toString(),
                      inputPort.getText().toString(),
                      inputType.getText().toString());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private EditText addField(LinearLayout layout, String label, String value) {
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(label);
        tv.setPadding(0, 20, 0, 5);
        layout.addView(tv);
        EditText et = new EditText(this);
        et.setText(value != null ? value : "");
        layout.addView(et);
        return et;
    }

    private void updateApn(String id, String name, String apn, String proxy, String port, String type) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Carriers.NAME, name);
        values.put(Telephony.Carriers.APN, apn);
        values.put(Telephony.Carriers.PROXY, proxy);
        values.put(Telephony.Carriers.PORT, port);
        values.put(Telephony.Carriers.TYPE, type);
        
        Uri uri = Uri.withAppendedPath(Uri.parse("content://telephony/carriers"), "subId/" + subId);
        String where = Telephony.Carriers._ID + " = ?";
        String[] args = new String[]{id};

        try {
            int rows = getContentResolver().update(uri, values, where, args);
            if (rows > 0) {
                Toast.makeText(this, "Success: APN Updated", Toast.LENGTH_SHORT).show();
                loadApns();
            } else {
                Toast.makeText(this, "Failed: No rows changed", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Security Error: Carrier Privileges Missing", Toast.LENGTH_LONG).show();
        }
    }

    public static class ApnData {
        String id, name, apn, type, proxy, port;
        public ApnData(String id, String name, String apn, String type) {
            this.id = id; this.name = name; this.apn = apn; this.type = type;
        }
    }
}