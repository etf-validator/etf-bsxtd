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
package de.interactive_instruments.etf.bsxm;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public enum SpatialRelOp {
    CONTAINS {
        @Override
        public boolean call(final com.vividsolutions.jts.geom.Geometry g1,
                final com.vividsolutions.jts.geom.Geometry g2) {
            return g1.contains(g2);
        }
    },
    CROSSES {
        @Override
        public boolean call(final com.vividsolutions.jts.geom.Geometry g1,
                final com.vividsolutions.jts.geom.Geometry g2) {
            return g1.crosses(g2);
        }
    },
    EQUALS {
        @Override
        public boolean call(final com.vividsolutions.jts.geom.Geometry g1,
                final com.vividsolutions.jts.geom.Geometry g2) {
            return g1.equals(g2);
        }
    },
    INTERSECTS {
        @Override
        public boolean call(final com.vividsolutions.jts.geom.Geometry g1,
                final com.vividsolutions.jts.geom.Geometry g2) {
            return g1.intersects(g2);
        }
    },
    ISDISJOINT {
        @Override
        public boolean call(final com.vividsolutions.jts.geom.Geometry g1,
                final com.vividsolutions.jts.geom.Geometry g2) {
            return g1.disjoint(g2);
        }
    },
    ISWITHIN {
        @Override
        public boolean call(final com.vividsolutions.jts.geom.Geometry g1,
                final com.vividsolutions.jts.geom.Geometry g2) {
            return g1.within(g2);
        }
    },
    OVERLAPS {
        @Override
        public boolean call(final com.vividsolutions.jts.geom.Geometry g1,
                final com.vividsolutions.jts.geom.Geometry g2) {
            return g1.overlaps(g2);
        }
    },
    TOUCHES {
        @Override
        public boolean call(final com.vividsolutions.jts.geom.Geometry g1,
                final com.vividsolutions.jts.geom.Geometry g2) {
            return g1.touches(g2);
        }
    };

    public abstract boolean call(final com.vividsolutions.jts.geom.Geometry g1, final com.vividsolutions.jts.geom.Geometry g2);
}
