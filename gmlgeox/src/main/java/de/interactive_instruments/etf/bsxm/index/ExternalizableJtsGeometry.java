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
package de.interactive_instruments.etf.bsxm.index;

import java.io.Externalizable;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * A factory for creating Externalizable JTS geometry wrappers
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
interface ExternalizableJtsGeometry extends Externalizable {

    static Coordinate[] toCoordinateArray(final double[] coordinates) {
        final Coordinate[] coordArray = new Coordinate[coordinates.length / 3];
        for (int i = 0, j = 0; i < coordArray.length; i++, j += 3) {
            coordArray[i] = new Coordinate(coordinates[j], coordinates[j + 1], coordinates[j + 2]);
        }
        return coordArray;
    }

    static LinearRing toLinearRing(final double[] coordinates, final GeometryFactory factory) {
        return new LinearRing(new CoordinateArraySequence(toCoordinateArray(coordinates)), factory);
    }

    static double[] simplify(final CoordinateSequence coordinateSequence) {
        final Coordinate[] coordArray = coordinateSequence.toCoordinateArray();
        final double[] simplified = new double[coordArray.length * 3];
        for (int i = 0, j = 0; i < simplified.length; i += 3, j++) {
            simplified[i] = coordArray[j].x;
            simplified[i + 1] = coordArray[j].y;
            simplified[i + 2] = coordArray[j].z;
        }
        return simplified;
    }

    Geometry toJtsGeometry(final GeometryFactory factory);

    static ExternalizableJtsGeometry create(final Geometry geometry) {
        if (geometry instanceof LineString) {
            return new ExternalizableJtsLineString((LineString) geometry);
        } else if (geometry instanceof Polygon) {
            return new ExternalizableJtsPolygon((Polygon) geometry);
        } else if (geometry instanceof MultiPolygon) {
            return new ExternalizableJtsMultiPolygon((MultiPolygon) geometry);
        } else if (geometry instanceof Point) {
            return new ExternalizableJtsPoint((Point) geometry);
        } else if (geometry instanceof MultiLineString) {
            return new ExternalizableJtsMultiLineString((MultiLineString) geometry);
        } else if (geometry instanceof MultiPoint) {
            return new ExternalizableJtsMultiPoint((MultiPoint) geometry);
        }

        throw new IllegalStateException("Unknown JTS type for serialization: " + geometry.getGeometryType());
    }
}
