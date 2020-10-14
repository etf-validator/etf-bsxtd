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

import org.deegree.geometry.Geometry;
import org.deegree.geometry.composite.CompositeGeometry;
import org.deegree.geometry.multi.MultiGeometry;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.Ring;
import org.deegree.geometry.primitive.Surface;
import org.deegree.geometry.primitive.patches.PolygonPatch;
import org.deegree.geometry.primitive.patches.SurfacePatch;
import org.deegree.geometry.primitive.segments.*;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class RepetitionInCurveSegmentsValidator implements Validator {

    @Override
    public int getId() {
        return 2;
    }

    @Override
    public void validate(final ElementContext elementContext, final ValidationResult result) {
        checkNoRepetitionInCurveSegment(elementContext, result, elementContext.deegreeGeom);
    }

    /**
     * Checks that two consecutive points in a posList - within a CurveSegment - are not equal.
     * <p>
     * Checks:
     * <ul>
     * <li>Curve (including CompositeCurve, LineString, OrientableCurve, Ring)</li>
     * <li>The following CurveSegment types: Arc, ArcString, CubicSpline, GeodesicString, LineStringSegment</li>
     * <li>The exterior and interior rings of polygon patches (contained within Surface, Polygon, PolyhedralSurface,
     * TriangulatedSurface, Tin, CompositeSurface, or OrientableSurface) - NOTE: all other types of surface patches are
     * currently ignored!</li>
     * <li>The elements of multi and composite geometries</li>
     * </ul>
     * Does NOT check curve segments within solids!
     *
     * @param geom
     *            the geometry that shall be tested
     * @return <code>true</code> if no repetition was detected (or if the geometry is a point, a solid, or consists of
     *         solids), else <code>false</code>
     */
    private boolean checkNoRepetitionInCurveSegment(final ElementContext elementContext, final ValidationResult result,
            final @NotNull Geometry geom) {

        if (geom instanceof Curve) {

            // includes CompositeCurve, LineString, OrientableCurve, Ring
            final Curve curve = (Curve) geom;
            final List<CurveSegment> segments = curve.getCurveSegments();
            for (int segmentIdx = 0; segmentIdx < segments
                    .size(); segmentIdx++) {

                final CurveSegment segment = segments.get(segmentIdx);
                final Points points;
                if (segment instanceof ArcString) {
                    ArcString as = (ArcString) segment;
                    points = as.getControlPoints();
                } else if (segment instanceof CubicSpline) {
                    CubicSpline cs = (CubicSpline) segment;
                    points = cs.getControlPoints();
                } else if (segment instanceof GeodesicString) {
                    GeodesicString gs = (GeodesicString) segment;
                    points = gs.getControlPoints();
                } else if (segment instanceof LineStringSegment) {
                    LineStringSegment lss = (LineStringSegment) segment;
                    points = lss.getControlPoints();
                } else {
                    // should never happen
                    throw new IllegalArgumentException("Unknown type: " + segment);
                }

                Point lastPoint = null;
                for (Point point : points) {
                    if (lastPoint != null) {
                        if (point.equals(lastPoint)) {
                            result.addError(elementContext, Message.translate(
                                    "gmlgeox.validation.geometry.repetitionincurvesegment",
                                    segmentIdx + 1), point);
                            return false;
                        }
                    }
                    lastPoint = point;
                }
            }
            return true;
        } else if (geom instanceof Surface) {
            final Surface s = (Surface) geom;
            final List<? extends SurfacePatch> patches = s.getPatches();
            for (SurfacePatch sp : patches) {
                if (sp instanceof PolygonPatch) {
                    final PolygonPatch pp = (PolygonPatch) sp;
                    final Ring exterior = pp.getExteriorRing();
                    if (!checkNoRepetitionInCurveSegment(elementContext, result, exterior)) {
                        return false;
                    }
                    final List<Ring> interiorRings = pp.getInteriorRings();
                    for (Ring interiorRing : interiorRings) {
                        if (!checkNoRepetitionInCurveSegment(elementContext, result, interiorRing)) {
                            return false;
                        }
                    }
                }
                // TODO is another type of surface patch relevant?
            }
            return true;
        } else if (geom instanceof MultiGeometry
                || geom instanceof CompositeGeometry) {
            @SuppressWarnings("rawtypes")
            final List l = (List) geom;
            for (Object o : l) {
                Geometry g = (Geometry) o;
                if (!checkNoRepetitionInCurveSegment(elementContext, result, g)) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }
}
