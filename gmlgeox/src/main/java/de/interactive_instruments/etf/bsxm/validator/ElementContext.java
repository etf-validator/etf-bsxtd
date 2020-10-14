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

import org.basex.query.value.node.FElem;
import org.deegree.geometry.Geometry;
import org.deegree.gml.GMLVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import de.interactive_instruments.etf.bsxm.DeegreeTransformer;
import de.interactive_instruments.etf.bsxm.GmlGeoXException;
import de.interactive_instruments.etf.bsxm.JtsTransformer;
import de.interactive_instruments.etf.bsxm.parser.GmlId;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class ElementContext {

    final Element element;
    final GMLVersion gmlVersion;
    final org.deegree.geometry.Geometry deegreeGeom;
    final JtsTransformer jtsTransformer;
    final DeegreeTransformer deegreeTransformer;
    private com.vividsolutions.jts.geom.Geometry jtsGeom;
    private boolean jtsTransformationExecuted = false;
    private static byte[] CONTEXT_ELEMENT_NAME = "context".getBytes();
    private static byte[] ID_NAME = "ID".getBytes();

    @Contract(pure = true)
    ElementContext(final Element element,
            final GMLVersion gmlVersion,
            final Geometry deegreeGeom,
            final JtsTransformer jtsTransformer,
            final DeegreeTransformer deegreeTransformer) {
        this.element = element;
        this.gmlVersion = gmlVersion;
        this.deegreeGeom = deegreeGeom;
        this.jtsTransformer = jtsTransformer;
        this.deegreeTransformer = deegreeTransformer;
    }

    @Nullable
    com.vividsolutions.jts.geom.Geometry getJtsGeometry(final ValidationResult result) {
        if (jtsGeom != null) {
            return jtsGeom;
        }
        synchronized (this) {
            if (jtsTransformationExecuted) {
                // second try after waiting
                if (jtsGeom != null) {
                    return jtsGeom;
                }
                return null;
            }
            jtsTransformationExecuted = true;
            try {
                jtsGeom = jtsTransformer.toJTSGeometry(deegreeGeom);
            } catch (final GmlGeoXException | IllegalArgumentException e) {
                if (e.getMessage() != null
                        && e.getMessage().startsWith("Cannot determine control points for curve, contains")) {
                    result.addError(this, Message.translate("gmlgeox.validation.geometry.non.linear.segment"), deegreeGeom);
                } else {
                    result.failWith(e);
                }
            }
            return jtsGeom;
        }
    }

    @NotNull
    @Contract(" -> new")
    FElem[] getContextLocation() {
        final String id = GmlId.getId(element);
        final FElem[] fElems;
        if (id != null) {
            fElems = new FElem[2];
            fElems[0] = ValidationReport.argument(ID_NAME, id.getBytes());
        } else {
            fElems = new FElem[1];
        }
        fElems[fElems.length - 1] = ValidationReport.argument(CONTEXT_ELEMENT_NAME, element.getLocalName().getBytes());
        return fElems;
    }
}
