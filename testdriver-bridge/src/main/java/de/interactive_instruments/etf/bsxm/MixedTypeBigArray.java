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

import org.basex.data.Data;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.value.Value;
import org.basex.query.value.item.Item;
import org.basex.query.value.item.Str;
import org.basex.query.value.node.DBNode;
import org.basex.query.value.seq.Seq;
import org.basex.util.InputInfo;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class MixedTypeBigArray implements BigArray {

    private interface Entry extends Externalizable {
        Item get(final QueryContext qc);
    }

    private static class StrTypeEntry implements Entry {

        private byte[] bytes;

        public StrTypeEntry(final Str str) {
            bytes = str.string();
        }

        public StrTypeEntry() {}

        @Override
        public Item get(final QueryContext qc) {
            return Str.get(bytes);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(bytes.length);
            out.write(bytes);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            bytes = new byte[in.readInt()];
            in.readFully(bytes);
        }
    }

    private static class DBNodeEntry implements Entry {
        private int pre;
        private int nodeKind;
        private String dbname;

        public DBNodeEntry(final DBNode dbNode) {
            pre = dbNode.pre();
            nodeKind = dbNode.kind();
            dbname = dbNode.data().meta.name;
        }

        public DBNodeEntry(final int pre, final int nodeKind, final String dbname) {
            this.pre = pre;
            this.nodeKind = nodeKind;
            this.dbname = dbname;
        }

        public DBNodeEntry() {}

        @Override
        public Item get(final QueryContext qc) {
            try {
                final Data d = qc.resources.database(dbname, new InputInfo("xpath", 0, 0));
                return new DBNode(d, pre, nodeKind);
            } catch (QueryException e) {
                throw new IllegalStateException("Node lookup failed. "
                        + ", DB: " + dbname
                        + ", PRE: " + pre
                        + ", NK: " + nodeKind, e);
            }
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(pre);
            out.writeInt(nodeKind);
            out.writeUTF(dbname);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            pre = in.readInt();
            nodeKind = in.readInt();
            dbname = in.readUTF();
        }
    }

    private Entry[] entries;
    private MixedTypeBigArray next;
    private int index = 0;

    MixedTypeBigArray(final long size) {
        final int maxSize = (int) Math.min(ARRAY_MAX_SIZE, size);
        entries = new Entry[maxSize];
        if (size > ARRAY_MAX_SIZE) {
            next = new MixedTypeBigArray(size - maxSize);
        }
    }

    public MixedTypeBigArray() {
        index = -1;
    }

    MixedTypeBigArray addNative(final int pre, final int nodeKind, final String dbname) {
        if (index < ARRAY_MAX_SIZE) {
            entries[index++] = new DBNodeEntry(pre, nodeKind, dbname);
            return this;
        } else {
            return next.addNative(pre, nodeKind, dbname);
        }
    }

    public BigArray add(final Value value) {
        if (index < ARRAY_MAX_SIZE) {
            if (value instanceof DBNode) {
                entries[index++] = new DBNodeEntry((DBNode) value);
            } else if (value instanceof Str) {
                entries[index++] = new StrTypeEntry((Str) value);
            } else {
                throw new IllegalArgumentException(
                        "Unexpected type stored in map: " + value.getClass().getName());
            }
        } else {
            next.add(value);
        }
        return this;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(entries.length);
        for (int i = 0; i < entries.length; i++) {
            out.writeObject(entries[i]);
        }
        out.writeObject(next);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final int size = in.readInt();
        entries = new Entry[size];
        for (int i = 0; i < size; i++) {
            entries[i] = (Entry) in.readObject();
        }
        next = (MixedTypeBigArray) in.readObject();
    }

    @Override
    public Seq sequence(final QueryContext qc) {
        return new EntrySequence(qc, this);
    }

    public long size() {
        if (next != null) {
            return entries.length + next.size();
        }
        return entries.length;
    }

    @Override
    public Item get(final QueryContext qc, final long index) {
        if (index < ARRAY_MAX_SIZE) {
            final int i = (int) index;
            return entries[i].get(qc);
        } else {
            return next.get(qc, index - ARRAY_MAX_SIZE);
        }
    }
}
