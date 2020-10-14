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
package de.interactive_instruments.etf.bsxm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class DbNameMapping implements Externalizable {

    private List<String> dbNames;
    private Map<String, Integer> nameMapping;

    public DbNameMapping() {}

    int idFor(final String dbName) {
        if (this.nameMapping == null) {
            this.nameMapping = new HashMap<>();
            this.dbNames = new ArrayList<>();
            this.dbNames.add(dbName);
            this.nameMapping.put(dbName, 0);
            return 0;
        }
        final Integer id = this.nameMapping.get(dbName);
        if (id == null) {
            this.dbNames.add(dbName);
            final int i = this.dbNames.size() - 1;
            this.nameMapping.put(dbName, i);
            return i;
        }
        return id;
    }

    String nameFor(final int id) {
        return this.dbNames.get(id);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(this.dbNames.size());
        for (final String s : this.dbNames) {
            out.writeUTF(s);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        final int size = in.readInt();
        this.dbNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.dbNames.add(in.readUTF());
        }
    }
}
