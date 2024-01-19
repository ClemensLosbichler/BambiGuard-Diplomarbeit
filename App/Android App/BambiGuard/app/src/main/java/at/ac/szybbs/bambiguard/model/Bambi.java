package at.ac.szybbs.bambiguard.model;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class Bambi {
    private static int ID = 0;
    private LatLng position;
    private boolean rescued;
    private int id;

    public Bambi(LatLng position) {
        this.position = position;
        rescued = false;
        this.id = ID++;
    }

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public boolean isRescued() {
        return rescued;
    }

    public void setRescued(boolean rescued) {
        this.rescued = rescued;
    }

    public int getId() {
        return id;
    }
}
