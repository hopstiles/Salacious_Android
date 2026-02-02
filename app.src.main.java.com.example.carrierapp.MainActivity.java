package com.example.carrierapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CarrierConfigManager carrierConfigManager;
    private SubscriptionManager subscriptionManager;
    private int activeSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fabReset = findViewById(R.id.fab_reset);
        fabReset.setOnClickListener(v -> resetConfig());

        carrierConfigManager = getSystemService(CarrierConfigManager.class);
        subscriptionManager = getSystemService(SubscriptionManager.class);

        loadCarrierConfig();
    }

    private void loadCarrierConfig() {
        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE}, 101);
            return;
        }

        List<SubscriptionInfo> activeSubs = subscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubs == null || activeSubs.isEmpty()) {
            showError("No active SIM card found.");
            return;
        }

        SubscriptionInfo subInfo = activeSubs.get(0);
        activeSubId = subInfo.getSubscriptionId();

        TelephonyManager tm = getSystemService(TelephonyManager.class)
                .createForSubscriptionId(activeSubId);

        if (!tm.hasCarrierPrivileges()) {
            showError("No Carrier Privileges on SIM Slot " + subInfo.getSimSlotIndex() +
                    ".\nCheck your certificate hash!");
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle("Carrier: " + subInfo.getCarrierName());
        }

        PersistableBundle fetchedBundle = carrierConfigManager.getConfigForSubId(activeSubId);
        if (fetchedBundle == null) fetchedBundle = new PersistableBundle();

        // --- SMART DEBUG DUMP ---
        Log.d("CarrierConfig", "--- STARTING DUMP ---");
        List<String> sortedKeys = new ArrayList<>(fetchedBundle.keySet());
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            Object value = fetchedBundle.get(key);
            String printValue;
            if (value instanceof String[]) printValue = Arrays.toString((String[]) value);
            else if (value instanceof int[]) printValue = Arrays.toString((int[]) value);
            else if (value instanceof long[]) printValue = Arrays.toString((long[]) value);
            else if (value instanceof boolean[]) printValue = Arrays.toString((boolean[]) value);
            else printValue = String.valueOf(value);
            Log.d("CarrierConfig", key + " = " + printValue);
        }
        Log.d("CarrierConfig", "--- END DUMP ---");
        // ------------------------

        final PersistableBundle finalBundle = fetchedBundle;

        CarrierConfigAdapter adapter = new CarrierConfigAdapter(finalBundle, new CarrierConfigAdapter.OnConfigChangeListener() {
            @Override
            public void onConfigChanged(String key, Object newValue) {
                if ("EDIT_REQUEST".equals(newValue)) {
                    showEditDialog(key, finalBundle);
                } else {
                    applyOverride(key, newValue);
                }
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void showEditDialog(String key, PersistableBundle bundle) {
        Object currentValue = bundle.get(key);
        EditText input = new EditText(this);
        input.setText(String.valueOf(currentValue));

        new AlertDialog.Builder(this)
                .setTitle("Edit Config")
                .setMessage(key)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String text = input.getText().toString();
                    Object finalValue = text;
                    if (currentValue instanceof Integer) {
                        try { finalValue = Integer.parseInt(text); } catch (Exception e) {}
                    } else if (currentValue instanceof Long) {
                        try { finalValue = Long.parseLong(text); } catch (Exception e) {}
                    }
                    applyOverride(key, finalValue);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyOverride(String key, Object value) {
        if (activeSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "Override requires Android 10+", Toast.LENGTH_SHORT).show();
            return;
        }

        PersistableBundle overrideBundle = new PersistableBundle();
        if (value instanceof Boolean) overrideBundle.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) overrideBundle.putInt(key, (Integer) value);
        else if (value instanceof Long) overrideBundle.putLong(key, (Long) value);
        else overrideBundle.putString(key, String.valueOf(value));

        try {
            carrierConfigManager.overrideConfig(activeSubId, overrideBundle);
            Toast.makeText(this, "Applied: " + key, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            showError("Security Exception: You do not have Carrier Privileges.");
        }
    }

    private void resetConfig() {
        if (activeSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;

        try {
            carrierConfigManager.overrideConfig(activeSubId, null);
            Toast.makeText(this, "Reset to Defaults", Toast.LENGTH_LONG).show();
            loadCarrierConfig();
        } catch (SecurityException e) {
            showError("Failed to reset config.");
        }
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_config, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (recyclerView.getAdapter() instanceof CarrierConfigAdapter) {
                    ((CarrierConfigAdapter) recyclerView.getAdapter()).filter(newText);
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            loadCarrierConfig();
            return true;
        } else if (id == R.id.action_reset) {
            resetConfig();
            return true;
        } else if (id == R.id.action_edit_apn) {
            if (activeSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                showError("No active SIM selected.");
            } else {
                Intent intent = new Intent(this, ApnEditorActivity.class);
                intent.putExtra("sub_id", activeSubId);
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            loadCarrierConfig();
        }
    }
}