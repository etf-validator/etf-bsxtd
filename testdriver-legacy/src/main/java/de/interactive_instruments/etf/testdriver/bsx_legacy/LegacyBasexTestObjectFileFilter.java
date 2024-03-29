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
package de.interactive_instruments.etf.testdriver.bsx_legacy;

import java.io.File;
import java.io.FileFilter;

import de.interactive_instruments.io.MultiFileFilter;

/**
 * A PathFilter and FileFilter implementation which accepts files with GML and XML filename endings.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class LegacyBasexTestObjectFileFilter implements MultiFileFilter {

    private final FileFilter ff;

    public LegacyBasexTestObjectFileFilter(final FileFilter ff) {
        this.ff = ff;
    }

    public LegacyBasexTestObjectFileFilter() {
        ff = null;
    }

    @Override
    public boolean accept(final File pathname) {
        final String p = pathname.getName().toUpperCase();
        return '.' != p.charAt(0) && (p.endsWith(".XML") || p.endsWith(".GML")) && (ff == null || ff
                .accept(pathname));
    }
}
