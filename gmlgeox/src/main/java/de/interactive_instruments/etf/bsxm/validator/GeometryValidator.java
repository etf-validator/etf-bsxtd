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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.basex.query.value.node.ANode;
import org.basex.query.value.node.FElem;
import org.jetbrains.annotations.NotNull;

import de.interactive_instruments.etf.bsxm.DeegreeTransformer;
import de.interactive_instruments.etf.bsxm.JtsTransformer;
import de.interactive_instruments.etf.bsxm.SrsLookup;
import de.interactive_instruments.etf.bsxm.parser.BxElementReader;
import de.interactive_instruments.etf.bsxm.parser.BxNamespaceHolder;

/**
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public final class GeometryValidator {

    private final static Validator[] preparedValidators = {
            new BasicValidator(),
            new PolygonPatchConnectivityValidator(),
            new RepetitionInCurveSegmentsValidator(),
            new GeometryIsSimpleValidator()
    };
    final static int NO_OF_VALIDATORS = preparedValidators.length;
    public final static byte[] VALIDATE_ALL = new String(new char[NO_OF_VALIDATORS]).replace("\0", "1").getBytes();

    private final TreeSet<String> gmlGeometryElementNames = new TreeSet<>();
    private final SrsLookup srsLookup;
    private final DeegreeTransformer deegreeTransformer;
    private final JtsTransformer jtsTransformer;
    private final BxNamespaceHolder namespaceHolder;

    private List<Validator> validators;

    private byte[] currentTestMask;
    private ValidationReport reportPrototype;

    public GeometryValidator(final SrsLookup srsLookup, final JtsTransformer jtsTransformer,
            final DeegreeTransformer deegreeTransformer, final BxNamespaceHolder namespaceHolder) {
        this.srsLookup = srsLookup;
        this.deegreeTransformer = deegreeTransformer;
        this.jtsTransformer = jtsTransformer;

        // default geometry types for which validation is performed
        registerGmlGeometry("Point");
        registerGmlGeometry("Polygon");
        registerGmlGeometry("Surface");
        registerGmlGeometry("Curve");
        registerGmlGeometry("LinearRing");

        registerGmlGeometry("MultiPoint");
        registerGmlGeometry("MultiPolygon");
        registerGmlGeometry("MultiGeometry");
        registerGmlGeometry("MultiSurface");
        registerGmlGeometry("MultiCurve");

        registerGmlGeometry("Ring");
        registerGmlGeometry("LineString");

        this.namespaceHolder = namespaceHolder;
        this.currentTestMask = null;
    }

    public void registerGmlGeometry(final String gmlGeometry) {
        gmlGeometryElementNames.add(gmlGeometry);
    }

    public void unregisterGmlGeometry(final String gmlGeometry) {
        gmlGeometryElementNames.remove(gmlGeometry);
    }

    public void unregisterAllGmlGeometries() {
        gmlGeometryElementNames.clear();
    }

    public String registeredGmlGeometries() {
        return String.join(", ", gmlGeometryElementNames);
    }

    public FElem validate(final ANode node, final @NotNull byte[] testMask) {
        return this.executeValidate(node, testMask).toBsxElement();
    }

    public String validateWithSimplifiedResults(final ANode node, final @NotNull byte[] testMask) {
        return this.executeValidate(node, testMask).getValidationResult();
    }

    private ValidationReport prepareValidatorsAndReport(final byte[] testMask) {
        if (!Arrays.equals(this.currentTestMask, testMask)) {
            reportPrototype = new ValidationReport();
            this.validators = new ArrayList<>(testMask.length);
            for (int i = 0; i < NO_OF_VALIDATORS; i++) {
                if (testMask.length >= i + 1 && testMask[i] == '1') {
                    this.validators.add(preparedValidators[i]);
                } else {
                    reportPrototype.skipped(i);
                }
            }
            this.currentTestMask = testMask;
        }
        return reportPrototype.creatCopyWithResults();
    }

    private ValidationReport executeValidate(final ANode node, @NotNull final byte[] testMask) {
        final ValidationReport report = prepareValidatorsAndReport(testMask);
        final DispatchingValidationHandler handler = new DispatchingValidationHandler(
                report,
                gmlGeometryElementNames,
                this.validators,
                srsLookup.getSrsForGeometryNode(node),
                this.jtsTransformer,
                this.deegreeTransformer);
        final BxElementReader reader = new BxElementReader(node, handler, namespaceHolder);
        reader.read();
        return report;
    }
}
