package at.ac.szybbs.bambiguard.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.LatLng;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.viewmodel.HelperViewModel;
import io.socket.emitter.Emitter;

public class HelperFlightActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, LocationListener {

    private static final float BAMBI_IN_REACH_DISTANCE = 3.5f;
    private static final int REQUEST_PERMISSION_CODE = 13;
    private final Emitter.Listener onFlightUpdate = args -> runOnUiThread(() -> updateFlightStatus((String) args[0], (int) args[1]));
    private final float[] rotationBuffer = new float[35];
    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];
    private ImageView imageViewCompass;
    private SensorManager sensorManager;
    private HelperViewModel viewModel;
    private LatLng bambiLocation;
    private LatLng myLocation;
    private boolean alertDialogShowing = false;
    private int bambiId;
    private final Emitter.Listener onBambiFound = args -> runOnUiThread(() -> searchForBambi((int) args[0], (double) args[1], (double) args[2]));
    private TextView textViewDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_flight);
        setViewModel();
        initUI();
        setSensorManager();
        setLocationManager();
        setTestBambiLocation();
        updateFlightStatus(getString(R.string.wait), 0);
    }

    private void setTestBambiLocation() {
        bambiLocation = new LatLng(48.136841589073526, 15.134916679066707);
    }

    private void searchForBambi(int id, double longitude, double latitude) {
        this.bambiId = id;
        bambiLocation = new LatLng(latitude, longitude);
    }

    @SuppressLint("SetTextI18n")
    private void updateFlightStatus(String status, int remainingMinutes) {
        if (status == null || status.isEmpty())
            status = getString(R.string.unknown);

        ((TextView) findViewById(R.id.textViewStatus)).setText(status);
        ((TextView) findViewById(R.id.textViewRemainingFlightDuration)).setText(remainingMinutes + " min");
    }

    private void setSensorManager() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @SuppressLint("MissingPermission")
    private void setLocationManager() {
        if (!checkPermissions())
            return;
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300, 0.001f, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300, 0.001f, this);
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    private void setViewModel() {
        viewModel = HelperViewModel.getInstance();
        viewModel.getSocket().addEventListener(onFlightUpdate, "flight_update");
        viewModel.getSocket().addEventListener(onBambiFound, "bambi_found");
    }

    private void initUI() {
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.in_flight_of) + viewModel.getPilotName());

        imageViewCompass = findViewById(R.id.imageViewCompass);
        textViewDistance = findViewById(R.id.textViewDistance);

        Button buttonLeaveFlight = findViewById(R.id.buttonLeaveFlight);
        buttonLeaveFlight.setOnClickListener(this);

        ((Button) findViewById(R.id.buttonRescueBambi)).setOnClickListener(this);
    }

    private void toggleCompass(float rotation) {
        imageViewCompass.setRotation(rotation);
    }

    private boolean updateOrientationAngles() {
        float[] rotationMatrix = new float[9];
        float[] orientationAngles = new float[3];

        if (!SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading))
            return false;

        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        float northRotation = (float) -Math.toDegrees(orientationAngles[0]);
        float rotationToBambi = calculateAngleToBambi(northRotation);

        updateRotationBuffer(rotationToBambi);
        toggleCompass(getAverageRotation());

        return true;
    }

    @SuppressLint("DefaultLocale")
    private void updateBambiSearch() {
        if (!updateOrientationAngles())
            return;

        textViewDistance.setText(String.format("%.1f m", getDistanceToBambi()));
        Log.d("kek", "distance " + getDistanceToBambi());


        if (isBambiInReach())
            enableRescueButton();
        else
            disableRescueButton();
    }

    private boolean isBambiInReach() {
        return getDistanceToBambi() < BAMBI_IN_REACH_DISTANCE;
    }

    private double getDistanceToBambi() {
        return myLocation.distanceTo(bambiLocation);
    }

    private float calculateAngleToBambi(float northRotation) {
        if (bambiLocation == null || myLocation == null)
            return 0;

        double longitudeDifference = bambiLocation.getLongitude() - myLocation.getLongitude();
        double latitudeDifference = bambiLocation.getLatitude() - myLocation.getLatitude();
        float rotationToBambi = 180 - (float) Math.toDegrees(Math.tan(latitudeDifference / longitudeDifference));

        return northRotation - rotationToBambi;
    }

    private void enableRescueButton() {
        ((TextView) findViewById(R.id.textViewDistance)).setVisibility(View.INVISIBLE);
        ((Button) findViewById(R.id.buttonRescueBambi)).setVisibility(View.VISIBLE);
    }

    private void disableRescueButton() {
        ((TextView) findViewById(R.id.textViewDistance)).setVisibility(View.VISIBLE);
        ((Button) findViewById(R.id.buttonRescueBambi)).setVisibility(View.INVISIBLE);
    }

    private void rescueBambi() {
        viewModel.getSocket().emitBambiRescued(bambiId);
        bambiLocation = null;
        textViewDistance.setText(R.string.no_bambi_found_yet);
        disableRescueButton();
    }

    private void updateRotationBuffer(float rotation) {
        System.arraycopy(rotationBuffer, 0, rotationBuffer, 1, rotationBuffer.length - 1);
        rotationBuffer[0] = rotation;
    }

    private float getAverageRotation() {
        float sum = 0;
        for (float r : rotationBuffer) {
            sum += r;
        }
        return sum / rotationBuffer.length;
    }

    private void showEnableGPSDialog() {
        if (alertDialogShowing)
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(R.string.enable_gps);
        alertDialogBuilder
                .setMessage(R.string.please_enable_gps_to_allow_navigation)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialog, id) -> setLocationManager());

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialogShowing = true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerReading = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerReading = event.values.clone();
        }

        if (accelerometerReading != null && magnetometerReading != null && bambiLocation != null && myLocation != null)
            updateBambiSearch();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        myLocation = new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        showEnableGPSDialog();
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        setLocationManager();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            setLocationManager();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonLeaveFlight) {
            changeActivity(LandingScreenActivity.class);
        } else if (v.getId() == R.id.buttonRescueBambi) {
            rescueBambi();
        }
    }

    public void changeActivity(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.getSocket().onDestroy();
    }
}