package at.ac.szybbs.bambiguard.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.fragments.helpertutorial.HelperTutorial1Fragment;
import at.ac.szybbs.bambiguard.fragments.helpertutorial.HelperTutorial2Fragment;
import at.ac.szybbs.bambiguard.fragments.helpertutorial.HelperTutorial3Fragment;
import at.ac.szybbs.bambiguard.fragments.helpertutorial.SliderAdapter;
import at.ac.szybbs.bambiguard.viewmodel.HelperViewModel;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class JoinFlightActivity extends AppCompatActivity implements View.OnClickListener {

    private Button buttonStart;
    private EditText editTextName;
    private TextView textViewWait;
    private LinearLayout linearLayoutName;
    private HelperViewModel viewModel;
    private boolean joinFlightPossible = false;
    private final Emitter.Listener onJoinFlight = args -> runOnUiThread(() -> showJoinFlightButton((String) Arrays.stream(args).toArray()[0]));
    private String name = null;
    private final Emitter.Listener onSocketConnect = args -> runOnUiThread(this::emitUserIdentification);
    private boolean alertDialogShowing = false;
    private final Emitter.Listener onSocketConnectError = args -> runOnUiThread(this::showNoInternetConnectionDialog);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_flight);
        setViewModel();
        initUI();
    }

    private void setViewModel() {
        (new Thread() {
            public void run() {
                HelperViewModel.initializeHelperViewModel();
                viewModel = HelperViewModel.getInstance();
                viewModel.getSocket().addEventListener(onSocketConnect, Socket.EVENT_CONNECT);
                viewModel.getSocket().addEventListener(onSocketConnectError, Socket.EVENT_CONNECT_ERROR);
                viewModel.getSocket().addEventListener(onSocketConnectError, Socket.EVENT_DISCONNECT);
                viewModel.getSocket().addEventListener(onJoinFlight, "join_flight");
            }
        }).start();
    }

    private void emitUserIdentification() {
        if (name != null && !name.isEmpty())
            viewModel.getSocket().emitUserIdentification(name);
    }

    private void joinFlight() {
        viewModel.getSocket().removeAllEventListeners();
        changeActivity(HelperFlightActivity.class);
    }

    private void showNoInternetConnectionDialog() {
        if (alertDialogShowing)
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("Fehler beim Verbinden mit dem Server");

        alertDialogBuilder
                .setMessage("Bitte schalten Sie WLAN oder Mobile Daten ein um eine Verbindung zum Server herzustellen.")
                .setCancelable(true)
                .setPositiveButton("OK", (dialog, id) -> viewModel.getSocket().reconnect());

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        alertDialogShowing = true;
    }

    private void showJoinFlightButton(String pilotName) {
        buttonStart.setText(String.format("%s %s %s", getString(R.string.flight_of_), pilotName, getString(R.string._join)));
        buttonStart.setVisibility(View.VISIBLE);
        textViewWait.setVisibility(View.INVISIBLE);
        joinFlightPossible = true;
    }

    private void showTextViewWait() {
        linearLayoutName.setVisibility(View.INVISIBLE);
        buttonStart.setVisibility(View.INVISIBLE);
        textViewWait.setVisibility(View.VISIBLE);
        name = editTextName.getText().toString();
        viewModel.getSocket().emitUserIdentification(name);

        showJoinFlightButton("Test");
    }

    private void initUI() {
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.join_flight);
        actionBar.setDisplayHomeAsUpEnabled(true);

        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(this);
        buttonStart.setEnabled(false);

        textViewWait = findViewById(R.id.textViewWait);

        linearLayoutName = findViewById(R.id.linearLayoutName);

        editTextName = findViewById(R.id.editTextName);
        editTextName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateEditTextName();
            }
        });

        initFragmentSlider();
    }

    private void initFragmentSlider() {
        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(new HelperTutorial1Fragment());
        fragments.add(new HelperTutorial2Fragment());
        fragments.add(new HelperTutorial3Fragment());

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        FragmentStateAdapter adapter = new SliderAdapter(fragments, this);
        viewPager.setAdapter(adapter);
    }

    private void validateEditTextName() {
        buttonStart.setEnabled(!editTextName.getText().toString().isEmpty());
    }

    @Override
    public void onClick(View v) {
        if (joinFlightPossible) {
            joinFlight();
        } else {
            showTextViewWait();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.getSocket().onDestroy();
    }

    public void changeActivity(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
    }
}