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

import static de.interactive_instruments.etf.bsxm.validator.GeometryValidator.NO_OF_VALIDATORS;

import java.util.ArrayList;
import java.util.Collection;

import org.basex.query.util.list.ANodeList;
import org.basex.query.value.item.QNm;
import org.basex.query.value.node.FAttr;
import org.basex.query.value.node.FElem;
import org.basex.query.value.node.FTxt;
import org.basex.util.Token;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class ValidationReport {

    // S - skipped, F - failed, V - valid
    private final byte[] testResults;

    public static final byte[] GGEO_NS = "https://modules.etf-validator.net/gmlgeox/2".getBytes();
    public static final byte[] GGEO_PREFIX = "ggeo".getBytes();
    private static final QNm VALIDATION_RESULT_QNM = new QNm(GGEO_PREFIX, "ValidationResult", GGEO_NS);
    private static final QNm VALID_QNM = new QNm(GGEO_PREFIX, "valid", GGEO_NS);
    private static final QNm RESULT_QNM = new QNm(GGEO_PREFIX, "result", GGEO_NS);
    static final QNm ERROR_QNM = new QNm(GGEO_PREFIX, "errors", GGEO_NS);

    public static final byte[] NS_ETF = "http://www.interactive-instruments.de/etf/2.0".getBytes();
    public static final byte[] ETF_PREFIX = "etf".getBytes();
    private static final QNm ARGUMENT_QNM = new QNm(ETF_PREFIX, "argument", NS_ETF);
    private static final QNm TOKEN_QNM = new QNm("token", NS_ETF);

    private Collection<FElem> validatorMessages;

    @Contract(pure = true)
    ValidationReport() {
        this.testResults = new byte[]{'V', 'V', 'V', 'V'};
    }

    private ValidationReport(@NotNull final ValidationReport report) {
        this.testResults = new byte[NO_OF_VALIDATORS];
        System.arraycopy(report.testResults, 0, this.testResults, 0, NO_OF_VALIDATORS);
    }

    // only copy skipped
    @NotNull
    @Contract(" -> new")
    ValidationReport creatCopyWithResults() {
        return new ValidationReport(this);
    }

    /**
     * @return the validationResult
     */
    @NotNull
    @Contract(pure = true)
    public String getValidationResult() {
        return Token.string(testResults);
    }

    void skipped(final int validatorId) {
        testResults[validatorId] = 'S';
    }

    @NotNull
    @Contract(" -> new")
    private FTxt isValidAsBytes() {
        for (int i = 0; i < testResults.length; i++) {
            if (testResults[i] == 'F') {
                return new FTxt("false".getBytes());
            }
        }
        return new FTxt("true".getBytes());
    }

    void addFatalError(@NotNull final Message message) {
        for (int i = 0; i < testResults.length; i++) {
            if (testResults[i] != 'S') {
                testResults[i] = 'F';
            }
        }
        final ANodeList children = message.toNodeList(null, null);
        final FElem errorElement = new FElem(ERROR_QNM, null, children, null);

        if (validatorMessages == null) {
            validatorMessages = new ArrayList<>(1);
        }
        validatorMessages.add(errorElement);
    }

    static FElem argument(final byte[] key, final byte[] value) {
        final ANodeList tokenAttribute = new ANodeList(1).add(
                new FAttr(TOKEN_QNM, key));
        final ANodeList argumentText = new ANodeList(1).add(
                new FTxt(Token.token(value)));
        return new FElem(ARGUMENT_QNM, null,
                argumentText,
                tokenAttribute);
    }

    FElem toBsxElement() {
        final FElem root = new FElem(VALIDATION_RESULT_QNM);
        final FElem valid = new FElem(VALID_QNM);
        valid.add(isValidAsBytes());
        root.add(valid);
        final FElem result = new FElem(RESULT_QNM);
        result.add(new FTxt(this.testResults));
        root.add(result);
        if (validatorMessages != null) {
            for (final FElem message : validatorMessages) {
                root.add(message);
            }
        }
        return root;
    }

    void addAllMessages(final Validator validator, final ValidationResult validationResult) {
        if (testResults[validator.getId()] == 'V') {
            testResults[validator.getId()] = validationResult.getResult();
        }
        if (validationResult.getMessages() != null && !validationResult.getMessages().isEmpty()) {
            if (this.validatorMessages == null) {
                this.validatorMessages = validationResult.getMessages();
            } else {
                this.validatorMessages.addAll(validationResult.getMessages());
            }
        }
    }
}
