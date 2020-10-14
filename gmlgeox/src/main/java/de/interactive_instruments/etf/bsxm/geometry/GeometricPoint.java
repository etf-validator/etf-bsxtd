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

import java.util.Objects;

import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.standard.primitive.DefaultPoint;

public final class GeometricPoint extends DefaultPoint {

    private final double x;
    private final double y;

    public GeometricPoint(Point point) {
        super(point.getId(), point.getCoordinateSystem(), point.getPrecision(), point.getAsArray());
        x = point.get0();
        y = point.get1();
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public boolean equals(GeometricPoint p) {
        return p.get0() == x && p.get1() == y;
    }

    public static String toPosListString(com.github.davidmoten.rtree.geometry.Point p) {
        return p.x() + " " + p.y();
    }

}
