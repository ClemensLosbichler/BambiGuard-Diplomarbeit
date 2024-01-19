package at.ac.szybbs.bambiguard.model;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public abstract class BambiGuardSocket {
    private final String SERVER_URL_HOME = "http://10.0.0.32:3000";
    private final String SERVER_URL_HOTSPOT = "http://192.168.43.184:3000";
    private final String SERVER_URL_SCHOOL = "http://10.20.152.170:3000";
    private final String SERVER_URL_DDNS = "http://losbichler.ddns.net:3000";
    private final String SERVER_URL = SERVER_URL_DDNS;
    private final ArrayList<EventListener> eventListeners = new ArrayList<>();
    protected Socket socket;
    private boolean identificationSent = false;

    public BambiGuardSocket() {
        try {
            IO.Options options = new IO.Options();
            options.timeout = 30000;
            options.reconnection = true;
            options.reconnectionAttempts = 10;
            options.reconnectionDelay = 1000;
            options.forceNew = true;
            socket = IO.socket(SERVER_URL, options);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        addEventListener(this::onDisconnect, Socket.EVENT_DISCONNECT);
        socket.connect();
    }

    private void onDisconnect(Object... args) {
        identificationSent = false;
    }

    public void reconnect() {
        if (socket.connected())
            return;

        socket.connect();
    }

    public void emitUserIdentification(String type, String username) {
        socket.emit("user_identification", type, username);
        identificationSent = true;
    }

    public void addEventListener(Emitter.Listener listener, String event) {
        socket.on(event, listener);
        eventListeners.add(new EventListener(listener, event));
    }

    public void removeEventListener(Emitter.Listener listener) {
        for (EventListener eventlistener : eventListeners) {
            if(eventlistener.listener.equals(listener))
            socket.off(eventlistener.event, eventlistener.listener);
        }
    }

    public void removeAllEventListeners() {
        for (EventListener eventlistener : eventListeners) {
            socket.off(eventlistener.event, eventlistener.listener);
        }
    }

    public void onDestroy() {
        socket.disconnect();
        removeAllEventListeners();
    }

    public boolean identificationSent() {
        return identificationSent;
    }

    private static class EventListener {
        Emitter.Listener listener;
        String event;

        public EventListener(Emitter.Listener listener, String event) {
            this.listener = listener;
            this.event = event;
        }
    }
}
