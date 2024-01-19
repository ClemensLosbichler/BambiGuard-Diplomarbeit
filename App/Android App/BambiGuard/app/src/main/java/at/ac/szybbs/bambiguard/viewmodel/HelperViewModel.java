package at.ac.szybbs.bambiguard.viewmodel;

import androidx.lifecycle.ViewModel;
import at.ac.szybbs.bambiguard.model.HelperSocket;

public class HelperViewModel extends ViewModel {
    private static HelperViewModel instance;
    private final HelperSocket socket;

    private String pilotName = "";

    public HelperViewModel() {
        socket = new HelperSocket();
    }

    public static HelperViewModel getInstance() {
        if (instance == null)
            instance = new HelperViewModel();
        return instance;
    }

    public static void initializeHelperViewModel() {
        instance = new HelperViewModel();
    }

    public HelperSocket getSocket() {
        return socket;
    }

    public String getPilotName() {
        return pilotName;
    }

    public void setPilotName(String pilotName) {
        this.pilotName = pilotName;
    }
}
