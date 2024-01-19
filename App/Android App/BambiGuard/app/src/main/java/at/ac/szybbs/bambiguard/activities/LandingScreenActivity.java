package at.ac.szybbs.bambiguard.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.net.URISyntaxException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.model.BambiGuardDetector;
import io.socket.client.IO;
import io.socket.client.Socket;

public class LandingScreenActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_screen);
        initUI();
    }

    private void initUI() {
        setSupportActionBar(findViewById(R.id.toolbar));

        Button buttonPilot = findViewById(R.id.buttonPilot);
        buttonPilot.setOnClickListener(this);

        Button buttonHelper = findViewById(R.id.buttonHelper);
        buttonHelper.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonPilot:
                changeActivity(FlightPlanningActivity.class);
                break;
            case R.id.buttonHelper:
                changeActivity(JoinFlightActivity.class);
                break;
        }
    }

    public void changeActivity(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
    }
}