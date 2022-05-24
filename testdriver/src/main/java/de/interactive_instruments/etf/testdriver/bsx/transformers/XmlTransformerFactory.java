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

import java.util.ArrayList;
import java.util.Collection;

import org.basex.core.cmd.Set;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.model.capabilities.TestObjectType;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class XmlTransformerFactory implements TransformerFactory {

    private final Collection<Set> parameters = new ArrayList<Set>() {
        {
            add(new Set("PARSER", "xml"));
        }
    };

    private static class XmlTransformer implements Transformer {

        private final Collection<Set> parameters;

        private XmlTransformer(final Collection<Set> parameters) {
            this.parameters = parameters;
        }

        @Override
        public PreparedFileCollection transform(final IFile file) {
            return new DefaultPreparedFileCollection(parameters, file);
        }
    }

    public Transformer create(final TestObjectType testObjectType, final IFile attachmentDir) {
        return new XmlTransformer(parameters);
    }
}
