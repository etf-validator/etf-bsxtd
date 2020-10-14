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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.basex.core.cmd.Set;

import de.interactive_instruments.IFile;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class DefaultPreparedFileCollection implements Transformer.PreparedFileCollection {

    private final Collection<Set> parameters;
    private final Collection<File> files;
    private IOException throwable;

    public DefaultPreparedFileCollection(final Collection<Set> parameters) {
        this.parameters = parameters;
        this.files = new ArrayList<>();
    }

    public DefaultPreparedFileCollection(final Collection<Set> parameters, final File file) {
        this.parameters = parameters;
        this.files = Collections.singleton(file);
    }

    void addFile(final IFile file) {
        this.files.add(file);
    }

    void setThrowable(final IOException throwable) {
        if (this.throwable != null) {
            throw new IllegalStateException("Exception already set", throwable);
        }
        this.throwable = throwable;
    }

    @Override
    public IOException getException() {
        return throwable;
    }

    @Override
    public Collection<Set> parameters() {
        return parameters;
    }

    @Override
    public Collection<File> files() {
        return files;
    }
}
