package at.ac.szybbs.bambiguard.model;

import android.util.Log;

import java.util.ArrayList;

public class BambiGuardCoveragePlanner {
    private static final double PRECISION = 0.05;

    public static ArrayList<Point> decomposePolygon(ArrayList<Point> polygon, double cameraCoverage) {
        ArrayList<Line> intersections = generateIntersections(polygon, cameraCoverage);
        return orderPoints(intersections);
    }

    private static ArrayList<Line> generateIntersections(ArrayList<Point> polygon, double cameraCoverage) {
        ArrayList<Line> lines = new ArrayList<>();

        double[] bounds = calculatePolygonBoundingBox(polygon);

        Line startingLine = new Line(new Point(bounds[0], bounds[1]), new Point(bounds[0], bounds[3]));
        lines.add(startingLine);

        int iterations = (int) Math.ceil((bounds[2] - bounds[0]) / cameraCoverage);

        for (int i = 0; i < iterations; i++) {
            Line line = startingLine.getParallelLine(-cameraCoverage * i);
            lines.addAll(getLinesIntersectingWithPolygon(polygon, line));
        }

        return lines;
    }

    private static ArrayList<Point> orderPoints(ArrayList<Line> intersections) {
        ArrayList<Point> points = new ArrayList<>();
        for (Line line : intersections) {
            points.add(line.getStart());
            points.add(line.getEnd());
        }

        return points;
    }

    private static double[] calculatePolygonBoundingBox(ArrayList<Point> polygon) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = 0, maxY = 0;

        for (Point point : polygon) {
            minX = Double.min(minX, point.getX());
            minY = Double.min(minY, point.getY());
            maxX = Double.max(maxX, point.getX());
            maxY = Double.max(maxY, point.getY());
        }

        return new double[]{minX, minY, maxX, maxY};
    }

    private static ArrayList<Line> getLinesIntersectingWithPolygon(ArrayList<Point> polygon, Line line) {
        ArrayList<Line> lines = new ArrayList<>();
        Point firstPointInside = null;

        for (double i = 0; i <= 1; i += PRECISION) {
            Point vector = line.getEnd().minus(line.getStart()).scalarProduct(i);
            Point point = line.getStart().plus(vector);

//            if (isPointInsidePolygon(polygon, point)) {
            if (isPointInsidePolygonShort(polygon, point)) {
                if (firstPointInside == null) firstPointInside = point;
            } else if (firstPointInside != null) {
                lines.add(new Line(firstPointInside, point));
                firstPointInside = null;
                Log.d("kek", "Point outside: " + i);
            }
        }

        return lines;
    }

    private static boolean isPointInsidePolygon(ArrayList<Point> polygon, Point point) {
        int count = 0;

        Point point1 = polygon.get(0);

        for (int i = 1; i <= polygon.size(); i++) {
            if (point.equals(point1))
                return true;

            Point point2 = polygon.get(i % polygon.size());

            // point is above or under the line
            if (point.getY() < Math.min(point1.getY(), point2.getY()) ||
                    point.getY() > Math.max(point1.getY(), point2.getY())) continue;

            // point is before line
            if (point.getX() <= Math.max(point1.getX(), point2.getX())) {
                // line is horizontal and point lies on line
                if (point1.getY() == point2.getY() && point.getX() >= Math.min(point1.getX(), point2.getX()))
                    return true;

                // line is vertical
                if (point1.getX() == point2.getX()) {
                    // point lies on vertical line
                    if (point.getX() == point1.getX()) return true;
                    else count++; // point is before line
                } else {
                    // cross point on the left side

                    // cross point of x
                    double xinters = (point.getY() - point1.getY()) * (point2.getX() - point1.getX()) / (point2.getY() - point1.getY()) + point1.getX();

                    // overlies on a line
                    if (Math.abs(point.getX() - xinters) < Double.MIN_NORMAL) return true;

                    // before line
                    if (point.getX() < xinters) count++;
                }
            }

            point1 = point2;
        }

        return count % 2 == 1;
    }

    private static boolean isPointInsidePolygonShort(ArrayList<Point> polygon, Point p) {
        boolean c = false;
        int i, j;
        for (i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            if (polygon.get(i).equals(p))
                return true;
            if ((((polygon.get(i).getY() <= p.getY()) && (p.getY() < polygon.get(j).getY())) ||
                    ((polygon.get(j).getY() <= p.getY()) && (p.getY() < polygon.get(i).getY()))) &&
                    (p.getX() < (polygon.get(j).getX() - polygon.get(i).getX()) * (p.getY() - polygon.get(i).getY()) /
                            (polygon.get(j).getY() - polygon.get(i).getY()) + polygon.get(i).getX()))
                c = !c;
        }
        return c;
    }
}
