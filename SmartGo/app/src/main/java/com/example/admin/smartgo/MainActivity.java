package com.example.admin.smartgo;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Bluetooth";
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BroadcastReceiver broadcastReceiver = null;

    FloatingActionButton fabScan;
    ListView lvDevices;
    ArrayAdapter<String> adapterDevices;
    ArrayList<String> listDevices;
    ArrayList<String> listAddress;

    FloatingActionButton fabMain;
    LinearLayout layoutScan;
    LinearLayout layoutNext;

    private ProgressDialog progressDialog;
    private boolean register = false;

    private Animation fabMenuOpenAnimation;
    private Animation fabMenuCloseAnimation;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private boolean isFabMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        register = false;
        addControls();
        addEvents();
    }

    @Override
    public void onBackPressed() {
        if (isFabMenuOpen) {
            collapseFabMenu();
            isFabMenuOpen = false;
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        /*if (register){
            register = false;
            unregisterReceiver(broadcastReceiver);
            Log.i(TAG, String.valueOf(register));
        }*/

        try {
            if (broadcastReceiver != null) {
                unregisterReceiver(broadcastReceiver);
            }
        }
        catch (IllegalArgumentException e){
            Log.i(TAG, e.getMessage());
        }

        super.onStop();
    }

    private void askPermission() {
        int buildVer = Build.VERSION.SDK_INT;
        if (buildVer >= 23){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                Log.i("permission", "ACCESS_FINE_LOCATION granted");
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                Log.i("permission", "ACCESS_FINE_LOCATION revoke");
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED){
            Log.i("permission", "ACCESS_NETWORK_STATE granted");
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 1);
            Log.i("permission", "ACCESS_NETWORK_STATE revoke");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED){
            Log.i("permission", "ACCESS_WIFI_STATE granted");
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1);
            Log.i("permission", "ACCESS_WIFI_STATE revoke");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED){
            Log.i("permission", "INTERNET granted");
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
            Log.i("permission", "INTERNET revoke");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED){
            Log.i("permission", "BLUETOOTH granted");
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 1);
            Log.i("permission", "BLUETOOTH revoke");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED){
            Log.i("permission", "BLUETOOTH_ADMIN granted");
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
            Log.i("permission", "BLUETOOTH_ADMIN revoke");
        }
    }

    private void addEvents() {
        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                    isFabMenuOpen = false;
                }

                /*if (register){
                    register = false;
                    unregisterReceiver(broadcastReceiver);
                    Log.i(TAG, String.valueOf(register));
                }*/

                String address = listAddress.get(i);
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("address", address);
                startActivity(intent);
            }
        });

        fabMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                    isFabMenuOpen = false;
                }
                else {
                    expandFabMenu();
                    isFabMenuOpen = true;
                }
            }
        });

        layoutScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                    isFabMenuOpen = false;
                }

                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Scanning, Please wait...");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();

                listAddress.clear();
                listDevices.clear();

                btAdapter.startDiscovery();
                broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        register = true;
                        String action = intent.getAction();

                        //Finding devices
                        if (BluetoothDevice.ACTION_FOUND.equals(action))
                        {
                            // Get the BluetoothDevice object from the Intent
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            // Add the name and address to an array adapter to show in a ListView
                            if (listAddress.size() == 0){
                                listDevices.add(device.getName() + " | " + device.getAddress());
                                listAddress.add(device.getAddress());
                                adapterDevices.notifyDataSetChanged();
                            }
                            else {
                                if (!listAddress.contains(device.getAddress())){
                                    listDevices.add(device.getName() + " | " + device.getAddress());
                                    listAddress.add(device.getAddress());
                                    adapterDevices.notifyDataSetChanged();
                                }
                            }

                            progressDialog.dismiss();
                        }
                    }
                };

                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(broadcastReceiver, filter);
            }
        });

        layoutNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                    isFabMenuOpen = false;
                }

                String address = "";
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("address", address);
                startActivity(intent);
            }
        });
    }

    private void addControls() {
        fabScan = (FloatingActionButton) findViewById(R.id.fabScan);
        fabMain = (FloatingActionButton) findViewById(R.id.fabMain);

        layoutScan = (LinearLayout) findViewById(R.id.layoutScan);
        layoutNext = (LinearLayout) findViewById(R.id.layoutNext);

        fabMenuCloseAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab_closing_menu);
        fabMenuOpenAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab_openning_menu);
        fabCloseAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab_closing);
        fabOpenAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab_openning);

        listAddress = new ArrayList<>();
        listDevices = new ArrayList<>();
        adapterDevices = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, listDevices);
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        lvDevices.setAdapter(adapterDevices);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            Log.i(TAG, "Bluetooth Not supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.i(TAG, "...Bluetooth is enabled...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void expandFabMenu() {
        layoutNext.startAnimation(fabMenuOpenAnimation);
        layoutScan.startAnimation(fabMenuOpenAnimation);
        fabMain.startAnimation(fabOpenAnimation);

        layoutNext.setVisibility(View.VISIBLE);
        layoutScan.setVisibility(View.VISIBLE);

        layoutNext.setClickable(true);
        layoutScan.setClickable(true);
    }

    private void collapseFabMenu() {
        layoutScan.startAnimation(fabMenuCloseAnimation);
        layoutNext.startAnimation(fabMenuCloseAnimation);
        fabMain.startAnimation(fabCloseAnimation);

        layoutScan.setVisibility(View.INVISIBLE);
        layoutNext.setVisibility(View.INVISIBLE);

        layoutNext.setClickable(false);
        layoutScan.setClickable(false);
    }
}