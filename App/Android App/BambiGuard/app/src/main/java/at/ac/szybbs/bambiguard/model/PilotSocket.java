package at.ac.szybbs.bambiguard.model;

public class PilotSocket extends BambiGuardSocket {

    public void emitUserIdentification(String pilotName) {
        super.emitUserIdentification("pilot", pilotName);
    }

    public void emitPilotNameChanged(String pilotName) {
        socket.emit("pilot_name_changed", pilotName);
    }

    public void emitGetHelpers() {
        socket.emit("get_helpers");
    }

    public void emitAcquireHelpers(int helperId) {
        socket.emit("acquire_helpers", helperId);
    }

    public void emitBambiFound(Bambi bambi) {
        socket.emit("bambi_found", bambi.getId(), bambi.getPosition().getLatitude(), bambi.getPosition().getLongitude());
    }
}
