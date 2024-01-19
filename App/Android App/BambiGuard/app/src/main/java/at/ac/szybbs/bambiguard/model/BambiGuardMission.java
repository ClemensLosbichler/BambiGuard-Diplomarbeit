package at.ac.szybbs.bambiguard.model;

import java.util.ArrayList;
import java.util.List;

import at.ac.szybbs.bambiguard.viewmodel.PilotViewModel;
import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;

public class BambiGuardMission {
    private static final float ALTITUDE = 11f;
    private static final float SPEED = 3f;
    public static WaypointMission.Builder missionBuilder;
    private final WaypointMissionFinishedAction finishedAction;
    private final WaypointMissionHeadingMode headingMode = WaypointMissionHeadingMode.AUTO;
    private final WaypointMissionOperatorListener eventNotificationListener;
    private final List<Waypoint> waypoints;
    private WaypointMissionOperator missionOperator;
    private float totalDistance;
    private float totalTime;

    private final PilotViewModel viewModel;

    public BambiGuardMission(WaypointMissionFinishedAction finishedAction, ArrayList<Waypoint> waypoints, WaypointMissionOperatorListener eventNotificationListener) {
        this.finishedAction = finishedAction;
        this.waypoints = waypoints;
        this.eventNotificationListener = eventNotificationListener;
        viewModel = PilotViewModel.getInstance();
    }

    private void addWaypoints() {
        for (Waypoint waypoint : waypoints) {
            missionBuilder.addWaypoint(waypoint);
        }

        if (missionBuilder.getWaypointList().size() > 0) {
            for (int i = 0; i < missionBuilder.getWaypointList().size(); i++) {
                missionBuilder.getWaypointList().get(i).altitude = ALTITUDE;
            }
        }
    }

    private WaypointMissionOperator getWaypointMissionOperator() {
        if (missionOperator == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null) {
                missionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return missionOperator;
    }

    public void configureWaypointMission() {
        if (missionBuilder == null) {
            missionBuilder = new WaypointMission.Builder().finishedAction(finishedAction)
                    .headingMode(headingMode)
                    .autoFlightSpeed(SPEED)
                    .maxFlightSpeed(SPEED)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        } else {
            missionBuilder.finishedAction(finishedAction)
                    .headingMode(headingMode)
                    .autoFlightSpeed(SPEED)
                    .maxFlightSpeed(SPEED)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }

        addWaypoints();

        totalDistance = missionBuilder.getLastCalculatedTotalDistance();
        totalTime = missionBuilder.getLastCalculatedTotalTime();

        DJIError error = getWaypointMissionOperator().loadMission(missionBuilder.build());
        if (error != null) {
            getWaypointMissionOperator().loadMission(missionBuilder.build());
        }
    }

    public void uploadWaypointMission() {
        getWaypointMissionOperator().uploadMission(error -> {
            if (error != null) {
                getWaypointMissionOperator().retryUploadMission(null);
            }
        });
    }

    public void startWaypointMission() {
        setState();
        getWaypointMissionOperator().startMission(djiError -> {
        });
    }

    public void stopWaypointMission() {
        setState();
        getWaypointMissionOperator().stopMission(djiError -> {
        });
    }

    public void pauseWaypointMission() {
        setState();
        getWaypointMissionOperator().pauseMission(djiError -> {
        });
    }

    public void resumeWaypointMission() {
        setState();
        getWaypointMissionOperator().resumeMission(djiError -> {
        });
    }

    public void addMissionOperatorListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    public void removeMissionOperatorListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private void setState() {
        viewModel.setMissionState(getState());
    }

    public WaypointMissionState getState() {
        return getWaypointMissionOperator().getCurrentState();
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public float getTotalTime() {
        return totalTime;
    }
}
