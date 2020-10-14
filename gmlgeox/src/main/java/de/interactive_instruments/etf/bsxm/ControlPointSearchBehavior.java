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

import java.util.Optional;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public enum ControlPointSearchBehavior {

    /**
     * Ignores the middle point of arcs, defined as either ArcString or Arc.
     */
    IGNORE_ARC_MID_POINT("IGNORE_ARC_MID_POINT"),
    /**
     * Ignores the middle point of arcs, defined as either ArcString or Arc.
     */
    IGNORE_NON_LINEAR_SEGMENTS("IGNORE_NON_LINEAR_SEGMENTS");

    private String name;

    ControlPointSearchBehavior(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * @param name
     *            The name of the search behavior enum to retrieve
     * @return the enum whose name is equal to, ignoring case, the given name; can be empty (if the name does not match one
     *         of the defined enum names) but not <code>null</code>
     */
    public static Optional<ControlPointSearchBehavior> fromString(String name) {

        for (ControlPointSearchBehavior v : values()) {
            if (v.getName().equalsIgnoreCase(name)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }
}
