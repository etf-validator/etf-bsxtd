/**
 * Copyright 2017-2020 European Union, interactive instruments GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf.bsxm.geometry;

import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.segments.Arc;
import org.deegree.geometry.standard.primitive.DefaultPoint;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class CcwNormalizedArc {

    private static final double COLLINEAR_EPSILON = 1E-6;

    private final double planTria;
    private final double minOrd0;
    private final double minOrd1;
    private final double startAngle;
    private final double endAngle;
    private final double radius;
    private final Point startPoint;
    private final Point endPoint;
    private final double centerX;
    private final double centerY;

    CcwNormalizedArc(final Arc arc) {
        this(arc.getPoint1(), arc.getPoint2(), arc.getPoint3(), arc instanceof Circle);
    }

    CcwNormalizedArc(final Point p0, final Point p1, final Point p2) {
        this(p0, p1, p2, false);
    }

    private CcwNormalizedArc(final Point p0, final Point p1, final Point p2, final boolean circle) {
        planTria = (p2.get0() - p0.get0()) * ((p2.get1() + p0.get1()) / 2)
                + (p1.get0() - p2.get0()) * ((p1.get1() + p2.get1()) / 2)
                + (p0.get0() - p1.get0()) * ((p0.get1() + p1.get1()) / 2);
        final Point p0_ccw;
        final Point p2_ccw;
        if (isClockwise()) {
            p0_ccw = p2;
            p2_ccw = p0;
        } else {
            p0_ccw = p0;
            p2_ccw = p2;
        }
        // shift the points down (to reduce the occurrence of floating point errors), independently on the x and y axes
        minOrd0 = findShiftOrd0(p0_ccw, p1, p2_ccw);
        minOrd1 = findShiftOrd1(p0_ccw, p1, p2_ccw);

        final Point p0Shifted = new DefaultPoint(null, p0_ccw.getCoordinateSystem(), p0_ccw.getPrecision(),
                new double[]{p0_ccw.get0() - minOrd0, p0_ccw.get1() - minOrd1});
        final Point p1Shifted = new DefaultPoint(null, p1.getCoordinateSystem(), p1.getPrecision(),
                new double[]{p1.get0() - minOrd0, p1.get1() - minOrd1});
        final Point p2Shifted = new DefaultPoint(null, p2_ccw.getCoordinateSystem(), p2_ccw.getPrecision(),
                new double[]{p2_ccw.get0() - minOrd0, p2_ccw.get1() - minOrd1});

        final double[] center = calcCircleCenter(p0Shifted, p1Shifted, p2Shifted);
        centerX = center[0];
        centerY = center[1];

        // ensure numerical stability for start point (= use original circle start point)
        startPoint = new DefaultPoint(null,
                p0_ccw.getCoordinateSystem(), p0_ccw.getPrecision(),
                new double[]{p0Shifted.get0() + minOrd0, p0Shifted.get1() + minOrd1});

        final double dx = p0Shifted.get0() - centerX;
        final double dy = p0Shifted.get1() - centerY;
        startAngle = Math.atan2(dy, dx);

        if (circle) {
            endAngle = startAngle;
            endPoint = startPoint;
        } else {
            final double ex = p2Shifted.get0() - centerX;
            final double ey = p2Shifted.get1() - centerY;
            endAngle = Math.atan2(ey, ex);
            // ensure numerical stability for end point (= use original circle start point)
            endPoint = createPointWithReducedFloatingPointErrors(p2Shifted.get0(), p2Shifted.get1());
        }
        radius = Math.sqrt(dx * dx + dy * dy);
    }

    Point createPointWithReducedFloatingPointErrors(final double x, final double y) {
        return new DefaultPoint(null,
                startPoint.getCoordinateSystem(),
                startPoint.getPrecision(), new double[]{x + minOrd0, y + minOrd1});
    }

    /**
     * Find the midpoint between the highest and the lowest ordinate 0 axis among the 3 points
     *
     * @param p0
     * @param p1
     * @param p2
     * @return shift midpoint
     */
    private static double findShiftOrd0(Point p0, Point p1, Point p2) {
        final double minOrd0 = Math.min(p0.get0(), Math.min(p1.get0(), p2.get0()));
        final double maxOrd0 = Math.max(p0.get0(), Math.max(p1.get0(), p2.get0()));
        return (maxOrd0 + minOrd0) / 2;
    }

    /**
     * Find the midpoint between the highest and the lowest ordinate 1 axis among the 3 points
     *
     * @param p0
     * @param p1
     * @param p2
     * @return shift midpoint
     */
    private static double findShiftOrd1(Point p0, Point p1, Point p2) {
        final double minOrd1 = Math.min(p0.get1(), Math.min(p1.get1(), p2.get1()));
        final double maxOrd1 = Math.max(p0.get1(), Math.max(p1.get1(), p2.get1()));
        return (maxOrd1 + minOrd1) / 2;
    }

    /**
     * Returns whether the order of the given three points is clockwise or counterclockwise. Uses the (signed) area of a
     * planar triangle to get to know about the order of the points.
     *
     * @return true, if order is clockwise, otherwise false
     * @throws IllegalArgumentException
     *             if no order can be determined, because the points are identical or collinear
     */
    boolean isClockwise() {
        return planTria < 0.0;
    }

    /**
     * Tests if the given three points are collinear.
     * <p>
     * NOTE: Only this method should be used throughout the whole linearization process for testing collinearity to avoid
     * inconsistent results (the necessary EPSILON would differ).
     * </p>
     *
     * @return true if the points are collinear, false otherwise
     */
    boolean areCollinear() {
        return Math.abs(planTria) < COLLINEAR_EPSILON;
    }

    /**
     * Finds the center of a circle/arc that is specified by three points that lie on the circle's boundary.
     * <p>
     * Credits go to <a href="http://en.wikipedia.org/wiki/Circumradius#Coordinates_of_circumcenter">wikipedia</a> (visited
     * on 13/08/09).
     * </p>
     *
     * @param p0
     *            first point
     * @param p1
     *            second point
     * @param p2
     *            third point
     * @return center of the circle (not shifted !!!)
     *
     * @throws IllegalArgumentException
     *             if the points are collinear, i.e. on a single line
     */
    private static double[] calcCircleCenter(final Point p0, final Point p1, final Point p2) throws IllegalArgumentException {
        final double minOrd0 = findShiftOrd0(p0, p1, p2);
        final double minOrd1 = findShiftOrd1(p0, p1, p2);

        // if the points are already shifted, this does no harm!
        final Point p0Shifted = new DefaultPoint(null, p0.getCoordinateSystem(), p0.getPrecision(),
                new double[]{p0.get0() - minOrd0, p0.get1() - minOrd1});
        final Point p1Shifted = new DefaultPoint(null, p1.getCoordinateSystem(), p1.getPrecision(),
                new double[]{p1.get0() - minOrd0, p1.get1() - minOrd1});
        final Point p2Shifted = new DefaultPoint(null, p2.getCoordinateSystem(), p2.getPrecision(),
                new double[]{p2.get0() - minOrd0, p2.get1() - minOrd1});

        final Vector3d a = new Vector3d(p0Shifted.get0(), p0Shifted.get1(), p0Shifted.get2());
        final Vector3d b = new Vector3d(p1Shifted.get0(), p1Shifted.get1(), p1Shifted.get2());
        final Vector3d c = new Vector3d(p2Shifted.get0(), p2Shifted.get1(), p2Shifted.get2());

        if (Double.isNaN(a.z)) {
            a.z = 0.0;
        }
        if (Double.isNaN(b.z)) {
            b.z = 0.0;
        }
        if (Double.isNaN(c.z)) {
            c.z = 0.0;
        }

        final Vector3d ab = new Vector3d(a);
        final Vector3d ac = new Vector3d(a);
        final Vector3d bc = new Vector3d(b);
        final Vector3d ba = new Vector3d(b);
        final Vector3d ca = new Vector3d(c);
        final Vector3d cb = new Vector3d(c);

        ab.sub(b);
        ac.sub(c);
        bc.sub(c);
        ba.sub(a);
        ca.sub(a);
        cb.sub(b);

        final Vector3d cros = new Vector3d();

        cros.cross(ab, bc);
        final double crosSquare = 2 * cros.lengthSquared();
        a.scale((bc.lengthSquared() * ab.dot(ac)) / crosSquare);
        b.scale((ac.lengthSquared() * ba.dot(bc)) / crosSquare);
        c.scale((ab.lengthSquared() * ca.dot(cb)) / crosSquare);

        final Point3d circle = new Point3d(a);
        circle.add(b);
        circle.add(c);

        circle.x += minOrd0;
        circle.y += minOrd1;

        // result must be shifted wih minOrd0 and minOrd1 !!!
        return new double[]{circle.x, circle.y};
    }

    Point getStartPoint() {
        return startPoint;
    }

    Point getEndpoint() {
        return endPoint;
    }

    public double getRadius() {
        return radius;
    }

    double getStartAngle() {
        return this.startAngle;
    }

    double getEndAngle() {
        return this.endAngle;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    List<Point> transformToOriginalOrientation(final List<Point> mutableInterpolationPoints) {
        if (isClockwise()) {
            return new ListRevWrapper<>(mutableInterpolationPoints);
        }
        return mutableInterpolationPoints;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NormalizedArcPoints{");
        sb.append("planTria=").append(planTria);
        sb.append(", cw=").append(isClockwise());
        sb.append(", col=").append(areCollinear());
        sb.append(", minOrd0=").append(minOrd0);
        sb.append(", minOrd1=").append(minOrd1);
        sb.append(", startAngle=").append(startAngle);
        sb.append(", endAngle=").append(endAngle);
        sb.append(", radius=").append(radius);
        sb.append(", startPoint=").append(startPoint);
        sb.append(", endPoint=").append(endPoint);
        sb.append(", centerX=").append(centerX);
        sb.append(", centerY=").append(centerY);
        sb.append('}');
        return sb.toString();
    }
}
