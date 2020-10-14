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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.linear.*;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.geometry.GeometryFactory;
import org.deegree.geometry.linearization.LinearizationCriterion;
import org.deegree.geometry.linearization.MaxErrorCriterion;
import org.deegree.geometry.linearization.NumPointsCriterion;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.precision.PrecisionModel;
import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.Ring;
import org.deegree.geometry.primitive.segments.*;
import org.deegree.geometry.primitive.segments.Arc;
import org.deegree.geometry.standard.curvesegments.DefaultLineStringSegment;
import org.deegree.geometry.standard.points.PointsList;
import org.deegree.geometry.standard.primitive.DefaultPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fixed version that supports tolerance
 */
public class CustomCurveLinearizer {
    private static Logger LOG = LoggerFactory.getLogger(org.deegree.geometry.linearization.CurveLinearizer.class);

    private static final double EPSILON = 1E-6;

    private final GeometryFactory geomFac;
    private double tolerance;

    private final static double TWO_PI = Math.PI * 2;

    public CustomCurveLinearizer(final GeometryFactory geomFac, final double tolerance) {
        this.geomFac = geomFac;
        this.tolerance = tolerance;
    }

    /**
     * Returns a linearized version of the given {@link Curve} geometry.
     * <p>
     * NOTE: This method respects the semantic difference between {@link Curve} and {@link Ring} geometries: if the input is
     * a {@link Ring}, a ring geometry will be returned.
     *
     * @param curve
     *            curve to be linearized, must not be <code>null</code>
     * @param crit
     *            linearization criterion, must not be <code>null</code>
     * @return linearized version of the input curve, never <code>null</code>
     */
    public Curve linearize(final Curve curve, final LinearizationCriterion crit) {
        final Curve linearizedCurve;
        switch (curve.getCurveType()) {
        case LineString: {
            // both LineString and LinearRing are handled by this case
            linearizedCurve = curve;
            break;
        }
        default: {
            if (curve instanceof Ring) {
                final Ring ring = (Ring) curve;
                final List<Curve> curves = ring.getMembers();
                final List<Curve> linearizedMembers = new ArrayList<>(curves.size());
                for (Curve member : curves) {
                    linearizedMembers.add(linearize(member, crit));
                }
                linearizedCurve = geomFac.createRing(ring.getId(), ring.getCoordinateSystem(), linearizedMembers);
            } else {
                final List<CurveSegment> segments = curve.getCurveSegments();
                final CurveSegment[] linearSegments = new CurveSegment[segments.size()];
                for (int i = 0; i < linearSegments.length; i++) {
                    linearSegments[i] = linearize(segments.get(i), crit);
                }
                linearizedCurve = geomFac.createCurve(curve.getId(), curve.getCoordinateSystem(), linearSegments);
            }
            break;
        }
        }
        return linearizedCurve;
    }

    /**
     * Returns a linearized version (i.e. a {@link LineStringSegment}) of the given {@link CurveSegment}.
     *
     * @param segment
     *            segment to be linearized, must not be <code>null</code>
     * @param crit
     *            determines the interpolation quality / number of interpolation points, must not be <code>null</code>
     * @return linearized version of the input segment, never <code>null</code>
     */
    public LineStringSegment linearize(CurveSegment segment, LinearizationCriterion crit) {
        LineStringSegment lineSegment = null;
        switch (segment.getSegmentType()) {
        case ARC:
        case CIRCLE:
            lineSegment = linearizeArc((Arc) segment, crit);
            break;
        case LINE_STRING_SEGMENT: {
            lineSegment = (LineStringSegment) segment;
            break;
        }
        case CUBIC_SPLINE: {
            lineSegment = linearizeCubicSpline((CubicSpline) segment, crit);
            break;
        }
        case ARC_STRING: {
            lineSegment = linearizeArcString((ArcString) segment, crit);
            break;
        }
        case GEODESIC_STRING: {
            lineSegment = linearizeGeodesicString((GeodesicString) segment, crit);
            break;
        }
        case ARC_BY_BULGE:
        case ARC_BY_CENTER_POINT:
        case ARC_STRING_BY_BULGE:
        case BEZIER:
        case BSPLINE:
        case CIRCLE_BY_CENTER_POINT:
        case CLOTHOID:
        case GEODESIC:
        case OFFSET_CURVE: {
            String msg = "Linearization of curve segment type '" + segment.getSegmentType().name()
                    + "' is not implemented yet.";
            throw new IllegalArgumentException(msg);
        }
        }
        return lineSegment;
    }

    private LineStringSegment linearizeGeodesicString(GeodesicString segment, LinearizationCriterion crit) {
        return new DefaultLineStringSegment(segment.getControlPoints());
    }

    /**
     * Returns a linearized version (i.e. a {@link LineStringSegment}) of the given
     * {@link org.deegree.geometry.primitive.segments.Arc}.
     * <p>
     * If the three control points <code>p0</code>, <code>p1</code> and <code>p2</code> of the arc are collinear, i.e. on a
     * straight line, the behaviour depends on the type of {@link org.deegree.geometry.primitive.segments.Arc}:
     * <ul>
     * <li>Generic {@link org.deegree.geometry.primitive.segments.Arc}: returns the linear segment
     * <code>(p0, p2)</code></li>
     * <li>{@link de.interactive_instruments.etf.bsxm.geometry.Circle}: returns the linear segment
     * <code>(p0, p1, p0)</code></li>
     * </ul>
     *
     * @param arc
     *            segment to be linearized, must not be <code>null</code>
     * @param crit
     *            determines the interpolation quality / number of interpolation points, must not be <code>null</code>
     * @return linearized version of the input segment, never <code>null</code>
     */
    public LineStringSegment linearizeArc(final Arc arc, final LinearizationCriterion crit) {
        final CcwNormalizedArc ccwNormalizedArc = new CcwNormalizedArc(arc);
        if (ccwNormalizedArc.areCollinear()) {
            // if the points are already on a line we don't need to (and must not) apply any linearization algorithm
            final Points points;
            if (arc instanceof de.interactive_instruments.etf.bsxm.geometry.Circle) {
                points = new PointsList(
                        Arrays.asList(arc.getPoint1(), arc.getPoint2(), arc.getPoint1()));
            } else {
                points = new PointsList(Arrays.asList(arc.getPoint1(), arc.getPoint3()));
            }
            return geomFac.createLineStringSegment(points);
        } else if (crit instanceof NumPointsCriterion) {
            final int numPoints = ((NumPointsCriterion) crit).getNumberOfPoints();
            return geomFac.createLineStringSegment(new PointsList(interpolate(ccwNormalizedArc, numPoints)));
        } else if (crit instanceof MaxErrorCriterion) {

            double error = ((MaxErrorCriterion) crit).getMaxError();
            int numPoints = calcNumPoints(ccwNormalizedArc, arc instanceof Circle, error);
            int maxNumPoints = ((MaxErrorCriterion) crit).getMaxNumPoints();
            if (maxNumPoints > 0 && maxNumPoints < numPoints) {
                numPoints = maxNumPoints;
            }
            LOG.debug("Using " + numPoints + " for segment linearization.");
            return geomFac.createLineStringSegment(new PointsList(interpolate(ccwNormalizedArc, numPoints)));
        } else {
            String msg = "Handling of criterion '" + crit.getClass().getName() + "' is not implemented yet.";
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Returns a linearized version (i.e. a {@link LineStringSegment}) of the given {@link ArcString}.
     * <p>
     * If one of the arc elements is collinear, it will be added as a straight segment.
     * </p>
     *
     * @param arcString
     *            curve segment to be linearized, must not be <code>null</code>
     * @param crit
     *            determines the interpolation quality / number of interpolation points, must not be <code>null</code>
     * @return linearized version of the input string, never <code>null</code>
     */
    public LineStringSegment linearizeArcString(ArcString arcString, LinearizationCriterion crit) {
        final Points srcpnts = arcString.getControlPoints();
        final List<Point> points = new ArrayList<>();
        for (int i = 0, j = (srcpnts.size() - 2); i < j; i += 2) {
            final Point a = srcpnts.get(i);
            final Point b = srcpnts.get(i + 1);
            final Point c = srcpnts.get(i + 2);
            final CcwNormalizedArc ccwNormalizedArc = new CcwNormalizedArc(a, b, c);
            if (ccwNormalizedArc.areCollinear()) {
                points.add(a);
                points.add(b);
                points.add(c);
            } else if (crit instanceof NumPointsCriterion) {
                int numPoints = ((NumPointsCriterion) crit).getNumberOfPoints();
                points.addAll(interpolate(ccwNormalizedArc, numPoints));
            } else if (crit instanceof MaxErrorCriterion) {
                double error = ((MaxErrorCriterion) crit).getMaxError();
                int numPoints = calcNumPoints(ccwNormalizedArc, false, error);
                int maxNumPoints = ((MaxErrorCriterion) crit).getMaxNumPoints();
                if (maxNumPoints > 0 && maxNumPoints < numPoints) {
                    numPoints = maxNumPoints;
                }
                points.addAll(interpolate(ccwNormalizedArc, numPoints));
            } else {
                String msg = "Handling of criterion '" + crit.getClass().getName() + "' is not implemented yet.";
                throw new IllegalArgumentException(msg);
            }
        }
        return geomFac.createLineStringSegment(new PointsList(points));
    }

    /**
     * Returns a linearized version (i.e. a {@link LineStringSegment}) of the given {@link CubicSpline}.
     * <p>
     * A cubic spline consists of n polynomials of degree 3: S<sub>j</sub>(x) = a<sub>j</sub> +
     * b<sub>j</sub>*(x-x<sub>j</sub>) + c<sub>j</sub>*(x-x<sub>j</sub>)<sup>2</sup> +
     * d<sub>j</sub>*(x-x<sub>j</sub>)<sup>3</sup>; that acts upon the interval [x<sub>j</sub>,x<sub>j+1</sub>], 0 <=j< n.
     * <p>
     * The algorithm for generating points on a spline defined with only control points and starting/ending tangents can be
     * found at <a href="http://persson.berkeley.edu/128A/lec14-2x3.pdf">http://persson.berkeley.edu/128A/lec14-2x3.pdf</a>
     * (last visited 19/08/09)
     *
     * @param spline
     *            curve segment to be linearized, must not be <code>null</code>
     * @param crit
     *            determines the interpolation quality / number of interpolation points, must not be <code>null</code>
     * @return linearized version of the input segment, never <code>null</code>
     */
    public LineStringSegment linearizeCubicSpline(CubicSpline spline, LinearizationCriterion crit) {

        if (spline.getCoordinateDimension() != 2) {
            throw new UnsupportedOperationException(
                    "Linearization of the cubic spline is only suported for a spline in 2D.");
        }

        Points controlPts = spline.getControlPoints();
        // build an array of Point in order to sort it in ascending order
        Point[] pts = new Point[controlPts.size()];
        // n denotes the # of polynomials, that is one less than the # of control pts
        int n = controlPts.size() - 1;
        for (int i = 0; i <= n; i++) {
            pts[i] = controlPts.get(i);
        }

        double startTan = Math.atan2(spline.getVectorAtStart().get1(), spline.getVectorAtStart().get0());
        double endTan = Math.atan2(spline.getVectorAtEnd().get1(), spline.getVectorAtEnd().get0());

        boolean ascending = true;
        if (pts[0].get0() > pts[1].get0()) {
            ascending = false;
        }

        for (int i = 0; i <= n - 1; i++) {
            if (ascending) {
                if (pts[i].get0() > pts[i + 1].get0()) {
                    throw new UnsupportedOperationException(
                            "It is expected that the control points are ordered on the X-axis either ascendingly or descendingly.");
                }
            } else {
                if (pts[i].get0() < pts[i + 1].get0()) {
                    throw new UnsupportedOperationException(
                            "It is expected that the control points are ordered on the X-axis either ascendingly or descendingly.");
                }
            }
        }

        if (!ascending) {
            // interchange the elements so that they are ordered ascendingly (on the X-axis)
            for (int i = 0; i <= (n / 2); i++) {
                Point aux = pts[i];
                pts[i] = pts[n - i];
                pts[n - i] = aux;
            }
            // also reverse the starting and ending tangents
            startTan = Math.atan2(-spline.getVectorAtEnd().get1(), -spline.getVectorAtEnd().get0());
            endTan = Math.atan2(-spline.getVectorAtStart().get1(), -spline.getVectorAtStart().get0());
        }

        // break-up the pts into xcoor in ycoor
        double[] xcoor = new double[n + 1];
        double[] ycoor = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            xcoor[i] = pts[i].get0();
            ycoor[i] = pts[i].get1();
        }

        double[] h = new double[n];
        for (int i = 0; i <= n - 1; i++) {
            h[i] = xcoor[i + 1] - xcoor[i];
        }

        double[][] matrixA = constructMatrixA(h, n);

        double[] vectorb = constructVectorB(n, ycoor, h, startTan, endTan);

        double[] vectorx = solveLinearEquation(matrixA, vectorb);

        int numPoints = -1;
        if (crit instanceof NumPointsCriterion) {
            numPoints = ((NumPointsCriterion) crit).getNumberOfPoints();
        } else if (crit instanceof MaxErrorCriterion) {
            numPoints = ((MaxErrorCriterion) crit).getMaxNumPoints();
            if (numPoints <= 0) {
                throw new UnsupportedOperationException(
                        "Linearization of the cubic spline with MaxErrorCriterion is currently not supported, unless the number of points is provided.");
                // TODO it is mathematically hard to get an expression of the numOfPoints with respect to the error;
                // there would be two work-arounds as I can see them: 1) through a trial-and-error procedure determine
                // how small should the sampling interval be, so that the difference in value is less than the
                // given error; 2) use the mathematical expression used for the arc/circle (something with Math.acos...)
                // - it needs a good approximation for the radius.
            }
        }

        double[] interpolated = interpolateSpline(n, h, xcoor, ycoor, vectorx, numPoints);

        // populate a list of points, so that later a LineStringSegment can be built from it
        List<Point> iPoints = new ArrayList<>(numPoints);
        ICRS crs = spline.getControlPoints().get(0).getCoordinateSystem();
        PrecisionModel pm = spline.getControlPoints().get(0).getPrecision();
        for (int i = 0; i < numPoints; i++) {
            iPoints.add(new DefaultPoint(null, crs, pm, new double[]{interpolated[2 * i], interpolated[2 * i + 1]}));
        }
        return geomFac.createLineStringSegment(new PointsList(iPoints));
    }

    private double[] constructVectorB(int n, double[] ycoor, double[] h, double startTan, double endTan) {
        double[] vectorb = new double[n + 1];
        vectorb[0] = 3 * (ycoor[1] - ycoor[0]) / h[0] - 3 * startTan;
        for (int i = 1; i <= n - 1; i++) {
            vectorb[i] = 3 * (ycoor[i + 1] - ycoor[i]) / h[i] - 3 * (ycoor[i] - ycoor[i - 1]) / h[i - 1];
        }
        vectorb[n] = 3 * endTan - 3 * (ycoor[n] - ycoor[n - 1]) / h[n - 1];

        return vectorb;
    }

    private double[] solveLinearEquation(double[][] matrixA, double[] vectorb) {

        RealMatrix coefficients = new Array2DRowRealMatrix(matrixA, false);

        // LU-decomposition
        DecompositionSolver solver = new LUDecompositionImpl(coefficients).getSolver();

        RealVector constants = new ArrayRealVector(vectorb, false);
        RealVector solution = null;
        try {
            solution = solver.solve(constants);
        } catch (SingularMatrixException e) {
            LOG.error(e.getLocalizedMessage());
            e.printStackTrace();
        }
        return solution.getData();
    }

    private double[] interpolateSpline(int n, double[] h, double[] xcoor, double[] ycoor, double[] vectorx,
            int numPoints) {
        double[] interpolated = new double[2 * numPoints];

        // compute coefficients of spline
        double[] a = new double[n + 1];
        double[] c = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            a[i] = ycoor[i];
            c[i] = vectorx[i];
        }

        double[] b = new double[n];
        double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            b[i] = (a[i + 1] - a[i]) / h[i] - h[i] * (2 * c[i] + c[i + 1]) / 3;
            d[i] = (c[i + 1] - c[i]) / (3 * h[i]);
        }

        // compute the spacing between points
        double spacing = (xcoor[n] - xcoor[0]) / (numPoints - 1);

        // current segment of polynomial
        int seg = 0;
        for (int i = 0; i <= numPoints - 1; i++) {
            double x = xcoor[0] + i * spacing;
            if (x > xcoor[seg + 1]) {
                seg++;
            }

            double y = a[seg] + b[seg] * (x - xcoor[seg]) + c[seg] * Math.pow(x - xcoor[seg], 2) + d[seg]
                    * Math.pow(x - xcoor[seg], 3);

            interpolated[2 * i] = x;
            interpolated[2 * i + 1] = y;
        }

        return interpolated;
    }

    private static double[][] constructMatrixA(double[] h, int n) {
        // first line
        double[][] matrixA = new double[n + 1][n + 1];
        Arrays.fill(matrixA[0], 0);
        matrixA[0][0] = 2 * h[0];
        matrixA[0][1] = h[0];

        // middle lines
        for (int i = 1; i <= n - 1; i++) {
            Arrays.fill(matrixA[i], 0);
            matrixA[i][i - 1] = h[i - 1];
            matrixA[i][i] = 2 * (h[i - 1] + h[i]);
            matrixA[i][i + 1] = h[i];
        }

        Arrays.fill(matrixA[n], 0);
        matrixA[n][n - 1] = h[n - 1];
        matrixA[n][n] = 2 * h[n - 1];

        return matrixA;
    }

    private List<Point> interpolate(final CcwNormalizedArc arcPoints, int numPoints) {
        final List<Point> interpolationPoints = new ArrayList<>(numPoints);
        double radius = arcPoints.getRadius();
        radius += this.tolerance;

        final double angleStep = createAngleStep(arcPoints, numPoints);
        // ensure numerical stability for start point (= use original circle start point)
        interpolationPoints.add(arcPoints.getStartPoint());

        // calculate intermediate (=interpolated) points on arc
        for (int i = 1; i < numPoints - 1; i++) {
            final double angle = arcPoints.getStartAngle() + i * angleStep;
            final double x = arcPoints.getCenterX() + Math.cos(angle) * radius;
            final double y = arcPoints.getCenterY() + Math.sin(angle) * radius;
            interpolationPoints.add(arcPoints.createPointWithReducedFloatingPointErrors(x, y));
        }
        interpolationPoints.add(arcPoints.getEndpoint());
        return arcPoints.transformToOriginalOrientation(interpolationPoints);
    }

    private static int calcNumPoints(CcwNormalizedArc arcPoints, final boolean isCircle, double error) {
        final double angleStep = 2 * Math.acos(1 - error / arcPoints.getRadius());
        final int numPoints;
        if (isCircle) {
            numPoints = (int) Math.ceil(2 * Math.PI / angleStep) + 1;
        } else {
            // if (!ccwNormalizedArcPoints.isClockwise()) {
            double endAngle = arcPoints.getEndAngle();
            if (endAngle < arcPoints.getStartAngle()) {
                endAngle += 2 * Math.PI;
            }
            numPoints = (int) Math.ceil((endAngle - arcPoints.getStartAngle()) / angleStep) + 1;
        }
        return numPoints;
    }

    private static double createAngleStep(final CcwNormalizedArc arcPoints, int numPoints) {
        final boolean isCircle = (Math.abs(arcPoints.getStartAngle() - arcPoints.getEndAngle()) < 1E-10);
        double sweepAngle = isCircle ? TWO_PI : (arcPoints.getStartAngle() - arcPoints.getEndAngle());
        if (!isCircle) {
            if (sweepAngle < 0) {
                /**
                 * Because sweepangle is negative but we are going ccw the sweepangle must be mathematically inverted
                 */
                sweepAngle = Math.abs(sweepAngle);
            } else {
                sweepAngle = TWO_PI - sweepAngle;
            }

        }
        return sweepAngle / (numPoints - 1);
    }
}
