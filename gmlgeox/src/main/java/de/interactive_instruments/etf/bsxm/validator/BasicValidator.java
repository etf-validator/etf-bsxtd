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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jts.operation.valid.TopologyValidationError;

import org.deegree.gml.GMLVersion;

import de.interactive_instruments.etf.bsxm.geometry.IIGeometryValidator;

/**
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
final class BasicValidator implements Validator {

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void validate(final ElementContext elementContext, final ValidationResult result) {
        final Geometry jtsGeom = elementContext.getJtsGeometry(result);
        boolean jtsValid = false;
        if (jtsGeom == null) {
            result.failSilently();
        } else {
            try {
                final IsValidOp ivo = new IsValidOp(jtsGeom);
                final TopologyValidationError topError = ivo.getValidationError();
                if (topError != null) {
                    // Optimization: ivo.isValid() is the same as topError==null. Otherwise
                    // the validation is performed twice in case of geometry problems.
                    result.addError(elementContext,
                            Message.translate("gmlgeox.validation.geometry.jts." + topError.getErrorType()), jtsGeom,
                            topError.getCoordinate());
                } else {
                    jtsValid = true;
                }
            } catch (final IllegalArgumentException e) {
                result.addError(elementContext,
                        Message.translate("gmlgeox.validation.geometry.unsupported", e.getMessage()));
            }
        }

        final BasicValidatorGMLEventHandler eventHandler = new BasicValidatorGMLEventHandler(elementContext, result,
                elementContext.gmlVersion == GMLVersion.GML_31);
        final IIGeometryValidator validator = new IIGeometryValidator(eventHandler,
                elementContext.deegreeTransformer.getGeometryFactory());
        // Deegree3 based validation. Errors are collected through the BasicValidatorGMLEventHandler
        validator.validateGeometry(elementContext.deegreeGeom, jtsValid);
    }
}
