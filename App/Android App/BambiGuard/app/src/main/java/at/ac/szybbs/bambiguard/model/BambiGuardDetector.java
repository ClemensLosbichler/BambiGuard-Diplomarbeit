package at.ac.szybbs.bambiguard.model;

public class BambiGuardDetector {
    static {
        System.loadLibrary("bambiGuardDetector");
    }

    public native String detectBambisInImage(int width, int height, byte[] bytes);
}
