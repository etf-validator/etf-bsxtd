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
package de.interactive_instruments.etf.testdriver.bsx;

import java.io.File;

import org.basex.core.Context;
import org.basex.io.IO;
import org.basex.io.IOFile;
import org.basex.query.util.UriResolver;
import org.basex.query.value.item.Uri;

import de.interactive_instruments.IFile;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class BsxUriResolver implements UriResolver {

    private final IFile ressourcesDir;
    private final File repoDir;

    public BsxUriResolver(final Context ctx, final IFile ressourcesDir) {
        this.ressourcesDir = ressourcesDir;
        this.repoDir = ctx.repo.path().file();
    }

    @Override
    public IO resolve(final String path, final String uri, final Uri base) {
        final IFile file = new IFile(repoDir).secureExpandPathDown(path);
        if (file.exists()) {
            return new IOFile(file);
        }
        return new IOFile(this.ressourcesDir.getAbsolutePath(), path);
    }
}
