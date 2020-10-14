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

import static de.interactive_instruments.etf.bsxm.IIGmlConstants.isGML31Namespace;
import static de.interactive_instruments.etf.bsxm.IIGmlConstants.isGML32Namespace;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import javax.xml.namespace.QName;

import org.deegree.commons.xml.XMLParsingException;
import org.deegree.commons.xml.stax.XMLStreamReaderWrapper;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.geometry.Geometry;
import org.deegree.gml.GMLInputFactory;
import org.deegree.gml.GMLStreamReader;
import org.deegree.gml.GMLVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import de.interactive_instruments.etf.bsxm.DeegreeTransformer;
import de.interactive_instruments.etf.bsxm.JtsTransformer;
import de.interactive_instruments.etf.bsxm.parser.BxElementHandler;
import de.interactive_instruments.etf.bsxm.parser.BxReader;
import de.interactive_instruments.etf.bsxm.parser.GmlId;

/**
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class DispatchingValidationHandler implements BxElementHandler {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DispatchingValidationHandler.class);

    private final JtsTransformer jtsTransformer;

    private final Set<QName> gmlGeometries = new HashSet<>();
    private final List<Validator> validators;
    private final ValidationReport report;
    private final ICRS defaultCrs;
    private final DeegreeTransformer deegreeTransformer;

    DispatchingValidationHandler(
            final ValidationReport report,
            final Set<String> gmlGeometryNames,
            final List<Validator> validators,
            final ICRS defaultCrs, final JtsTransformer jtsTransformer, final DeegreeTransformer deegreeTransformer) {
        this.report = report;
        this.validators = validators;
        this.defaultCrs = defaultCrs;
        this.jtsTransformer = jtsTransformer;
        this.deegreeTransformer = deegreeTransformer;
        for (final String gmlGeometryName : gmlGeometryNames) {
            gmlGeometries.add(new QName(gmlGeometryName));
        }
    }

    @Override
    public Set<QName> elementsToRegister() {
        return gmlGeometries;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When this is called on a new feature member, the geometry counters are reset.
     * </p>
     */
    @Override
    public BxElementHandler.ElementVisitResult onStart(final Element element, final BxReader reader) {
        return ElementVisitResult.SKIP_SUBTREE;
    }

    /** {@inheritDoc} */
    @Override
    public void onEnd(final Element element, final BxReader reader) {
        validate(element, reader);
    }

    private void validate(final Element element, final BxReader reader) {
        final String namespaceURI = element.getNamespaceURI();

        final GMLVersion gmlVersion;
        if (isGML32Namespace(namespaceURI)) {
            // GML 3.2
            gmlVersion = GMLVersion.GML_32;
        } else if (isGML31Namespace(namespaceURI)) {
            gmlVersion = GMLVersion.GML_31;
        } else {
            // Throw an exception as this is not a local proble
            report.addFatalError(Message.translate("gmlgeox.validation.geometry.gml.unknown-version"));
            return;
        }

        try {
            final GMLStreamReader gmlStream = GMLInputFactory.createGMLStreamReader(
                    gmlVersion, new XMLStreamReaderWrapper(reader.createSubStreamReader(element), reader.getSystemId()));
            gmlStream.setGeometryFactory(deegreeTransformer.getGeometryFactory());
            gmlStream.setDefaultCRS(defaultCrs);
            final Geometry geom = gmlStream.readGeometry();

            final ElementContext elementContext = new ElementContext(element, gmlVersion, geom, jtsTransformer,
                    deegreeTransformer);

            final ValidationResult[] results = new ValidationResult[this.validators.size()];
            IntStream.range(0, this.validators.size()).forEach(i -> {
                results[i] = new ValidationResult();
                validators.get(i).validate(elementContext, results[i]);
            });
            for (int i = 0; i < this.validators.size(); i++) {
                report.addAllMessages(validators.get(i), results[i]);
            }
        } catch (final XMLParsingException e) {
            LOGGER.trace("Error parsing XML ", e);
            report.addFatalError(Message.translate("gmlgeox.validation.parsing.xml",
                    element.getLocalName(), GmlId.getId(element), e.getMessage()));
        } catch (final Exception e) {
            LOGGER.trace("Unexpected exception during XML parsing", e);
            report.addFatalError(Message.translate("gmlgeox.validation.parsing.unexpected", e.getMessage()));
        }
    }

}
