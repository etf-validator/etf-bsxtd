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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * ExternalizableJtsMultiPolygon
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class ExternalizableJtsMultiPolygon implements ExternalizableJtsGeometry {

    private ExternalizableJtsPolygon[] polygons;

    public ExternalizableJtsMultiPolygon() {}

    ExternalizableJtsMultiPolygon(final MultiPolygon geometry) {
        polygons = new ExternalizableJtsPolygon[geometry.getNumGeometries()];
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            polygons[i] = new ExternalizableJtsPolygon((Polygon) geometry.getGeometryN(i));
        }
    }

    @Override
    public Geometry toJtsGeometry(final GeometryFactory factory) {
        final Polygon[] ps = new Polygon[polygons.length];
        for (int i = 0; i < ps.length; i++) {
            ps[i] = (Polygon) polygons[i].toJtsGeometry(factory);
        }
        return new MultiPolygon(ps, factory);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(polygons.length);
        for (int i = 0; i < polygons.length; i++) {
            out.writeObject(polygons[i]);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        polygons = new ExternalizableJtsPolygon[in.readInt()];
        for (int i = 0; i < polygons.length; i++) {
            polygons[i] = (ExternalizableJtsPolygon) in.readObject();
        }
    }
}
