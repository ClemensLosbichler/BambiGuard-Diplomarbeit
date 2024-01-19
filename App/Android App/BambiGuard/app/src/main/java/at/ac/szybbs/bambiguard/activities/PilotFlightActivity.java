package at.ac.szybbs.bambiguard.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.ProjectedMeters;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.dji.DJIApplication;
import at.ac.szybbs.bambiguard.fragments.ConnectDroneDialogFragment;
import at.ac.szybbs.bambiguard.model.Bambi;
import at.ac.szybbs.bambiguard.model.BambiGuardDetector;
import at.ac.szybbs.bambiguard.model.BambiGuardMission;
import at.ac.szybbs.bambiguard.viewmodel.PilotViewModel;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.MissionState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class PilotFlightActivity extends AppCompatActivity implements SurfaceTextureListener, View.OnClickListener, View.OnTouchListener, OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private static final int SWITCH_CODEC_MANAGER_INTERVAL_MILLISECONDS = 300;
    private static final double MAP_ZOOM_FACTOR = 16D;
    private final WaypointMissionOperatorListener missionOperatorListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
        }

        @Override
        public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
        }

        @Override
        public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
        }

        @Override
        public void onExecutionStart() {
        }

        @Override
        public void onExecutionFinish(@Nullable DJIError djiError) {
            runOnUiThread(() -> showViewContinueToFlightOverview());
        }
    };
    protected VideoFeeder.VideoDataListener videoDataListener = null;
    protected DJICodecManager codecManager = null;
    protected TextureView textureView = null;
    protected boolean receiverIsRegistered = false;
    Handler codecManagerHandler;
    private Camera camera;
    private Button buttonStart;
    private Button buttonStop;
    private ConnectDroneDialogFragment connectDroneDialogFragment;
    private PilotViewModel viewModel;
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkAircraftConnection();
        }
    };
    private BambiGuardMission bambiGuardMission;
    private MapView mapView;
    private MapboxMap map;
    Runnable codecManagerTask = new Runnable() {
        @Override
        public void run() {
            codecManagerHandler.postDelayed(codecManagerTask, SWITCH_CODEC_MANAGER_INTERVAL_MILLISECONDS);

            checkAircraftConnection();

            if (!viewModel.getDroneConnected().getValue() || viewModel.getMissionState() != MissionState.EXECUTING)
                return;

            takeBitmapTextureViewData();
            setFlightStatusTextViews();
        }
    };
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;
    private ArrayList<Marker> bambiMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pilot_flight);

        initViewModel();
        showDialog();
        initUI();
        registerBroadCastReceiver();
        initMapView(savedInstanceState);
        checkAircraftConnection();
    }

    private void initMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        bambiMarkers = new ArrayList<>();
    }

    private void initBambiGuardMission() {
        ArrayList<Waypoint> waypoints = new ArrayList<>();
        for (LatLng point : viewModel.getWaypoints().getValue()) {
            point.setAltitude(20);
            waypoints.add(new Waypoint(point.getLatitude(), point.getLongitude(), (float) point.getAltitude()));
        }

        bambiGuardMission = new BambiGuardMission(viewModel.getWaypointMissionFinishedAction().getValue(), waypoints, missionOperatorListener);
        try {
            bambiGuardMission.configureWaypointMission();
        } catch(Exception e) {
            e.printStackTrace();
        }

        bambiGuardMission.addMissionOperatorListener();
        viewModel.setFlyingTime(bambiGuardMission.getTotalTime());
        viewModel.setFlightDistance(bambiGuardMission.getTotalDistance());
    }

    private void initViewModel() {
        viewModel = PilotViewModel.getInstance();
        viewModel.setConnectDroneDialogVisible(PilotViewModel.ConnectDroneDialog.Unknown);
    }

    private void showDialog() {
        connectDroneDialogFragment = new ConnectDroneDialogFragment();
        connectDroneDialogFragment.show(getSupportFragmentManager(), "connectDrone");
        viewModel.getConnectDroneDialogVisible().observe(this, connectDroneDialog -> {
            switch (connectDroneDialog) {
                case Continue:
                    onConnectDroneDialogPositive();
                    break;
                case Dismiss:
                    onConnectDroneDialogNegative();
                    break;
            }
        });
    }

    private void onConnectDroneDialogPositive() {
        initBambiGuardMission();
        startFlight();
    }

    private void startFlight() {
        bambiGuardMission.uploadWaypointMission();
    }

    private void cancelFlight() {
        runOnUiThread(this::showViewContinueToFlightOverview);
    }

    private void showViewContinueToFlightOverview() {
        Button buttonContinueToFlightOverview = findViewById(R.id.buttonContinueToFlightOverview);
        buttonContinueToFlightOverview.setOnClickListener(v -> startFlightEndActivity());
        View view = findViewById(R.id.layoutContinueToFlightOverview);
        view.setVisibility(View.VISIBLE);
    }

    private void startFlightEndActivity() {
        Intent intent = new Intent(this, FlightEndActivity.class);
        startActivity(intent);
    }

    private void onConnectDroneDialogNegative() {
        finish();
    }

    private void registerBroadCastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(receiver, filter);
        receiverIsRegistered = true;
    }

    private void initThermalCamera() {
        List<Camera> cameras = DJIApplication.getProductInstance().getCameras();
        Camera thermalCamera = null;

        if (cameras != null && cameras.size() > 1) {
            for (int i = 0; i < cameras.size(); i++) {
                if (cameras.get(i).isThermalCamera()) {
                    thermalCamera = cameras.get(i);
                }
            }
        }

        if (thermalCamera != null) {
            if (thermalCamera.isThermalCamera()) {
                thermalCamera.setDisplayMode(SettingsDefinitions.DisplayMode.THERMAL_ONLY, djiError -> {
                });
            }
        }
    }

    private void initCodecManager(SurfaceTexture surface, int width, int height) {
        codecManager = new DJICodecManager(this, surface, width, height);
    }

    private void notifyStatusChange() {
        BaseProduct product = DJIApplication.getProductInstance();

        HandlerThread handlerThread = new HandlerThread("CodecManagerHandlerThread");
        handlerThread.start();
        codecManagerHandler = new Handler(handlerThread.getLooper());
        codecManagerHandler.post(codecManagerTask);

        if (product == null || !product.isConnected()) {
            camera = null;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initUI() {
        textureView = findViewById(R.id.textureView);

        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(this);
        buttonStart.setOnTouchListener(this);
        buttonStop = findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(this);
        buttonStop.setOnTouchListener(this);
    }

    private void initTextureView() {
        textureView.setSurfaceTextureListener(this);
    }

    private void checkAircraftConnection() {
        runOnUiThread(() -> {
            BaseProduct product = DJIApplication.getProductInstance();

            if(product == null)
                Log.d("kek", "product is null shit");
            else
                Log.d("kek", "product connected " + product.isConnected());


            if (product != null && product.isConnected()) {
                viewModel.setDroneConnected(true);
                connectDroneDialogFragment.toggleDroneConnected(true);
                initThermalCamera();
            } else {
                DJIApplication.getInstance().registerApplication();
                viewModel.setDroneConnected(false);
                connectDroneDialogFragment.toggleDroneConnected(false);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private void setFlightStatusTextViews() {
        TextView textViewCompletion = findViewById(R.id.textViewCompletion);
        TextView textViewDuration = findViewById(R.id.textViewDuration);
        TextView textViewBambis = findViewById(R.id.textViewBambis);

        if (viewModel.getFlightDuration() == 0)
            return;
        float completion = viewModel.getFlyingTime() / viewModel.getFlightDuration() * 100;
        textViewCompletion.setText(String.format("%.0f", completion));
        textViewDuration.setText(String.valueOf(viewModel.getFlyingTime()));
        textViewBambis.setText(viewModel.getBambis().getValue().size());
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        initCodecManager(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (codecManager != null) {
            codecManager.cleanSurface();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buttonStart) {
            buttonStartPressed();
        } else if (view.getId() == R.id.buttonStop) {
            buttonStopPressed();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (view.getId() == R.id.buttonStop)
                if (buttonStop.isEnabled())
                    buttonStop.setBackgroundResource(R.drawable.stop_active);
                else if (bambiGuardMission.getState().equals(WaypointMissionState.EXECUTING))
                    buttonStart.setBackgroundResource(R.drawable.start_active);
                else
                    buttonStart.setBackgroundResource(R.drawable.pause_active);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (view.getId() == R.id.buttonStop)
                toggleButtonStop(buttonStop.isEnabled());
            else if (bambiGuardMission.getState().equals(WaypointMissionState.EXECUTING))
                buttonStart.setBackgroundResource(R.drawable.start);
            else
                buttonStart.setBackgroundResource(R.drawable.pause);
        }
        return false;
    }

    private void buttonStartPressed() {
        WaypointMissionState state = bambiGuardMission.getState();
        if (WaypointMissionState.EXECUTING.equals(state)) {
            bambiGuardMission.pauseWaypointMission();
            buttonStart.setBackgroundResource(R.drawable.start);
            toggleButtonStop(true);
        } else if (WaypointMissionState.EXECUTION_PAUSED.equals(state)) {
            bambiGuardMission.resumeWaypointMission();
            buttonStart.setBackgroundResource(R.drawable.pause);
            toggleButtonStop(false);
        } else {
            bambiGuardMission.startWaypointMission();
            buttonStart.setBackgroundResource(R.drawable.pause);
            toggleButtonStop(true);
        }
    }

    private void toggleButtonStop(boolean enabled) {
        buttonStop.setEnabled(enabled);
        if (enabled)
            buttonStop.setBackgroundResource(R.drawable.stop);
        else
            buttonStop.setBackgroundResource(R.drawable.stop_disabled);
    }

    private void buttonStopPressed() {
        WaypointMissionState state = bambiGuardMission.getState();
        if (WaypointMissionState.EXECUTION_PAUSED.equals(state)) {
            bambiGuardMission.stopWaypointMission();
            cancelFlight();
        }
    }

    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initLocationEngine();
            initLocationLayer();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initLocationEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void initLocationLayer() {
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.NONE);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }

    private void setCameraPosition(Location location) {
        if (map != null)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                    location.getLongitude()), MAP_ZOOM_FACTOR));
    }

    private void addBambiMarker(LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().position(position);
        Icon icon = IconFactory.getInstance(this).fromResource(R.drawable.bambi_guard_logo);
        Marker marker = map.addMarker(markerOptions.icon(icon));
        bambiMarkers.add(marker);
    }

    private void drawPolygon() {
        List<LatLng> positions = new ArrayList<>(viewModel.getWaypoints().getValue());

        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(positions)
                .fillColor(Color.parseColor("#29b17b"))
                .alpha(0.3f);

        map.addPolygon(polygonOptions);
    }

    private void setLanguage() {
        for (Layer layer : map.getLayers())
            layer.setProperties(PropertyFactory.textField("{name_de}"));
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        setLanguage();
        enableLocation();
        drawPolygon();
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (originLocation == null)
                setCameraPosition(location);
            originLocation = location;
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Please grant the permissions", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        if (locationEngine != null)
            locationEngine.requestLocationUpdates();
        if (locationLayerPlugin != null)
            locationLayerPlugin.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationEngine != null)
            locationEngine.removeLocationUpdates();
        if (locationLayerPlugin != null)
            locationLayerPlugin.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        initTextureView();
        notifyStatusChange();
    }

    @Override
    public void onPause() {
        if (camera != null) {
            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(videoDataListener);
            }
        }

        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager.destroyCodec();
        }

        if (receiverIsRegistered)
            unregisterReceiver(receiver);
        receiverIsRegistered = false;
        super.onDestroy();
        if (bambiGuardMission != null)
            bambiGuardMission.removeMissionOperatorListener();
        if (locationEngine != null)
            locationEngine.deactivate();

        if (codecManagerHandler != null)
            codecManagerHandler.getLooper().quit();
    }

    private void takeBitmapTextureViewData() {
        Bitmap bitmap = textureView.getBitmap();
        if (bitmap == null)
            return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("kek", "color space: " + bitmap.getColorSpace().getName());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d("kek", "bitmap: " + bitmap.getPixel(0, 0));
        }
        Log.d("kek", "byte array: " + byteArray[0]);

        detectBambis(byteArray, stream.size(), bitmap.getWidth(), bitmap.getHeight());

        bitmap.recycle();
    }

    private void detectBambis(byte[] frame, int dataSize, int width, int height) throws IllegalStateException {
        BambiGuardDetector bambiGuardDetector = new BambiGuardDetector();
        String bambiString = bambiGuardDetector.detectBambisInImage(width, height, frame);
        int[][] bambis = new int[1][2];

        // Test Bambis
        bambis[0][0] = 2;
        bambis[0][1] = 2;

        for (int[] bambi : bambis) {
            runOnUiThread(() -> bambiFound(bambi));
        }

        Log.d("kek", "retrun value from JNI: " + bambiString);
    }

    private void bambiFound(int[] bambiPositionRelativeToCameraInMeters) {
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();

        if (aircraft == null || map == null)
            return;

        LocationCoordinate3D aircraftLocation = aircraft.getFlightController().getState().getAircraftLocation();
        LatLng bambiOffset = map.getProjection().getLatLngForProjectedMeters(
                new ProjectedMeters(bambiPositionRelativeToCameraInMeters[0],
                        bambiPositionRelativeToCameraInMeters[1]));

        LatLng location = new LatLng(bambiOffset.getLatitude() + aircraftLocation.getLatitude(),
                bambiOffset.getLongitude() + aircraftLocation.getLongitude());
        addBambiMarker(location);
        Bambi bambi = new Bambi(location);
        viewModel.addBambi(bambi);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            sendBroadcast(attachedIntent);
        }
    }
}
