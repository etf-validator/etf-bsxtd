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
import java.util.List;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.LinearRing;

import org.deegree.geometry.Geometry;
import org.deegree.geometry.composite.CompositeGeometry;
import org.deegree.geometry.linearization.LinearizationCriterion;
import org.deegree.geometry.multi.MultiGeometry;
import org.deegree.geometry.primitive.*;
import org.deegree.geometry.primitive.patches.PolygonPatch;
import org.deegree.geometry.primitive.patches.SurfacePatch;
import org.deegree.geometry.primitive.segments.ArcByCenterPoint;
import org.deegree.geometry.primitive.segments.BSpline;
import org.deegree.geometry.primitive.segments.Clothoid;
import org.deegree.geometry.primitive.segments.CubicSpline;
import org.deegree.geometry.primitive.segments.CurveSegment;
import org.deegree.geometry.primitive.segments.CurveSegment.CurveSegmentType;
import org.deegree.geometry.primitive.segments.GeodesicString;
import org.deegree.geometry.primitive.segments.LineStringSegment;
import org.deegree.geometry.primitive.segments.OffsetCurve;
import org.deegree.geometry.validation.GeometryValidationEventHandler;
import org.deegree.geometry.validation.GeometryValidator;
import org.deegree.geometry.validation.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Geometry validator, loosely based on the according deegree class. This class is optimized for GmlGeoX (does not
 * compute validation events that are ignored by the GMLValidationEventHandler, also performs orientation checks [and
 * linearization for such checks] for a given geometry only if it is valid according to JTS validation).
 * <p>
 * With this class, the issue of incorrect results when checking the orientation of surface boundaries whose coordinates
 * are given in a left-handed CRS could also be fixed. For more details, see
 * https://github.com/deegree/deegree3/issues/886.
 */
public class IIGeometryValidator {

    private static final Logger logger = LoggerFactory.getLogger(IIGeometryValidator.class);

    private final CustomCurveLinearizer linearizer;
    private final LinearizationCriterion crit;
    private final GeometryValidationEventHandler eventHandler;

    /**
     * Creates a new {@link GeometryValidator} which performs callbacks on the given {@link GeometryValidationEventHandler}
     * in case of errors.
     *
     * @param eventHandler
     *            callback handler for errors, must not be <code>null</code>
     * @param geomFac
     *            The geometry factory to use.
     */
    public IIGeometryValidator(final GeometryValidationEventHandler eventHandler, final IIGeometryFactory geomFac) {
        this.eventHandler = eventHandler;
        this.linearizer = new CustomCurveLinearizer(geomFac, 0.0);
        this.crit = geomFac.getMaxErrorCriterion();
    }

    /**
     * Validates the given {@link Geometry}.
     * <p>
     * Contained geometry objects and geometry particles are recursively checked (e.g. the members of a
     * {@link MultiGeometry}) and callbacks to the associated {@link GeometryValidationEventHandler} are performed for each
     * detected issue.
     *
     * @param geom
     *            geometry to be validated
     * @param jtsValidationSucceeded
     *            <code>true</code> if the JTS geometry representing the given deegree geometry is known to be valid
     *            according to JTS validation, <code>false</code> if it is known to be invalid; relevant for some checks
     *            that require valid JTS geometries
     * @return true, if the geometry is valid, false otherwise (depends on the {@link GeometryValidationEventHandler}
     *         implementation)
     */
    public boolean validateGeometry(Geometry geom, boolean jtsValidationSucceeded) {
        return validateGeometry(geom, new ArrayList<>(), jtsValidationSucceeded);
    }

    private boolean validateGeometry(final Geometry geom, final List<Object> affectedGeometryParticles,
            final boolean jtsValidationSucceeded) {
        boolean isValid = false;
        switch (geom.getGeometryType()) {
        case COMPOSITE_GEOMETRY: {
            isValid = validate((CompositeGeometry<?>) geom, affectedGeometryParticles, jtsValidationSucceeded);
            break;
        }
        case ENVELOPE: {
            String msg = "Internal error: envelope 'geometries' should not occur here.";
            throw new IllegalArgumentException(msg);
        }
        case MULTI_GEOMETRY: {
            isValid = validate((MultiGeometry<?>) geom, affectedGeometryParticles, jtsValidationSucceeded);
            break;
        }
        case PRIMITIVE_GEOMETRY: {
            isValid = validate((GeometricPrimitive) geom, affectedGeometryParticles, jtsValidationSucceeded);
            break;
        }
        }
        return isValid;
    }

    private boolean validate(GeometricPrimitive geom, List<Object> affectedGeometryParticles,
            boolean jtsValidationSucceeded) {
        boolean isValid = true;
        switch (geom.getPrimitiveType()) {
        case Point: {
            logger.debug("Point geometry. No validation necessary.");
            break;
        }
        case Curve: {
            isValid = validateCurve((Curve) geom, affectedGeometryParticles);
            break;
        }
        case Surface: {
            isValid = validateSurface((Surface) geom, affectedGeometryParticles, jtsValidationSucceeded);
            break;
        }
        case Solid: {
            String msg = "Validation of solids is not available";
            throw new IllegalArgumentException(msg);
        }
        }
        return isValid;
    }

    /**
     * Checks for duplicate control points and discontinuous segments in curves. If the curve is a ring, it is also checked
     * whether the ring is closed or not.
     *
     * @param curve
     * @param affectedGeometryParticles
     * @return
     */
    private boolean validateCurve(final Curve curve, final List<Object> affectedGeometryParticles) {

        boolean isValid = true;

        final List<Object> affectedGeometryParticles2 = new ArrayList<>(affectedGeometryParticles);
        affectedGeometryParticles2.add(curve);

        logger.trace("Curve geometry. Testing for duplication of successive control points.");

        int segmentIdx = 0;
        for (CurveSegment segment : curve.getCurveSegments()) {
            if (segment.getSegmentType() == CurveSegmentType.LINE_STRING_SEGMENT) {
                final LineStringSegment lineStringSegment = (LineStringSegment) segment;
                Point lastPoint = null;
                for (Point point : lineStringSegment.getControlPoints()) {
                    if (lastPoint != null) {
                        if (point.equals(lastPoint)) {
                            logger.debug("Found duplicate control points.");
                            if (!fireEvent(new DuplicatePoints(curve, point, affectedGeometryParticles2))) {
                                isValid = false;
                            }
                        }
                    }
                    lastPoint = point;
                }
            } else {
                logger.debug("Non-linear curve segment. Skipping check for duplicate control points.");
            }
            segmentIdx++;
        }

        logger.trace("Curve geometry. Testing segment continuity.");
        Point lastSegmentEndPoint = null;
        segmentIdx = 0;
        for (CurveSegment segment : curve.getCurveSegments()) {

            if (doesNotSupportGettingStartOrEndPoint(segment)) {
                /*
                 * Operations getStartPoint() and getEndPoint() not supported skip this segment; continue check starting with
                 * next supported segment type.
                 */
                lastSegmentEndPoint = null;

            } else {

                Point startPoint = segment.getStartPoint();
                if (lastSegmentEndPoint != null) {
                    if (startPoint.get0() != lastSegmentEndPoint.get0()
                            || startPoint.get1() != lastSegmentEndPoint.get1()) {
                        logger.debug("Found discontinuous segments.");
                        if (!fireEvent(new CurveDiscontinuity(curve, segmentIdx, affectedGeometryParticles2))) {
                            isValid = false;
                        }
                    }
                }

                lastSegmentEndPoint = segment.getEndPoint();
            }
            segmentIdx++;
        }

        if (curve instanceof Ring) {
            logger.trace("Ring geometry. Testing if it's closed. ");
            List<CurveSegment> segments = curve.getCurveSegments();

            if (doesNotSupportGettingStartOrEndPoint(segments.get(0))
                    || doesNotSupportGettingStartOrEndPoint(segments.get(segments.size() - 1))) {
                logger.debug(
                        "Ring with start or end segment not supporting getStartPoint or getEndPoint operation. Skipping check if ring is closed.");
            } else {
                if (!curve.isClosed()) {
                    logger.debug("Not closed.");
                    if (!fireEvent(new RingNotClosed((Ring) curve, affectedGeometryParticles2))) {
                        isValid = false;
                    }
                }
            }
        }

        // CurveSelfIntersection check is ignored by the GMLValidationEventHandler,
        // since self intersection checks are performed separately (by the JTS
        // validation).
        // No need to perform the self intersection test here, in this geometry
        // validator.

        return isValid;
    }

    private boolean doesNotSupportGettingStartOrEndPoint(CurveSegment segment) {

        if (segment instanceof ArcByCenterPoint || segment instanceof BSpline || segment instanceof Clothoid
                || segment instanceof CubicSpline || segment instanceof GeodesicString
                || segment instanceof OffsetCurve) {
            return true;
        } else {
            return false;
        }
    }

    private boolean validateSurface(Surface surface, List<Object> affectedGeometryParticles,
            boolean jtsValidationSucceeded) {
        logger.trace("Surface geometry. Validating individual patches.");
        boolean isValid = true;
        List<Object> affectedGeometryParticles2 = new ArrayList<Object>(affectedGeometryParticles);
        affectedGeometryParticles2.add(surface);

        List<? extends SurfacePatch> patches = surface.getPatches();
        if (patches.size() > 1) {
            logger.debug(
                    "Surface consists of multiple patches, but validation of inter-patch topology is not available yet.");
        }
        for (SurfacePatch patch : surface.getPatches()) {
            if (!(patch instanceof PolygonPatch)) {
                logger.debug("Skipping validation of surface patch -- not a PolygonPatch.");
            } else {
                if (!validatePatch((PolygonPatch) patch, affectedGeometryParticles2, jtsValidationSucceeded)) {
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    private boolean validatePatch(final PolygonPatch patch, final List<Object> affectedGeometryParticles,
            final boolean jtsValidationSucceeded) {

        boolean isValid = true;
        List<Object> affectedGeometryParticles2 = new ArrayList<>(affectedGeometryParticles);
        affectedGeometryParticles2.add(patch);
        logger.trace("Surface patch. Validating rings and spatial ring relations.");

        // validate the curve of the exterior ring
        final Ring exteriorRing = patch.getExteriorRing();
        if (!validateCurve(exteriorRing, affectedGeometryParticles2)) {
            isValid = false;
        }

        // CGAlgorithms.isCCW(..) is only guaranteed to work for valid rings. If a ring
        // is not valid, then the result
        // of isCCW(..) is not reliable.
        // Note that axis orientation plays a role for CGAlgorithms.isCCW(..). It
        // assumes a right-handed coordinate reference system.
        // An explanation for why CGAlgorithms.isCCW(..) would return false (in unit
        // test
        // test_arc_interpolation_self_intersection for test surface with
        // gml:id=DETHL56P00019ALS) when our
        // custom linearizer is used would be that the result of isCCW(..) is wrong
        // because the linearized ring is
        // invalid (due to the self-intersection).
        // We perform an orientation check only if the geometry is valid (based upon the
        // result of the JTS validation).
        // The orientation checks are performed based upon the results of our custom
        // linearization (because with a
        // different linearization the resulting JTS geometry could be invalid).
        // TODO If the ring is given in a left-handed CRS, the ring coordinates need to
        // be transformed. Realizing such a transformation is future work.

        // Only perform the orientation test if JTS validation found out that the
        // geometry is valid
        if (jtsValidationSucceeded) {

            // transform exterior ring to linearized JTS geometry
            final LinearRing exteriorJTSRing = getJTSRing(exteriorRing);

            logger.trace("Surface patch. Validating exterior ring orientation.");

            final boolean isClockwise = !CGAlgorithms.isCCW(exteriorJTSRing.getCoordinates());

            if (!fireEvent(new ExteriorRingOrientation(patch, isClockwise, affectedGeometryParticles2))) {
                isValid = false;
            }
        }

        final List<Ring> interiorRings = patch.getInteriorRings();
        int interiorRingIdx = 0;
        for (Ring interiorRing : interiorRings) {

            // validate curve of interior ring
            if (!validateCurve(interiorRing, affectedGeometryParticles2)) {
                isValid = false;
            }

            // Only perform the orientation test if JTS validation found out that the
            // geometry is valid
            if (jtsValidationSucceeded) {

                // transform interior ring to linearized JTS geometries
                final LinearRing interiorJTSRing = getJTSRing(interiorRing);
                logger.trace("Surface patch. Validating interior ring orientation.");
                boolean isClockwise = !CGAlgorithms.isCCW(interiorJTSRing.getCoordinates());
                if (!fireEvent(new InteriorRingOrientation(patch, interiorRingIdx++, isClockwise,
                        affectedGeometryParticles2))) {
                    isValid = false;
                }
            }
        }

        // Validation events InteriorRingIntersectsExterior,
        // InteriorRingOutsideExterior, InteriorRingsIntersect,
        // InteriorRingsNested, InteriorRingsTouch, and InteriorRingTouchesExterior are
        // ignored by the
        // GMLValidationEventHandler. These events are handled by the JTS validation.
        // Thus, no need to compute such events here.

        return isValid;
    }

    private boolean validate(final CompositeGeometry<?> geom, final List<Object> affectedGeometryParticles,
            boolean jtsValidationSucceeded) {
        logger.debug("Composite geometry found, but validation of inter-primitive topology is not available yet.");
        boolean isValid = true;
        List<Object> affectedGeometryParticles2 = new ArrayList<Object>(affectedGeometryParticles);
        affectedGeometryParticles2.add(geom);
        for (GeometricPrimitive geometricPrimitive : geom) {
            if (!validate(geometricPrimitive, affectedGeometryParticles2, jtsValidationSucceeded)) {
                isValid = false;
            }
        }
        return isValid;
    }

    private boolean validate(final MultiGeometry<?> geom, final List<Object> affectedGeometryParticles,
            boolean jtsValidationSucceeded) {
        logger.debug("MultiGeometry. Validating individual member geometries.");
        boolean isValid = true;
        List<Object> affectedGeometryParticles2 = new ArrayList<Object>(affectedGeometryParticles);
        affectedGeometryParticles2.add(geom);
        for (Geometry member : geom) {
            if (!validateGeometry(member, affectedGeometryParticles2, jtsValidationSucceeded)) {
                isValid = false;
            }
        }
        return isValid;
    }

    private LinearRing getJTSRing(final Ring ring) {
        final IIRing linearizedRing = (IIRing) this.linearizer.linearize(ring, this.crit);
        return linearizedRing.buildJTSGeometry();
    }

    private boolean fireEvent(GeometryValidationEvent event) {
        return this.eventHandler.fireEvent(event);
    }

}
