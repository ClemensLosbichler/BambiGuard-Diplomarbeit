package at.ac.szybbs.bambiguard.model;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.Objects;

public class Point {
    private double x;
    private double y;


    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getLength() {
        return Math.abs(Math.hypot(x, y));
    }

    public Point plus(Point point) {
        return new Point(x + point.getX(), y + point.getY());
    }

    public Point minus(Point point) {
        return new Point(x - point.getX(), y - point.getY());
    }

    public Point scalarProduct(double scalar) {
        return new Point(x * scalar, y * scalar);
    }

    public LatLng toLatLng() {
        return new LatLng(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Double.compare(point.x, x) == 0 && Double.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return x + "|" + y;
    }
}
