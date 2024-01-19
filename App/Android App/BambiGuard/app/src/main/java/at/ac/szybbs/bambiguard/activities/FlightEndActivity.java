package at.ac.szybbs.bambiguard.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.components.ListViewAdapter;
import at.ac.szybbs.bambiguard.model.Bambi;
import at.ac.szybbs.bambiguard.model.Helper;
import at.ac.szybbs.bambiguard.viewmodel.PilotViewModel;

public class FlightEndActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private final double ZOOM_FACTOR = 16D;

    private PilotViewModel viewModel;

    private MapView mapView;
    private MapboxMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_end);

        viewModel = PilotViewModel.getInstance();
        initUI();
        initMapView(savedInstanceState);
        setFlightOverview();
        setHelpers();
    }

    private void initUI() {
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.flight_overview);
        }

        Button buttonFinish = findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(this);
    }

    private void setFlightOverview() {
        int rescuedBambis = 0;
        for (Bambi bambi : viewModel.getBambis().getValue()) {
            if (bambi.isRescued())
                rescuedBambis++;
        }

        String helperNumber = String.valueOf(viewModel.getHelpers().getValue().size());
        String detectedBambis = String.valueOf(viewModel.getBambis().getValue().size());

        ((TextView) findViewById(R.id.textViewHelperNumber)).setText(helperNumber);
        ((TextView) findViewById(R.id.textViewDetectedBambis)).setText(detectedBambis);
        ((TextView) findViewById(R.id.textViewRescuedBambis)).setText(String.valueOf(rescuedBambis));
        ((TextView) findViewById(R.id.textViewFlightDuration)).setText(String.valueOf(viewModel.getFlyingTime()));
        ((TextView) findViewById(R.id.textViewSearchStatus)).setText(R.string.successful);
        ((TextView) findViewById(R.id.textViewFieldSize)).setText(R.string.unknown);
    }

    private void setHelpers() {
        ArrayList<String> helperNames = new ArrayList<>();

        ListViewAdapter listViewAdapter = new ListViewAdapter(this, R.layout.list_view_row);
        ListView listViewAvailableHelpers = findViewById(R.id.listViewHelpers);
        listViewAvailableHelpers.setAdapter(listViewAdapter);

        for (Helper helper : viewModel.getHelpers().getValue())
            helperNames.add("+ " + helper.getName());

        listViewAdapter.addAll(helperNames);
    }

    private void initMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        Log.d("kek", "getMapAsync");
    }

    private LatLng calculateCenter(ArrayList<LatLng> positions) {
        double longitude = 0, latitude = 0;

        for (LatLng position : positions) {
            longitude += position.getLongitude();
            latitude += position.getLatitude();
        }

        return new LatLng(longitude / positions.size(), latitude / positions.size());
    }

    private void setCameraPositionToCenter() {
        if (map == null)
            return;

        ArrayList<LatLng> positions = new ArrayList<>();
        for (Bambi bambi : viewModel.getBambis().getValue())
            positions.add(bambi.getPosition());

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(calculateCenter(positions), ZOOM_FACTOR));
    }

    private void addBambiMarkers() {
        for (Bambi bambi : viewModel.getBambis().getValue()) {
            addBambiMarker(bambi.getPosition());
        }
    }

    private void addBambiMarker(LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().position(position);
        Icon icon = IconFactory.getInstance(this).fromResource(R.drawable.marker_100x100);
        map.addMarker(markerOptions.icon(icon));
    }

    private void drawFieldArea() {
        List<LatLng> positions = viewModel.getWaypoints().getValue();

        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(positions)
                .fillColor(Color.parseColor("#29b17b"))
                .alpha(0.5f);

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
        addBambiMarkers();
        drawFieldArea();
        setCameraPositionToCenter();
        Log.d("kek", "onMapReady");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonFinish) {
            changeActivity(LandingScreenActivity.class);
        }
    }

    public void changeActivity(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
    }
}