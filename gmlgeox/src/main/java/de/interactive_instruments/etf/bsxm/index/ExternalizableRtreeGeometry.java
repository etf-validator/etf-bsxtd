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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;

/**
 * ExternalizableRtreeGeometry
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
interface ExternalizableRtreeGeometry extends Externalizable {

    class ExternalizableRtree2DPoint implements ExternalizableRtreeGeometry {

        private double x;
        private double y;

        @Override
        public Point toRtreeGeometry() {
            return Geometries.point(x, y);
        }

        ExternalizableRtree2DPoint() {}

        ExternalizableRtree2DPoint(final Point point) {
            this.x = point.x();
            this.y = point.y();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeDouble(x);
            out.writeDouble(y);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            x = in.readDouble();
            y = in.readDouble();
        }
    }

    class ExternalizableRtreeRectangle implements ExternalizableRtreeGeometry {

        private double x1;
        private double y1;
        private double x2;
        private double y2;

        public ExternalizableRtreeRectangle() {}

        ExternalizableRtreeRectangle(final Rectangle geometry) {
            x1 = geometry.x1();
            y1 = geometry.y1();
            x2 = geometry.x2();
            y2 = geometry.y2();
        }

        @Override
        public Rectangle toRtreeGeometry() {
            return Geometries.rectangle(x1, y1, x2, y2);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeDouble(x1);
            out.writeDouble(y1);
            out.writeDouble(x2);
            out.writeDouble(y2);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            x1 = in.readDouble();
            y1 = in.readDouble();
            x2 = in.readDouble();
            y2 = in.readDouble();
        }
    }

    Geometry toRtreeGeometry();

    static ExternalizableRtreeGeometry create(final Geometry geometry) {
        if (geometry instanceof Rectangle) {
            return new ExternalizableRtreeRectangle((Rectangle) geometry);
        } else {
            return new ExternalizableRtree2DPoint((Point) geometry);
        }
    }

}
