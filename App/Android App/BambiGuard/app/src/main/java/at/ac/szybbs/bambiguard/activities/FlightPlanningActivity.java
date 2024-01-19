package at.ac.szybbs.bambiguard.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.dji.DJIApplication;
import at.ac.szybbs.bambiguard.fragments.FlightPlanning1Fragment;
import at.ac.szybbs.bambiguard.fragments.FlightPlanning2Fragment;
import at.ac.szybbs.bambiguard.model.Helper;
import at.ac.szybbs.bambiguard.viewmodel.PilotViewModel;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class FlightPlanningActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private static final int REQUEST_PERMISSION_CODE;

    static {
        REQUEST_PERMISSION_CODE = 12345;
    }

    private final List<String> missingPermission = new ArrayList<>();
    private FragmentManager fragmentManager;
    private FlightPlanning1Fragment flightPlanning1Fragment;
    private FlightPlanning2Fragment flightPlanning2Fragment;
    private PilotViewModel viewModel;
    private final Emitter.Listener onReceiveHelpers = (Object... args) -> runOnUiThread(() -> populateHelpers(args));
    private final Emitter.Listener onSocketConnect = (Object... args) -> runOnUiThread(this::sendUserIdentification);
    private boolean alertDialogShowing = false;
    private final Emitter.Listener onSocketConnectError = (Object... args) -> runOnUiThread(this::showNoInternetConnectionDialog);
    private Button buttonStart;
    private final Observer<String> pilotNameObserver = new Observer<String>() {
        @Override
        public void onChanged(String pilotName) {
            toggleButtonStart();
            sendPilotNameChanged();
        }
    };
    private final Observer<Boolean> flightAreaMarkedObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean b) {
            toggleButtonStart();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_planning);

        setViewModel();
        initUI();
        registerApplication();
    }

    private void requestHelpers() {
        if (!viewModel.getSocket().identificationSent())
            return;

        viewModel.getSocket().addEventListener(onReceiveHelpers, "helper");
        viewModel.getSocket().emitGetHelpers();
    }

    private void requestHelpersRecurring() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            requestHelpers();
            requestHelpersRecurring();
        }, 2000);
    }

    private void sendUserIdentification() {
        if (viewModel.getSocket().identificationSent())
            return;

        viewModel.getSocket().emitUserIdentification(viewModel.getPilotName().getValue());
        requestHelpersRecurring();
    }

    private void sendPilotNameChanged() {
        viewModel.getSocket().emitPilotNameChanged(viewModel.getPilotName().getValue());
    }

    private void populateHelpers(Object... args) {
        ArrayList<Helper> helperList = viewModel.getAvailableHelpersHelpers().getValue();

        try {
            String name = (String) args[0];
            int id = (int) args[1];

            helperList.add(new Helper(name, id));
            viewModel.getAvailableHelpersHelpers().setValue(helperList);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    private void showNoInternetConnectionDialog() {
        if (alertDialogShowing)
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(R.string.error_when_connecting_with_server);
        alertDialogBuilder
                .setMessage(R.string.please_turn_on_wifi_to_connect_to_the_server)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    viewModel.getSocket().reconnect();
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialogShowing = true;
    }

    private void setViewModel() {
        viewModel = PilotViewModel.getInstance();
        viewModel.setDroneConnected(false);
        viewModel.getSocket().addEventListener(onSocketConnect, Socket.EVENT_CONNECT);
        viewModel.getSocket().addEventListener(onSocketConnectError, Socket.EVENT_CONNECT_ERROR);
        viewModel.getSocket().addEventListener(onSocketConnectError, Socket.EVENT_DISCONNECT);
    }

    private void registerApplication() {
        (new Thread() {
            public void run() {
                if (!DJIApplication.getInstance().registerApplication()) {
                    checkAndRequestPermissions();
                }
            }
        }).start();
    }

    private void checkAndRequestPermissions() {
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }

        if (!missingPermission.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[0]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }

        if (missingPermission.isEmpty()) {
            registerApplication();
        } else {
            showToast("Missing permissions!!!");
        }
    }

    private void initUI() {
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.flight_planning);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(this);
        buttonStart.setEnabled(false);

        flightPlanning1Fragment = new FlightPlanning1Fragment();
        flightPlanning2Fragment = new FlightPlanning2Fragment();

        fragmentManager = getSupportFragmentManager();
        switchToFlightPlanning1();
    }

    private void toggleButtonStart() {
        if (isFlightPlanning1Fragment()) {
            buttonStart.setEnabled(viewModel.getFlightAreaMarked().getValue());
        } else {
            buttonStart.setEnabled(!viewModel.getPilotName().getValue().isEmpty());
        }
    }

    private void switchToFlightPlanning2() {
        flightPlanning1Fragment.saveMarkers();
        replaceFragment(flightPlanning2Fragment);
        buttonStart.setText(R.string.start);
        buttonStart.setEnabled(false);
        if (viewModel.getPilotName().hasObservers())
            viewModel.getFlightAreaMarked().removeObserver(flightAreaMarkedObserver);
        viewModel.getPilotName().observe(this, pilotNameObserver);
        toggleButtonStart();
    }

    private void switchToFlightPlanning1() {
        if (viewModel.getPilotName().hasObservers())
            viewModel.getPilotName().removeObserver(pilotNameObserver);
        viewModel.getFlightAreaMarked().observe(this, flightAreaMarkedObserver);
        buttonStart.setText(R.string.next);
        replaceFragment(flightPlanning1Fragment);
        toggleButtonStart();
    }

    private void replaceFragment(Fragment fragment) {
        fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainerView, fragment, null)
                .commit();
    }

    private boolean backPressed() {
        if (isFlightPlanning1Fragment())
            return false;
        switchToFlightPlanning1();
        return true;
    }

    private void startFlight() {
        acquireHelpers();
        Intent intent = new Intent(this, PilotFlightActivity.class);
        startActivity(intent);
    }

    private void acquireHelpers() {
        for (Helper helper : viewModel.getHelpers().getValue()) {
            viewModel.getSocket().emitAcquireHelpers(helper.getId());
        }
    }

    private boolean isFlightPlanning1Fragment() {
        return fragmentManager.findFragmentById(R.id.fragmentContainerView) instanceof FlightPlanning1Fragment;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!backPressed())
                NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!backPressed())
            super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.getSocket().removeEventListener(onSocketConnectError);
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.getSocket().addEventListener(onSocketConnectError, Socket.EVENT_CONNECT_ERROR);
        viewModel.getSocket().addEventListener(onSocketConnectError, Socket.EVENT_DISCONNECT);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonStart) {
            if (isFlightPlanning1Fragment()) {
                switchToFlightPlanning2();
            } else {
                startFlight();
            }
        }
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show());
    }
}