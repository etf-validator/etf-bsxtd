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
import org.deegree.geometry.points.Points;
import org.deegree.geometry.precision.PrecisionModel;
import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.segments.CurveSegment;
import org.deegree.geometry.primitive.segments.LineStringSegment;
import org.deegree.geometry.standard.primitive.DefaultCurve;
import org.deegree.geometry.standard.primitive.DefaultRing;

/**
 * Based on the implementation of {@link DefaultRing}.
 */
public class IIRing extends DefaultRing {

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
     * @param members
     *            the <code>Curve</code>s that compose the <code>Ring</code>
     * @param fac
     *            geometry factory, primarily used to determine the max error criterion when linearizing curve segments
     */
    IIRing(final String id, final ICRS crs, final PrecisionModel pm, final List<Curve> members, final IIGeometryFactory fac) {
        super(id, crs, pm, members);
        this.geomFactory = fac;
        this.linearizer = new CustomCurveLinearizer(geomFactory, 0.0);
    }

    @Override
    public com.vividsolutions.jts.geom.LinearRing buildJTSGeometry() {
        final List<Coordinate> coords = new ArrayList<>();
        for (CurveSegment segment : segments) {
            final LineStringSegment lsSegment = linearizer.linearize(segment, geomFactory.getMaxErrorCriterion());
            coords.addAll(getCoordinates(lsSegment));
        }
        return jtsFactory.createLinearRing(coords.toArray(new Coordinate[0]));
    }

    private static List<Coordinate> getCoordinates(final LineStringSegment lsSegment) {
        final Points points = lsSegment.getControlPoints();
        final List<Coordinate> coordinates = new ArrayList<>(points.size());
        for (Point point : points) {
            coordinates.add(new Coordinate(point.get0(), point.get1(), point.get2()));
        }
        return coordinates;
    }
}
