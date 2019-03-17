package com.example.admin.smartgo;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    String DATABASE_NAME="dbSmartGo.sqlite";
    private static final String DB_PATH_SUFFIX = "/databases/";
    SQLiteDatabase database=null;

    private static final String TAG = "Bluetooth";
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    // HC-06 UUID: 00001101-0000-1000-8000-00805F9B34FB
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Server's MAC address
    private static String address;

    FloatingActionButton fabSearch;
    FloatingActionButton fabMaps;
    LinearLayout layoutRoute;
    LinearLayout layoutSend;
    LinearLayout layoutClear;
    LinearLayout layoutReport;
    Toolbar toolbar;
    DrawerLayout drawer;
    ActionBarDrawerToggle toggle;
    NavigationView navigationView;

    private Animation fabMenuOpenAnimation;
    private Animation fabMenuCloseAnimation;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private boolean isFabMenuOpen = false;

    LatLng source;
    LatLng destination;
    ConnectAsyncTask connectAsyncTask;
    boolean doAsyncTask;
    SendRouteToDeviceViaBluetooth sendRouteToDeviceViaBluetooth;
    Polyline lines;

    // Route
    ArrayList<String> arrPath = new ArrayList<>();
    ArrayList<String> arrPathFinal = new ArrayList<>();

    // Street name
    ArrayList<String> arrStreet = new ArrayList<>();

    // Latitude
    ArrayList<String> arrLat = new ArrayList<>();
    ArrayList<String> arrLat1 = new ArrayList<>();
    ArrayList<String> arrLat2 = new ArrayList<>();
    ArrayList<String> arrLatFinal = new ArrayList<>();

    // Longitude
    ArrayList<String> arrLng = new ArrayList<>();
    ArrayList<String> arrLng1 = new ArrayList<>();
    ArrayList<String> arrLng2 = new ArrayList<>();
    ArrayList<String> arrLngFinal = new ArrayList<>();

    // 0: straight
    // 1: left
    // 2: right
    ArrayList<String> arrTurn = new ArrayList<>();
    ArrayList<String> arrTurnFinal = new ArrayList<>();

    ArrayList<Integer> headLoc = new ArrayList<>();

    private static final String[] preConst = {
            "toward",
            "onto",
            "on"
    };

    private static final String[] endConst = {
            "Pass",
            "Destination"
    };

    String sendBluetooth = "";
    String route = "";

    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    boolean firstOpen = true;
    private GoogleMap mMap;
    Marker markerLocation = null;
    Marker markerDestination = null;
    double desLat;
    double desLng;
    GoogleMap.OnMyLocationChangeListener locationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            source = new LatLng(location.getLatitude(),location.getLongitude());
            if (markerLocation != null){
                markerLocation.remove();
            }
            markerLocation = mMap.addMarker(new MarkerOptions().position(source).title("My Location"));

            if (firstOpen){
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(source,15.5f));
                firstOpen = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Intent intent = getIntent();
        address = intent.getStringExtra("address");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapBike);
        if(mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (isFabMenuOpen) {
                collapseFabMenu();
                isFabMenuOpen = false;
            }
            else {
                if (sendRouteToDeviceViaBluetooth != null) {
                    sendRouteToDeviceViaBluetooth.cancel(true);
                }

                doAsyncTask = false;
                if (connectAsyncTask != null){
                    connectAsyncTask.cancel(true);
                }

                if (outStream != null) {
                    try {
                        outStream.flush();
                    } catch (IOException e) {
                        Log.i(TAG, "In onBackPressed() and failed to flush output stream: " + e.getMessage() + ".");
                    }
                }

                if (!address.equals("")) {
                    try     {
                        btSocket.close();
                    } catch (IOException e2) {
                        Log.i(TAG, "In onBackPressed() and failed to close socket." + e2.getMessage() + ".");
                    }
                }

                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onStop() {
        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                Log.i(TAG, "In onStop() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        if (!address.equals("")) {
            try     {
                btSocket.close();
            } catch (IOException e2) {
                Log.i(TAG, "In onStop() and failed to close socket." + e2.getMessage() + ".");
            }
        }

        super.onStop();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getCurrentPosition();

        processCopy();
        getDestinationFromDatabase();
        if (desLat <= 360 && desLng <= 360) {
            destination = new LatLng(desLat, desLng);
            if (markerDestination != null) {
                markerDestination.remove();
            }
            markerDestination = mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
        }

        addControls();
        addEvents();

        if (!address.equals("")) {
            connectBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);

                destination = place.getLatLng();
                desLat = destination.latitude;
                desLng = destination.longitude;

                if (markerDestination != null) {
                    markerDestination.remove();
                }
                markerDestination = mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination,15.5f));

                setDestinationToDatabase();

                Log.i("LOCATION", "Place: " + place.getName());
            }
            else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("LOCATION", status.getStatusMessage());
            }
            else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    private void connectBluetooth() {
        ProgressDialog progressDialog = new ProgressDialog(MapsActivity.this);
        progressDialog.setMessage("Connecting to Bluetooth, Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        if (btAdapter.isEnabled()){
            // Set up a pointer to the remote node using it's address.
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // Two things are needed to make a connection:
            //   A MAC address, which we got above.
            //   A Service ID or UUID.  In this case we are using the UUID for SPP.
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.i(TAG, "In onResume() and socket create failed: " + e.getMessage() + ".");
            }

            // Discovery is resource intensive.  Make sure it isn't going on
            // when you attempt to connect and pass your message.
            btAdapter.cancelDiscovery();

            // Establish the connection.  This will block until it connects.
            Log.i(TAG, "...Connecting to Remote...");
            try {
                btSocket.connect();
                Log.i(TAG, "...Connection established and data link opened...");
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.i(TAG, "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }

            // Create a data stream so we can talk to server.
            Log.i(TAG, "...Creating Socket...");

            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
                Log.i(TAG, "In onResume() and output stream creation failed:" + e.getMessage() + ".");
            }
        }

        progressDialog.dismiss();
    }

    private void addEvents() {
        fabMaps.setOnClickListener(new View.OnClickListener() {
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

        layoutRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                    isFabMenuOpen = false;
                }

                if (source != null && destination != null){
                    Log.i("LOCATION",Double.toString(source.latitude));
                    Log.i("LOCATION",Double.toString(source.longitude));
                    Log.i("LOCATION",Double.toString(destination.latitude));
                    Log.i("LOCATION",Double.toString(destination.longitude));

                    String url = makeURL(source.latitude, source.longitude, destination.latitude, destination.longitude);
                    doAsyncTask = true;
                    connectAsyncTask = new ConnectAsyncTask(url);
                    connectAsyncTask.execute();
                }
            }
        });

        layoutSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                    isFabMenuOpen = false;
                }

                sendRouteToDeviceViaBluetooth = new SendRouteToDeviceViaBluetooth(MapsActivity.this, route);
                sendRouteToDeviceViaBluetooth.execute();
            }
        });

        if (mMap != null){
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    if (markerDestination != null){
                        markerDestination.remove();
                    }
                    markerDestination = mMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));
                    destination = latLng;

                    desLat = latLng.latitude;
                    desLng = latLng.longitude;

                    setDestinationToDatabase();
                }
            });
        }

        layoutClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                    isFabMenuOpen = false;
                }

                if (markerDestination != null) {
                    markerDestination.remove();
                }
                if (lines != null) {
                    lines.remove();
                }
                desLat = 361;
                desLng = 361;
                destination = null;

                setDestinationToDatabase();
            }
        });

        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                    .build(MapsActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }
            }
        });

        layoutReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                    isFabMenuOpen = false;
                }

                Intent intent = new Intent(MapsActivity.this, ReportActivity.class);
                startActivity(intent);
            }
        });
    }

    private void addControls() {
        fabMaps = (FloatingActionButton) findViewById(R.id.fabMaps);
        fabSearch = (FloatingActionButton) findViewById(R.id.fabSearch);

        layoutRoute = (LinearLayout) findViewById(R.id.layoutRoute);
        layoutSend = (LinearLayout) findViewById(R.id.layoutSend);
        layoutClear = (LinearLayout) findViewById(R.id.layoutClear);
        layoutReport = (LinearLayout) findViewById(R.id.layoutReport);

        fabMenuCloseAnimation = AnimationUtils.loadAnimation(MapsActivity.this, R.anim.fab_closing_menu);
        fabMenuOpenAnimation = AnimationUtils.loadAnimation(MapsActivity.this, R.anim.fab_openning_menu);
        fabCloseAnimation = AnimationUtils.loadAnimation(MapsActivity.this, R.anim.fab_closing);
        fabOpenAnimation = AnimationUtils.loadAnimation(MapsActivity.this, R.anim.fab_openning);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void getCurrentPosition() {
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationChangeListener(locationChangeListener);
        }
    }

    public String makeURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString.append(Double.toString(sourcelog));
        urlString.append("&destination=");// to
        urlString.append(Double.toString(destlat));
        urlString.append(",");
        urlString.append(Double.toString(destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        urlString.append("&key=AIzaSyDe8qaCTZ-qcpLjHzNKYbQetS1zH7aFVqQ");
        return urlString.toString();
    }

    public void drawPath(String  result) {
        try {
            if (lines != null){
                lines.remove();
            }

            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);

            PolylineOptions options = new PolylineOptions().width(12).color(Color.BLUE).geodesic(true);
            for (int z = 0; z < list.size(); z++) {
                LatLng point = list.get(z); options.add(point);
            }
            lines = mMap.addPolyline(options);
        }
        catch (JSONException e) {
        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    private class ConnectAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;
        String url;
        ConnectAsyncTask(String urlPass){
            url = urlPass;
        }
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            progressDialog = new ProgressDialog(MapsActivity.this);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }
        @Override
        protected String doInBackground(Void... params) {
            String json = null;
            if (doAsyncTask) {
                JSONParser jParser = new JSONParser();
                json = jParser.getJSONFromUrl(url);
            }
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.i("LOCATION", result);

            getRoutes(result);

            progressDialog.dismiss();
            doAsyncTask = false;
            if(result!=null){
                drawPath(result);
            }
        }
    }

    private void getRoutes(String result) {
        Menu menu = navigationView.getMenu();
        menu.clear();
        route = "";

        route = result.replaceAll("\\\\u003cb\\\\u003e", "");
        route = route.replaceAll("\\\\u003c/b\\\\u003e", "");
        route = route.replaceAll("\\\\u003cdiv style=\\\\\"font-size:0.9em\\\\\"\\\\u003e", " ");
        route = route.replaceAll("\\\\u003c/div\\\\u003e", "");
        route = route.replaceAll("&nbsp;", "");
        Log.i("LOCATION",route);

        findPath(route, menu);
    }

    private void findTurn(String str) {
        arrTurn.clear();
        ArrayList<String> turn = new ArrayList<>();
        ArrayList<String> turnRaw = new ArrayList<>();
        turn.add("head");

        Pattern pattern = Pattern.compile("maneuver\" : \"(.*?)\",");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()){
            System.out.println(matcher.group());
            Log.i("LOCATION", matcher.group());

            String string = matcher.group();
            string = string.replaceAll("maneuver\" : \"", "");
            string = string.replaceAll("\",", "");

            turn.add(string);
        }

        int j = 0;
        for (int i = 0; i < arrPath.size(); i++){
            if (arrPath.get(i).toLowerCase().contains("head") || arrPath.get(i).toLowerCase().contains("straight") || arrPath.get(i).toLowerCase().contains("right") || arrPath.get(i).toLowerCase().contains("left") || arrPath.get(i).toLowerCase().contains("roundabout")){
                turnRaw.add(turn.get(j));
                Log.i(TAG, turnRaw.get(i));
                j++;
            }
            else {
                turnRaw.add(arrPath.get(i));
                Log.i(TAG, turnRaw.get(i));
            }
        }

        for (int i = 0; i < turnRaw.size(); i++) {
            if (turnRaw.get(i).contains("left")) {
                arrTurn.add("1");
            }
            else {
                if (turnRaw.get(i).contains("right")) {
                    arrTurn.add("2");
                }
                else {
                    arrTurn.add("0");
                }
            }
        }
    }

    private void findLng(String str) {
        ArrayList<String> lng = new ArrayList<>();
        arrLng.clear();
        arrLng1.clear();
        arrLng2.clear();

        Pattern p2 = Pattern.compile("lng\" : (.*?)\n");
        Matcher m2 = p2.matcher(str);
        while (m2.find()){
            System.out.println(m2.group());
            Log.i("LOCATION", m2.group());

            String string = m2.group();
            string = string.replaceAll("lng\" : ", "");
            string = string.replaceAll("\n", "");

            lng.add(string);
        }

        for (int i = 0; i < arrPath.size()*2; i++){
            if (i % 2 != 0){
                arrLng.add(lng.get(i+4));
            }
        }

        for (int i = 0; i < arrLng.size(); i++){
            int iend = arrLng.get(i).indexOf(".");
            if (iend != -1) {
                arrLng1.add(arrLng.get(i).substring(0, iend));
                arrLng2.add(arrLng.get(i).replaceAll(arrLng1.get(i) + ".", ""));
            }
        }
    }

    private void findLat(String str) {
        ArrayList<String> lat = new ArrayList<>();
        arrLat.clear();
        arrLat1.clear();
        arrLat2.clear();

        Pattern p1 = Pattern.compile("lat\" : (.*?),");
        Matcher m1 = p1.matcher(str);
        while (m1.find()){
            System.out.println(m1.group());
            Log.i("LOCATION", m1.group());

            String string = m1.group();
            string = string.replaceAll("lat\" : ", "");
            string = string.replaceAll(",", "");

            lat.add(string);
        }

        for (int i = 0; i < arrPath.size()*2; i++){
            if (i % 2 != 0){
                arrLat.add(lat.get(i+4));
            }
        }

        for (int i = 0; i < arrLat.size(); i++){
            int iend = arrLat.get(i).indexOf(".");
            if (iend != -1) {
                arrLat1.add(arrLat.get(i).substring(0, iend));
                arrLat2.add(arrLat.get(i).replaceAll(arrLat1.get(i) + ".", ""));
            }
        }
    }

    private void findPath(String str, Menu menu) {
        arrPath.clear();

        Pattern pattern = Pattern.compile("html_instructions\" : \"(.*?)\",");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            System.out.println(matcher.group());
            Log.i("LOCATION",matcher.group());

            String string = matcher.group();
            string = string.replaceAll("html_instructions\" : \"", "");
            string = string.replaceAll("\",","");

            if (menu.size() == 0){
                menu.add(string);
                arrPath.add(string);
            }
            else {
                if (string.contains("Head")){
                    return;
                }
                else {
                    menu.add(string);
                    arrPath.add(string);
                }
            }
        }
    }

    private void findStreet (ArrayList<String> paths) {
        arrStreet.clear();
        String pre;
        String end;
        String name;

        for (String path : paths) {
            pre = findPre(path);
            end = findEnd(path);
            if (!pre.equals("")) {
                if (!end.equals("")) {
                    if (path.indexOf(end) > path.indexOf(pre)) {
                        name = path.substring(path.indexOf(pre) + pre.length() + 1, path.indexOf(end));
                    }
                    else {
                        name = " ";
                    }
                }
                else {
                    name = path.substring(path.indexOf(pre) + pre.length() + 1, path.length());
                }
            }
            else {
                name = " ";
            }
            arrStreet.add(name);
        }
    }

    private void checkBTState() {
        if (!address.equals("")) {
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
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Sending data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In sendData() and an exception occurred during write: " + e.getMessage();
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
            Log.i(TAG, msg);
        }
    }

    private void expandFabMenu() {
        if (!address.equals("")) {
            layoutSend.startAnimation(fabMenuOpenAnimation);
            layoutSend.setVisibility(View.VISIBLE);
            layoutSend.setClickable(true);
        }

        layoutRoute.startAnimation(fabMenuOpenAnimation);
        layoutRoute.setVisibility(View.VISIBLE);
        layoutRoute.setClickable(true);

        layoutClear.startAnimation(fabMenuOpenAnimation);
        layoutClear.setVisibility(View.INVISIBLE);
        layoutClear.setClickable(true);

        layoutReport.startAnimation(fabMenuOpenAnimation);
        layoutReport.setVisibility(View.VISIBLE);
        layoutReport.setClickable(true);

        fabMaps.startAnimation(fabOpenAnimation);
    }

    private void collapseFabMenu() {
        if (!address.equals("")) {
            layoutSend.startAnimation(fabMenuCloseAnimation);
            layoutSend.setVisibility(View.INVISIBLE);
            layoutSend.setClickable(false);
        }

        layoutRoute.startAnimation(fabMenuCloseAnimation);
        layoutRoute.setVisibility(View.INVISIBLE);
        layoutRoute.setClickable(false);

        layoutClear.startAnimation(fabMenuCloseAnimation);
        layoutClear.setVisibility(View.INVISIBLE);
        layoutClear.setClickable(false);

        layoutReport.startAnimation(fabMenuCloseAnimation);
        layoutReport.setVisibility(View.INVISIBLE);
        layoutReport.setClickable(false);

        fabMaps.startAnimation(fabCloseAnimation);
    }

    private String convertString (String str) {
        str = str.replaceAll("á", "a");
        str = str.replaceAll("à", "a");
        str = str.replaceAll("ả", "a");
        str = str.replaceAll("ã", "a");
        str = str.replaceAll("ạ", "a");
        str = str.replaceAll("ắ", "a");
        str = str.replaceAll("ằ", "a");
        str = str.replaceAll("ẳ", "a");
        str = str.replaceAll("ẵ", "a");
        str = str.replaceAll("ặ", "a");
        str = str.replaceAll("ă", "a");
        str = str.replaceAll("â", "a");
        str = str.replaceAll("ấ", "a");
        str = str.replaceAll("ầ", "a");
        str = str.replaceAll("ẩ", "a");
        str = str.replaceAll("ẫ", "a");
        str = str.replaceAll("ậ", "a");
        str = str.replaceAll("ó", "o");
        str = str.replaceAll("ò", "o");
        str = str.replaceAll("ỏ", "o");
        str = str.replaceAll("õ", "o");
        str = str.replaceAll("ọ", "o");
        str = str.replaceAll("ô", "o");
        str = str.replaceAll("ố", "o");
        str = str.replaceAll("ồ", "o");
        str = str.replaceAll("ỗ", "o");
        str = str.replaceAll("ộ", "o");
        str = str.replaceAll("ổ", "o");
        str = str.replaceAll("ơ", "o");
        str = str.replaceAll("ớ", "o");
        str = str.replaceAll("ờ", "o");
        str = str.replaceAll("ở", "o");
        str = str.replaceAll("ỡ", "o");
        str = str.replaceAll("ợ", "o");
        str = str.replaceAll("é", "e");
        str = str.replaceAll("è", "e");
        str = str.replaceAll("ẻ", "e");
        str = str.replaceAll("ẽ", "e");
        str = str.replaceAll("ẹ", "e");
        str = str.replaceAll("ê", "e");
        str = str.replaceAll("ế", "e");
        str = str.replaceAll("ề", "e");
        str = str.replaceAll("ể", "e");
        str = str.replaceAll("ễ", "e");
        str = str.replaceAll("ệ", "e");
        str = str.replaceAll("ú", "u");
        str = str.replaceAll("ù", "u");
        str = str.replaceAll("ủ", "u");
        str = str.replaceAll("ũ", "u");
        str = str.replaceAll("ụ", "u");
        str = str.replaceAll("ư", "u");
        str = str.replaceAll("ứ", "u");
        str = str.replaceAll("ừ", "u");
        str = str.replaceAll("ử", "u");
        str = str.replaceAll("ữ", "u");
        str = str.replaceAll("ự", "u");
        str = str.replaceAll("đ", "d");
        str = str.replaceAll("í", "i");
        str = str.replaceAll("ì", "i");
        str = str.replaceAll("ỉ", "i");
        str = str.replaceAll("ĩ", "i");
        str = str.replaceAll("ị", "i");
        str = str.replaceAll("ý", "y");
        str = str.replaceAll("ỳ", "y");
        str = str.replaceAll("ỷ", "y");
        str = str.replaceAll("ỹ", "y");
        str = str.replaceAll("ỵ", "y");
        str = str.replaceAll("Á", "A");
        str = str.replaceAll("À", "A");
        str = str.replaceAll("Ả", "A");
        str = str.replaceAll("Ã", "A");
        str = str.replaceAll("Ạ", "A");
        str = str.replaceAll("Ă", "A");
        str = str.replaceAll("Ắ", "A");
        str = str.replaceAll("Ằ", "A");
        str = str.replaceAll("Ẳ", "A");
        str = str.replaceAll("Ẵ", "A");
        str = str.replaceAll("Ặ", "A");
        str = str.replaceAll("Â", "A");
        str = str.replaceAll("Ấ", "A");
        str = str.replaceAll("Ầ", "A");
        str = str.replaceAll("Ẩ", "A");
        str = str.replaceAll("Ẫ", "A");
        str = str.replaceAll("Ậ", "A");
        str = str.replaceAll("Ó", "O");
        str = str.replaceAll("Ò", "O");
        str = str.replaceAll("Ỏ", "O");
        str = str.replaceAll("Õ", "O");
        str = str.replaceAll("Ọ", "O");
        str = str.replaceAll("Ô", "O");
        str = str.replaceAll("Ố", "O");
        str = str.replaceAll("Ồ", "O");
        str = str.replaceAll("Ổ", "O");
        str = str.replaceAll("Ỗ", "O");
        str = str.replaceAll("Ộ", "O");
        str = str.replaceAll("Ơ", "O");
        str = str.replaceAll("Ớ", "O");
        str = str.replaceAll("Ờ", "O");
        str = str.replaceAll("Ở", "O");
        str = str.replaceAll("Ỡ", "O");
        str = str.replaceAll("Ợ", "O");
        str = str.replaceAll("É", "E");
        str = str.replaceAll("È", "E");
        str = str.replaceAll("Ẻ", "E");
        str = str.replaceAll("Ẽ", "E");
        str = str.replaceAll("Ê", "E");
        str = str.replaceAll("Ế", "E");
        str = str.replaceAll("Ề", "E");
        str = str.replaceAll("Ể", "E");
        str = str.replaceAll("Ễ", "E");
        str = str.replaceAll("Ệ", "E");
        str = str.replaceAll("Đ", "D");
        str = str.replaceAll("Ú", "U");
        str = str.replaceAll("Ù", "U");
        str = str.replaceAll("Ủ", "U");
        str = str.replaceAll("Ũ", "U");
        str = str.replaceAll("Ụ", "U");
        str = str.replaceAll("Ư", "U");
        str = str.replaceAll("Ứ", "U");
        str = str.replaceAll("Ừ", "U");
        str = str.replaceAll("Ử", "U");
        str = str.replaceAll("Ữ", "U");
        str = str.replaceAll("Ự", "U");
        str = str.replaceAll("Í", "I");
        str = str.replaceAll("Ì", "I");
        str = str.replaceAll("Ỉ", "I");
        str = str.replaceAll("Ĩ", "I");
        str = str.replaceAll("Ị", "I");
        str = str.replaceAll("Ý", "Y");
        str = str.replaceAll("Ỳ", "Y");
        str = str.replaceAll("Ỷ", "Y");
        str = str.replaceAll("Ỹ", "Y");
        str = str.replaceAll("Ỵ", "Y");

        return str;
    }

    private String findPre (String str) {
        for (String aPreConst : preConst) {
            if (str.contains(aPreConst)) {
                Log.i("LOCATION", aPreConst);
                return aPreConst;
            }
        }
        Log.i("LOCATION", "null");
        return "";
    }

    private String findEnd (String str) {
        for (String aEndConst : endConst) {
            if (str.contains(aEndConst)) {
                Log.i("LOCATION", aEndConst);
                return aEndConst;
            }
        }
        Log.i("LOCATION", "null");
        return "";
    }

    private class SendRouteToDeviceViaBluetooth extends AsyncTask<Integer, Integer, String> {
        private ProgressDialog dialog;
        private String route;

        private SendRouteToDeviceViaBluetooth(MapsActivity activity, String route) {
            dialog = new ProgressDialog(activity);
            dialog.setCancelable(false);

            this.route = route;
        }

        @Override
        protected String doInBackground(Integer... integers) {
            try {
                sendBluetooth = "";

                findLat(route);
                findLng(route);
                findTurn(route);
                findStreet(arrPath);

                publishProgress(arrPath.size());

                for (int i = 0; i < arrPath.size(); i++) {
                    sendBluetooth = "(" + arrTurn.get(i) + "," + arrLat.get(i) + "," + arrLng.get(i) + "," + arrStreet.get(i) + ")\0";
                    sendBluetooth = convertString(sendBluetooth);
                    sendData(sendBluetooth);

                    Thread.sleep(1000);


                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog.setMessage("Sending route to device, Please wait...");
            dialog.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    private void setDestinationToDatabase() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("desLat", desLat);
        database.update("Destination",contentValues,"id=?",new String[]{"0"});

        ContentValues contentValues1 = new ContentValues();
        contentValues1.put("desLng", desLng);
        database.update("Destination",contentValues1,"id=?",new String[]{"0"});
    }

    private void getDestinationFromDatabase() {
        database = openOrCreateDatabase(DATABASE_NAME,MODE_PRIVATE,null);
        Cursor cursor = database.rawQuery("select * from Destination",null);
        cursor.moveToFirst();
        do {
            desLat = cursor.getDouble(1);
            desLng = cursor.getDouble(2);
        }
        while (cursor.moveToNext());
        cursor.close();
    }

    private void processCopy() {
        File dbFile = getDatabasePath(DATABASE_NAME);
        if (!dbFile.exists()){
            try{
                CopyDataBaseFromAsset();
            }
            catch (Exception e){
            }
        }
    }

    private void CopyDataBaseFromAsset() {
        try {
            InputStream myInput;

            myInput = getAssets().open(DATABASE_NAME);

            //Path to the just created empty db
            String outFileName = getDatabasePath();

            //if the path doesn't exist first, create it
            File f = new File(getApplicationInfo().dataDir + DB_PATH_SUFFIX);
            if (!f.exists())
                f.mkdir();

            // Open the empty db as the output stream
            OutputStream myOutput = new FileOutputStream(outFileName);

            // transfer bytes from the input file to the output file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }

            // Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDatabasePath(){
        return getApplicationInfo().dataDir + DB_PATH_SUFFIX+ DATABASE_NAME;
    }
}
