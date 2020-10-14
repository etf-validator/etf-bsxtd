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

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.value.Value;
import org.basex.query.value.item.Item;
import org.basex.query.value.seq.Seq;

public class ExternalizedMapEntry implements Externalizable {

    byte[] key;
    private BigArray arr;

    public ExternalizedMapEntry(final Item key, final Value value) throws QueryException {
        this.key = key.string(null);
        arr = new DBNodeBigArray(value.size());
        for (final Item item : value.iter()) {
            arr = arr.add(item);
        }
    }

    public ExternalizedMapEntry() {}

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(key.length);
        out.write(key);
        out.writeObject(arr);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        key = new byte[in.readInt()];
        in.readFully(key);
        arr = (BigArray) in.readObject();
    }

    public byte[] getKey() {
        return key;
    }

    public Value getValues(final QueryContext qc) {
        final Seq seq = arr.sequence(qc);
        if (seq.isItem()) {
            return seq.itemAt(0);
        }
        return seq;
    }
}
