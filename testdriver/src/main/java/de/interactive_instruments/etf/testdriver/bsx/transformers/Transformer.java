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

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.basex.core.cmd.Set;

import de.interactive_instruments.IFile;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public interface Transformer {

    interface PreparedFileCollection {
        /**
         * BaseX parameters to use for adding this file
         *
         * @return BaseX database parameters
         */
        Collection<Set> parameters();

        /**
         * Returns the transformed file(s)
         *
         * @return transformed file
         */
        Collection<File> files();

        /**
         * The overall size of all transformed files
         *
         * @return
         */
        default long size() {
            long size = 0;
            for (final File file : files()) {
                size += file.length();
            }
            return size;
        }

        /**
         *
         * @return
         */
        default long fileCount() {
            return files().size();
        }

        IOException getException();

        default boolean exceptionOccurred() {
            return getException() != null;
        }
    }

    PreparedFileCollection transform(final IFile file) throws IOException;
}
