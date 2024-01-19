package at.ac.szybbs.bambiguard.fragments;

import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.model.BambiGuardCoveragePlanner;
import at.ac.szybbs.bambiguard.model.Point;
import at.ac.szybbs.bambiguard.viewmodel.PilotViewModel;

public class FlightPlanning1Fragment extends Fragment implements OnMapReadyCallback, LocationEngineListener, PermissionsListener, View.OnClickListener, MapboxMap.OnMapClickListener, View.OnTouchListener {

    private final double ZOOM_FACTOR = 17D;
    private PilotViewModel viewModel;

    private Button buttonAddMarkers;
    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;
    private ArrayList<Marker> markers;
    private ArrayList<Marker> plusMarkers;
    private Polygon polygon;
    private ArrayList<Polyline> polylines;
    private ArrayList<Point> coveragePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flight_planning1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Mapbox.getInstance(getContext(), getString(R.string.mapbox_access_token));
        viewModel = PilotViewModel.getInstance();
        initUI(view);
        initMapView(view, savedInstanceState);
    }

    private void initMapView(View view, Bundle savedInstanceState) {
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        mapView.setOnTouchListener(this);

        markers = new ArrayList<>();
        plusMarkers = new ArrayList<>();

        viewModel.setFlightAreaMarked(false);
    }

    private void initUI(View view) {
        buttonAddMarkers = view.findViewById(R.id.buttonAddMarker);
        buttonAddMarkers.setOnClickListener(this);
    }

    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(getContext())) {
            initLocationEngine();
            initLocationLayer();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initLocationEngine() {
        locationEngine = new LocationEngineProvider(getContext()).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
            buttonAddMarkers.setEnabled(true);
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
                    location.getLongitude()), ZOOM_FACTOR));
    }

    private void setOldCameraPosition() {
        if (originLocation != null)
            setCameraPosition(originLocation);
    }

    private void setOldMarkers() {
        ArrayList<LatLng> positions = viewModel.getWaypoints().getValue();
        if (positions.isEmpty())
            return;

        for (LatLng position : positions) {
            addMarker(position);
        }

        resetPlusMarkers();
        redraw();
        buttonAddMarkers.setText(R.string.remove_marker);
        viewModel.setFlightAreaMarked(true);
    }

    private void addInitialMarkers(LatLng point) {
        LatLng distances = map.getProjection().getLatLngForProjectedMeters(new ProjectedMeters(20, 20));
        double distLong = distances.getLongitude(),
                distLat = distances.getLatitude(),
                pointLong = point.getLongitude(),
                pointLat = point.getLatitude();

        LatLng pointTopLeft = new LatLng(pointLat - distLat, pointLong - distLong);
        LatLng pointTopRight = new LatLng(pointLat + distLat, pointLong - distLong);
        LatLng pointBottomLeft = new LatLng(pointLat - distLat, pointLong + distLong);
        LatLng pointBottomRight = new LatLng(pointLat + distLat, pointLong + distLong);

        addMarker(pointTopLeft);
        addMarker(pointTopRight);
        addMarker(pointBottomRight);
        addMarker(pointBottomLeft);

        addPlusMarker(pointTopLeft, pointTopRight);
        addPlusMarker(pointTopRight, pointBottomRight);
        addPlusMarker(pointBottomRight, pointBottomLeft);
        addPlusMarker(pointBottomLeft, pointTopLeft);
    }

    private void addPlusMarker(LatLng point1, LatLng point2) {
        double distLong = point1.getLongitude() - point2.getLongitude();
        double distLat = point1.getLatitude() - point2.getLatitude();
        LatLng position = new LatLng(point2.getLatitude() + distLat / 2, point2.getLongitude() + distLong / 2);

        Icon icon = IconFactory.getInstance(getContext()).fromResource(R.drawable.plus_100x100);
        MarkerOptions markerOptions = new MarkerOptions().position(position).icon(icon);

        Marker marker = map.addMarker(markerOptions);
        plusMarkers.add(marker);
    }

    private void addMarker(LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().position(position);
        Icon icon = IconFactory.getInstance(getContext()).fromResource(R.drawable.marker_100x100);
        Marker marker = map.addMarker(markerOptions.icon(icon));
        markers.add(marker);
    }

    private void drawPolygon() {
        List<LatLng> positions = new ArrayList<>();
        for (Marker marker : markers) {
            positions.add(marker.getPosition());
        }

        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(positions)
                .fillColor(Color.parseColor("#29b17b"))
                .alpha(0.5f);

        polygon = map.addPolygon(polygonOptions);
    }

    private void drawCoveragePath() {
        for (int i = 0; i < coveragePath.size() - 1; i++) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .width(2.8f)
                    .color(Color.parseColor("#22BDB8"))
                    .alpha(0.8f);
            polylineOptions.add(coveragePath.get(i).toLatLng(), coveragePath.get(i + 1).toLatLng());
            polylines.add(map.addPolyline(polylineOptions));
        }
    }

    private void calculateCoveragePath() {
        ArrayList<Point> points = new ArrayList<>();
        for (LatLng latLng : polygon.getPoints())
            points.add(new Point(latLng.getLatitude(), latLng.getLongitude()));

        double cameraCoverage = map.getProjection().getLatLngForProjectedMeters(new ProjectedMeters(5, 5)).getLatitude();

        coveragePath = BambiGuardCoveragePlanner.decomposePolygon(points, cameraCoverage);
    }

    private void insertMarker(int plusMarkerIndex, LatLng position) {
        ArrayList<Marker> oldMarkers = (ArrayList<Marker>) markers.clone();
        clearMarkers();
        clearPlusMarkers();

        for (int i = 0; i < oldMarkers.size(); i++) {
            addMarker(oldMarkers.get(i).getPosition());
            if (i == plusMarkerIndex)
                addMarker(position);
        }

        resetPlusMarkers();
        redraw();
    }

    private void resetPlusMarkers() {
        clearPlusMarkers();
        for (int i = 1; i < markers.size(); i++)
            addPlusMarker(markers.get(i - 1).getPosition(), markers.get(i).getPosition());
        addPlusMarker(markers.get(markers.size() - 1).getPosition(), markers.get(0).getPosition());
    }

    private void dragMarker(int markerIndex, LatLng newPosition) {
        markers.get(markerIndex).setPosition(newPosition);
        resetPlusMarkers();
        List<LatLng> points = polygon.getPoints();
        points.get(markerIndex).setLongitude(newPosition.getLongitude());
        points.get(markerIndex).setLatitude(newPosition.getLatitude());
        polygon.setPoints(points);
    }

    private boolean dragMarkers(MotionEvent event) {
        for (int i = 0; i < markers.size(); i++) {
            PointF touchLocation = new PointF(event.getX(), event.getY());
            PointF markerScreenLocation = map.getProjection().toScreenLocation(markers.get(i).getPosition());
            if (getDistance(touchLocation, markerScreenLocation) < 40) {
                dragMarker(i, map.getProjection().fromScreenLocation(touchLocation));
                return true;
            }
        }
        return false;
    }

    private void addMarkerBetween(LatLng touchPosition) {
        for (int i = 0; i < plusMarkers.size(); i++) {
            PointF touchLocation = map.getProjection().toScreenLocation(touchPosition);
            PointF markerScreenLocation = map.getProjection().toScreenLocation(plusMarkers.get(i).getPosition());
            if (getDistance(touchLocation, markerScreenLocation) < 40) {
                insertMarker(i, plusMarkers.get(i).getPosition());
                return;
            }
        }
    }

    private double getDistance(PointF point1, PointF point2) {
        return Math.hypot(point1.x - point2.x, point1.y - point2.y);
    }

    private void clearMarkers() {
        for (Marker marker : markers)
            map.removeMarker(marker);
        markers.clear();
    }

    private void clearPlusMarkers() {
        for (Marker plusMarker : plusMarkers)
            map.removeMarker(plusMarker);
        plusMarkers.clear();
    }

    private void clearPolygon() {
        if (polygon != null)
            map.removePolygon(polygon);
        polygon = null;
    }

    private void clearCoveragePath() {
        if (polylines == null) {
            polylines = new ArrayList<>();
            return;
        }

        for (Polyline polyline : polylines)
            map.removePolyline(polyline);
        polylines.clear();
    }

    public void saveMarkers() {
        ArrayList<LatLng> waypoints = new ArrayList<>();
        for (Point point : coveragePath)
            waypoints.add(point.toLatLng());
        viewModel.setWaypoints(waypoints);
    }

    private void setLanguage() {
        for (Layer layer : map.getLayers())
            layer.setProperties(PropertyFactory.textField("{name_de}"));
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.addOnMapClickListener(this);
        setLanguage();
        enableLocation();

        setOldCameraPosition();
        setOldMarkers();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (dragMarkers(event)) {
            if (event.getAction() == MotionEvent.ACTION_UP)
                redraw();
            view.performClick();
            return true;
        }
        return false;
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        addMarkerBetween(point);
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
            buttonAddMarkers.setEnabled(true);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getContext(), "Please grant the permissions", Toast.LENGTH_LONG).show();
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

    private void removeAllMarkers() {
        clearMarkers();
        clearPlusMarkers();
        clearPolygon();
        buttonAddMarkers.setText(R.string.add_marker);
        viewModel.setFlightAreaMarked(false);
    }

    private void addAllMarkers() {
        if (map == null || originLocation == null)
            return;
        addInitialMarkers(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()));
        redraw();
        buttonAddMarkers.setText(R.string.remove_marker);
        viewModel.setFlightAreaMarked(true);
    }

    private void redraw() {
        clearPolygon();
        drawPolygon();
        clearCoveragePath();

        calculateCoveragePath();
        //drawCoveragePath();
    }

    @Override
    public void onClick(View view) {
        if (markers.isEmpty()) {
            addAllMarkers();
        } else {
            removeAllMarkers();
        }
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
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
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
    public void onDestroy() {
        super.onDestroy();
        if (locationEngine != null)
            locationEngine.deactivate();
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
}