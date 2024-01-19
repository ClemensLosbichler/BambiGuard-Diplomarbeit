package at.ac.szybbs.bambiguard.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import at.ac.szybbs.bambiguard.R;
import at.ac.szybbs.bambiguard.viewmodel.PilotViewModel;

public class ConnectDroneDialogFragment extends DialogFragment {

    private PilotViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(R.layout.fragment_connect_drone_dialog)
                .setCancelable(false)
                .setPositiveButton(R.string.Start_flight, (dialog, buttonId) -> {
                    if (viewModel == null)
                        viewModel = PilotViewModel.getInstance();
                    viewModel.setConnectDroneDialogVisible(PilotViewModel.ConnectDroneDialog.Continue);
                })
                .setNegativeButton(getString(R.string.abort), (dialog, buttonId) -> {
                    if (viewModel == null)
                        viewModel = PilotViewModel.getInstance();
                    viewModel.setConnectDroneDialogVisible(PilotViewModel.ConnectDroneDialog.Dismiss);
                });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(d -> ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false));
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = PilotViewModel.getInstance();
        viewModel.getDroneConnected().observe(getViewLifecycleOwner(), this::toggleDroneConnected);
    }

    private void toggleButtonPositive(boolean enabled) {
        ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
    }

    public void toggleDroneConnected(boolean droneConnected) {
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog != null) {
            TextView textViewDroneConnection = alertDialog.findViewById((R.id.textViewDroneConnected));
            if (droneConnected) {
                textViewDroneConnection.setText(R.string.connected);
                textViewDroneConnection.setTextColor(getResources().getColor(R.color.colorPrimaryDark, getResources().newTheme()));
                toggleButtonPositive(true);
            } else {
                textViewDroneConnection.setText(R.string.disconnected);
                textViewDroneConnection.setTextColor(getResources().getColor(R.color.colorRed, getResources().newTheme()));
                toggleButtonPositive(false);
            }
        }
    }
}
