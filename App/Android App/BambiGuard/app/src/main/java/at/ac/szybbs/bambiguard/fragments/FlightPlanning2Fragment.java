package at.ac.szybbs.bambiguard.fragments;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.components.ListViewAdapter;
import at.ac.szybbs.bambiguard.model.Helper;
import at.ac.szybbs.bambiguard.viewmodel.PilotViewModel;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;

public class FlightPlanning2Fragment extends Fragment {

    private PilotViewModel viewModel;

    private ListViewAdapter listViewHelpersAdapter;
    private ListViewAdapter listViewAvailableHelpersAdapter;

    private EditText editTextName;

    public FlightPlanning2Fragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flight_planning2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = PilotViewModel.getInstance();
        initUI(view);
        setPilotName();
        viewModel.getAvailableHelpersHelpers().observe(getActivity(), this::populateListViewAvailableHelpers);
    }

    private void populateListViewAvailableHelpers(ArrayList<Helper> helpers) {
        ArrayList<String> helperNames = new ArrayList<>();

        listViewAvailableHelpersAdapter.clear();
        for (Helper helper : helpers) {
            helperNames.add("+ " + helper.getName());
        }
        listViewAvailableHelpersAdapter.addAll(helperNames);
    }

    private void initUI(View view) {
        listViewHelpersAdapter = new ListViewAdapter(getContext(), R.layout.list_view_row);
        ListView listViewHelpers = view.findViewById(R.id.listViewHelpers);
        listViewHelpers.setAdapter(listViewHelpersAdapter);
        listViewHelpers.setOnItemClickListener((parent, view1, position, id) -> removeHelper(position));

        listViewAvailableHelpersAdapter = new ListViewAdapter(getContext(), R.layout.list_view_row);
        ListView listViewAvailableHelpers = view.findViewById(R.id.listViewAvailableHelpers);
        listViewAvailableHelpers.setAdapter(listViewAvailableHelpersAdapter);
        listViewAvailableHelpers.setOnItemClickListener((parent, view1, position, id) -> moveHelper(position));

        editTextName = view.findViewById(R.id.editTextName);
        editTextName.setOnEditorActionListener(this::onEditorAction);

        RadioGroup radioGroupFlightending = view.findViewById(R.id.radioGroupFlightending);
        radioGroupFlightending.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == 0)
                viewModel.setWaypointMissionFinishedAction(WaypointMissionFinishedAction.GO_HOME);
            else
                viewModel.setWaypointMissionFinishedAction(WaypointMissionFinishedAction.NO_ACTION);
        });
    }

    private void moveHelper(int availableHelperPosition) {
        String helper = listViewAvailableHelpersAdapter.getItem(availableHelperPosition);
        listViewAvailableHelpersAdapter.remove(helper);
        listViewHelpersAdapter.add(helper.substring(2));
    }

    private void removeHelper(int helperPosition) {
        String helper = listViewHelpersAdapter.getItem(helperPosition);
        listViewAvailableHelpersAdapter.add("+ " + helper);
        listViewHelpersAdapter.remove(helper);
    }

    private boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event != null &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if (event == null || !event.isShiftPressed()) {
                setPilotName();
                return false;
            }
        }
        return false;
    }

    private void setPilotName() {
        viewModel.setPilotName(editTextName.getText().toString());
    }
}