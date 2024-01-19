package at.ac.szybbs.bambiguard.viewmodel;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import at.ac.szybbs.bambiguard.model.Bambi;
import at.ac.szybbs.bambiguard.model.Helper;
import at.ac.szybbs.bambiguard.model.PilotSocket;
import dji.common.mission.MissionState;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;

public class PilotViewModel extends ViewModel {

    private static PilotViewModel instance;
    private final MutableLiveData<Boolean> droneConnected = new MutableLiveData<>();
    private final MutableLiveData<ConnectDroneDialog> connectDroneDialogVisible = new MutableLiveData<>();
    private final MutableLiveData<WaypointMissionFinishedAction> waypointMissionFinishedAction = new MutableLiveData<>();

    private final MutableLiveData<String> pilotName = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<LatLng>> waypoints = new MutableLiveData<>();
    private final MutableLiveData<Boolean> flightAreaMarked = new MutableLiveData<>();

    private final MutableLiveData<ArrayList<Helper>> availableHelpers = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<Helper>> helpers = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<Bambi>> bambis = new MutableLiveData<>();
    private final PilotSocket socket;
    private float flightDuration = 0;
    private float flightDistance = 0;
    private float flyingTime = 0;
    private MissionState missionState;

    public PilotViewModel() {
        droneConnected.setValue(false);
        connectDroneDialogVisible.setValue(ConnectDroneDialog.Unknown);
        waypointMissionFinishedAction.setValue(WaypointMissionFinishedAction.GO_HOME);

        pilotName.setValue("");
        waypoints.setValue(new ArrayList<>());
        flightAreaMarked.setValue(false);

        helpers.setValue(new ArrayList<>());
        availableHelpers.setValue(new ArrayList<>());
        bambis.setValue(new ArrayList<>());

        socket = new PilotSocket();
    }

    public static PilotViewModel getInstance() {
        if (instance == null)
            instance = new PilotViewModel();
        return instance;
    }

    public MutableLiveData<ArrayList<LatLng>> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(ArrayList<LatLng> waypoints) {
        this.waypoints.setValue(waypoints);
    }

    public MutableLiveData<Boolean> getFlightAreaMarked() {
        return flightAreaMarked;
    }

    public void setFlightAreaMarked(boolean flightAreaMarked) {
        this.flightAreaMarked.setValue(flightAreaMarked);
    }

    public MutableLiveData<String> getPilotName() {
        return pilotName;
    }

    public void setPilotName(String pilotName) {
        this.pilotName.setValue(pilotName);
    }

    public LiveData<Boolean> getDroneConnected() {
        return droneConnected;
    }

    public void setDroneConnected(boolean droneConnected) {
        this.droneConnected.setValue(droneConnected);
    }

    public LiveData<ConnectDroneDialog> getConnectDroneDialogVisible() {
        return connectDroneDialogVisible;
    }

    public void setConnectDroneDialogVisible(ConnectDroneDialog connectDroneDialogVisible) {
        this.connectDroneDialogVisible.setValue(connectDroneDialogVisible);
    }

    public LiveData<WaypointMissionFinishedAction> getWaypointMissionFinishedAction() {
        return waypointMissionFinishedAction;
    }

    public void setWaypointMissionFinishedAction(WaypointMissionFinishedAction waypointMissionFinishedAction) {
        this.waypointMissionFinishedAction.setValue(waypointMissionFinishedAction);
    }

    public MutableLiveData<ArrayList<Bambi>> getBambis() {
        return bambis;
    }

    public void addBambi(Bambi bambi) {
        bambis.getValue().add(bambi);
        socket.emitBambiFound(bambi);
    }

    public PilotSocket getSocket() {
        return socket;
    }

    public MutableLiveData<ArrayList<Helper>> getHelpers() {
        return helpers;
    }

    public MutableLiveData<ArrayList<Helper>> getAvailableHelpersHelpers() {
        return availableHelpers;
    }

    public float getFlightDuration() {
        return flightDuration;
    }

    public void setFlightDuration(int flightDuration) {
        this.flightDuration = flightDuration;
    }

    public float getFlyingTime() {
        return flyingTime;
    }

    public void setFlyingTime(float flyingTime) {
        this.flyingTime = flyingTime;
    }

    public float getFlightDistance() {
        return flightDistance;
    }

    public void setFlightDistance(float flightDistance) {
        this.flightDistance = flightDistance;
    }

    public MissionState getMissionState() {
        return missionState;
    }

    public void setMissionState(MissionState missionState) {
        this.missionState = missionState;
    }

    public enum ConnectDroneDialog {
        Unknown(0), Continue(1), Dismiss(2);
        private final int value;

        ConnectDroneDialog(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
