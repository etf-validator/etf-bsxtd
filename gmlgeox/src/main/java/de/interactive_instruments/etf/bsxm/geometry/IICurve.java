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

import com.vividsolutions.jts.geom.Coordinate;

import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.precision.PrecisionModel;
import org.deegree.geometry.primitive.segments.Arc;
import org.deegree.geometry.primitive.segments.ArcString;
import org.deegree.geometry.primitive.segments.CurveSegment;
import org.deegree.geometry.primitive.segments.LineStringSegment;
import org.deegree.geometry.standard.primitive.DefaultCurve;

/**
 * Based on the implementation of {@link DefaultCurve}.
 */
public class IICurve extends DefaultCurve {

    private final IIGeometryFactory geomFactory;
    private final CustomCurveLinearizer linearizer;

    /**
     * Creates a new {@link DefaultCurve} instance from the given parameters.
     *
     * @param id
     *            identifier, may be null
     * @param crs
     *            coordinate reference system, may be null
     * @param pm
     *            precision model, may be null
     * @param segments
     *            segments that belong to the curve
     * @param fac
     *            geometry factory, primarily used to determine the max error criterion when linearizing curve segments
     */
    IICurve(final String id, final ICRS crs, final PrecisionModel pm,
            final List<CurveSegment> segments, final IIGeometryFactory fac) {
        super(id, crs, pm, segments);
        this.geomFactory = fac;
        this.linearizer = new CustomCurveLinearizer(geomFactory, 0.0);
    }

    @Override
    public com.vividsolutions.jts.geom.LineString buildJTSGeometry() {
        final List<CurveSegment> segments = getCurveSegments();
        final int segmentsSize = segments.size();
        final ArrayList<Coordinate[]> coordinates = new ArrayList<>(segmentsSize);
        coordinates.add(linearize(segments.get(0)).getControlPoints().toCoordinateArray());
        int arrSize = coordinates.get(0).length;
        for (int i = 1; i < segmentsSize; i++) {
            coordinates.add(linearize(segments.get(i)).getControlPoints().toCoordinateArray());
            arrSize += coordinates.get(i).length - 1;
        }
        final Coordinate[] coordArray = new Coordinate[arrSize];
        System.arraycopy(coordinates.get(0), 0, coordArray, 0, coordinates.get(0).length);
        for (int i = 1, previousSize = coordinates.get(0).length; i < coordinates.size(); i++) {
            System.arraycopy(coordinates.get(i), 1, coordArray, previousSize, coordinates.get(i).length - 1);
            previousSize += coordinates.get(i).length - 1;
        }
        return jtsFactory.createLineString(coordArray);
    }

    private LineStringSegment linearize(final CurveSegment segment) {
        switch (segment.getSegmentType()) {
        case LINE_STRING_SEGMENT: {
            return (LineStringSegment) segment;
        }
        case ARC:
        case CIRCLE: {
            return linearizer.linearizeArc((Arc) segment, geomFactory.getMaxErrorCriterion());
        }
        case ARC_STRING: {
            return linearizer.linearizeArcString((ArcString) segment, geomFactory.getMaxErrorCriterion());
        }
        default:
            throw new IllegalArgumentException("Cannot determine control points for curve, contains non-linear segments.");
        }
    }

    @Override
    public boolean equals(final Geometry geometry) {
        if (geometry instanceof IICurve) {
            final List<CurveSegment> segments = this.getCurveSegments();
            final List<CurveSegment> otherSegments = ((IICurve) geometry)
                    .getCurveSegments();
            if (segments.size() == 1 && otherSegments.size() == 1) {
                final CurveSegment curve = segments.get(0);
                final CurveSegment otherCurve = otherSegments.get(0);
                if (curve instanceof ArcString
                        && otherCurve instanceof ArcString) {
                    final Points controlPoints = ((ArcString) curve)
                            .getControlPoints();
                    final Points otherControlPoints = ((ArcString) otherCurve)
                            .getControlPoints();
                    return (controlPoints.get(1)
                            .equals(otherControlPoints.get(1))
                            && ((controlPoints.get(0)
                                    .equals(otherControlPoints.get(0))
                                    && controlPoints.get(2)
                                            .equals(otherControlPoints.get(2))))
                            || (controlPoints.get(2)
                                    .equals(otherControlPoints.get(0))
                                    && controlPoints.get(2).equals(
                                            otherControlPoints.get(0))));
                }
            }

        }
        return super.equals(geometry);
    }
}
