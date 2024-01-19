package at.ac.szybbs.bambiguard.model;

public class Line {
    private Point start;
    private Point end;

    public Line(Point start, Point end) {
        this.start = start;
        this.end = end;
    }

    public Point getStart() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point getEnd() {
        return end;
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    public double getLength() {
        double differenceX = start.getX() - end.getX(),
                differenceY = start.getY() - end.getY();
        return Math.hypot(differenceX, differenceY);
    }

    public Point getNormalVector() {
        Point vector = new Point(end.getX() - start.getX(), end.getY() - start.getY());
        return new Point(-vector.getY(), vector.getX());
    }

    public Line getParallelLine(double offset) {
        Point vector = new Point(end.getX() - start.getX(), end.getY() - start.getY());
        Point normalVector = getNormalVector();

        double normalVector0 = 1 / normalVector.getLength();
        Point stretchedNormalVector = normalVector.scalarProduct(normalVector0).scalarProduct(offset);

        Point parallelStart = new Point(start.getX() + stretchedNormalVector.getX(), start.getY() + stretchedNormalVector.getY());
        Point parallelEnd = new Point(parallelStart.getX() + vector.getX(), parallelStart.getY() + vector.getY());
        return new Line(parallelStart, parallelEnd);
    }

    @Override
    public String toString() {
        return "Line: " + start +
                ", " + end;
    }
}
