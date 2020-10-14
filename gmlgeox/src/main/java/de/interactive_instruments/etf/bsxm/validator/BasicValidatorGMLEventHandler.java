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
package de.interactive_instruments.etf.bsxm.validator;

import java.util.List;

import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.patches.PolygonPatch;
import org.deegree.geometry.primitive.segments.CurveSegment;
import org.deegree.geometry.validation.GeometryValidationEventHandler;
import org.deegree.geometry.validation.event.*;

/**
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
final class BasicValidatorGMLEventHandler implements GeometryValidationEventHandler {

    private final ValidationResult result;
    private final ElementContext elementContext;
    private boolean ignoreRingRotation;

    public BasicValidatorGMLEventHandler(final ElementContext elementContext, final ValidationResult result,
            final boolean ignoreRingRotation) {
        this.elementContext = elementContext;
        this.result = result;
        this.ignoreRingRotation = ignoreRingRotation;
    }

    @Override
    public boolean fireEvent(final GeometryValidationEvent event) {

        if (event instanceof CurveDiscontinuity) {

            return curveDiscontinuity((CurveDiscontinuity) event);
        } else if (event instanceof RingNotClosed) {

            return ringNotClosed((RingNotClosed) event);
        } else if (event instanceof CurveSelfIntersection) {
            // Currently handled by separate JTS validation
            return true;
            // return curveSelfIntersection((CurveSelfIntersection) event);
        } else if (event instanceof DuplicatePoints) {
            // Ignore.
            return true;
        } else if (event instanceof InteriorRingIntersectsExterior) {
            // Currently handled by separate JTS validation
            return true;
            // return interiorRingIntersectsExterior((InteriorRingIntersectsExterior) event);
        } else if (event instanceof InteriorRingOutsideExterior) {
            // Currently handled by separate JTS validation
            return true;
            // return interiorRingOutsideExterior((InteriorRingOutsideExterior) event);
        } else if (event instanceof InteriorRingsIntersect) {

            // Currently handled by separate JTS validation
            return true;
            // return interiorRingsIntersect((InteriorRingsIntersect) event);
        } else if (event instanceof InteriorRingsNested) {
            // Currently handled by separate JTS validation
            return true;
            // return interiorRingsWithin((InteriorRingsNested) event);
        } else if (event instanceof InteriorRingsTouch) {

            // Currently handled by separate JTS validation
            return true;
            // return interiorRingsTouch( (InteriorRingsTouch) event);
        } else if (event instanceof InteriorRingTouchesExterior) {

            // Currently handled by separate JTS validation
            return true;
            // return interiorRingTouchesExterior( (InteriorRingTouchesExterior) event);
        } else if (event instanceof ExteriorRingOrientation) {
            if (ignoreRingRotation) {
                return true;
            }

            return exteriorRingOrientation((ExteriorRingOrientation) event);
        } else if (event instanceof InteriorRingOrientation) {
            if (ignoreRingRotation) {
                return true;
            }
            return interiorRingOrientation((InteriorRingOrientation) event);
        } else {
            throw new IllegalStateException("Unknown event: " + event.getClass().getName());
        }
    }

    private boolean curveDiscontinuity(final CurveDiscontinuity evt) {
        final Curve curve = evt.getCurve();
        final int segmentIdx = evt.getEndPointSegmentIndex();

        final Point endPoint = curve.getCurveSegments().get(segmentIdx - 1).getEndPoint();
        final Point startPoint = curve.getCurveSegments().get(segmentIdx).getStartPoint();

        result.addError(elementContext, Message.translate("gmlgeox.validation.geometry.curvediscontinuity",
                Message.formatPoint(startPoint),
                segmentIdx + 1,
                Message.formatPoint(endPoint)), startPoint);
        return false;
    }

    private boolean exteriorRingOrientation(final ExteriorRingOrientation evt) {
        if (evt.isClockwise()) {
            final PolygonPatch patch = evt.getPatch();
            result.addError(elementContext, Message.translate(
                    "gmlgeox.validation.geometry.exteriorRingCW"), patch.getExteriorRing());
            return false;
        }
        return true;
    }

    private boolean interiorRingOrientation(final InteriorRingOrientation evt) {
        if (evt.isClockwise()) {
            return true;
        }
        evt.getGeometryParticleHierarchy();
        final PolygonPatch patch = evt.getPatch();
        final int ringIdx = evt.getRingIdx();
        result.addError(elementContext, Message.translate(
                "gmlgeox.validation.geometry.interiorRingCCW", ringIdx + 1), patch.getInteriorRings().get(ringIdx));
        return false;
    }

    private boolean ringNotClosed(final RingNotClosed evt) {
        final List<CurveSegment> curveSegments = evt.getRing().getCurveSegments();
        final Point startPoint = curveSegments.get(0).getStartPoint();
        final Point endPoint = curveSegments.get(curveSegments.size() - 1).getEndPoint();
        result.addError(elementContext,
                Message.translate("gmlgeox.validation.geometry.ringnotclosed",
                        Message.formatPoint(startPoint), Message.formatPoint(endPoint)),
                evt.getRing());

        return false;
    }
}
