package at.ac.szybbs.bambiguard.model;

public class HelperSocket extends BambiGuardSocket {
    public HelperSocket() {
    }

    public void emitUserIdentification(String username) {
        super.emitUserIdentification("helper", username);
    }

    public void emitBambiRescued(int id) {
        super.emitUserIdentification("bambi_rescued", String.valueOf(id));
    }
}
