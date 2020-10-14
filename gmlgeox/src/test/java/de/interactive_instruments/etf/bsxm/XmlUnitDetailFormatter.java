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

import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DefaultComparisonFormatter;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class XmlUnitDetailFormatter extends DefaultComparisonFormatter {

    public String getControlDetailDescription(final Comparison comparison) {
        return getDetailDescription(comparison, comparison.getControlDetails());
    }

    public String getTestDetailDescription(final Comparison comparison) {
        return getDetailDescription(comparison, comparison.getTestDetails());
    }

    private String getDetailDescription(final Comparison difference, final Comparison.Detail detail) {
        final ComparisonType type = difference.getType();
        String description = type.getDescription();
        final String target = getShortString(detail.getTarget(), detail.getXPath(),
                type);

        if (type == ComparisonType.ATTR_NAME_LOOKUP) {
            return new StringBuilder().append(description).append(" ").append(detail.getXPath()).append(" ").append(target)
                    .toString();
        }
        return new StringBuilder().append(description).append(" ").append(getValue(detail.getValue(), type)).append(" ")
                .append(target).toString();
    }
}
