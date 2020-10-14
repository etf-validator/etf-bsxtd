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
package de.interactive_instruments.etf.testdriver.bsx.transformers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.model.capabilities.TestObjectType;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final public class ForwardingTransformerFactory {

    private static ForwardingTransformerFactory instance = new ForwardingTransformerFactory();
    private final Map<String, TransformerFactory> transformerFactories;
    private final TransformerFactory defaultTransformerFactory = new XmlTransformerFactory();

    private ForwardingTransformerFactory() {
        final HashMap<String, TransformerFactory> factories = new HashMap<>();
        factories.put("e1d4a306-7a78-4a3b-ae2d-cf5f0810853e", defaultTransformerFactory);
        transformerFactories = Collections.unmodifiableMap(factories);
    }

    public static ForwardingTransformerFactory getInstance() {
        return instance;
    }

    public Transformer create(final TestObjectType testObjectType, final IFile attachmentDir) throws IOException {
        if (testObjectType != null) {
            TestObjectType currentTestObjectType = testObjectType;
            do {
                final TransformerFactory transformer = transformerFactories.get(currentTestObjectType.getId().getId());
                if (transformer != null) {
                    return transformer.create(testObjectType,
                            attachmentDir.secureExpandPathDown("transformed").ensureDir());
                }
                currentTestObjectType = currentTestObjectType.getParent();
            } while (currentTestObjectType != null);
        }
        return defaultTransformerFactory.create(testObjectType, attachmentDir);
    }
}
