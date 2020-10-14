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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.basex.data.Data;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.value.Value;
import org.basex.query.value.item.Item;
import org.basex.query.value.node.DBNode;
import org.basex.query.value.seq.Seq;
import org.basex.util.InputInfo;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class DBNodeBigArray implements BigArray {

    private long[] compressedData;
    private DbNameMapping dbNameMapping = new DbNameMapping();
    private int index = 0;
    private DBNodeBigArray next;

    public DBNodeBigArray(final long size) {
        final int maxSize = (int) Math.min(ARRAY_MAX_SIZE, size);
        compressedData = new long[maxSize];
        if (size > ARRAY_MAX_SIZE) {
            next = new DBNodeBigArray(size - maxSize);
        }
    }

    /**
     * Ctor for Externalizable
     */
    public DBNodeBigArray() {
        this.index = -1;
    }

    private static int nodeKind(final long compressedData) {
        return ((int) compressedData) >>> 16;
    }

    private static int pre(final long compressedData) {
        return (int) (compressedData >> 32);
    }

    private String dbName(final long compressedData) {
        final int dbIndex = (((int) compressedData) & 0xFFFF);
        return this.dbNameMapping.nameFor(dbIndex);
    }

    private long compress(final DBNode dbNode) {
        final String dbName = dbNode.data().meta.name;
        final int dbId = this.dbNameMapping.idFor(dbName);
        return (((long) dbNode.pre()) << 32) | (dbNode.kind() << 16 | dbId & 0xFFFFFFFFL);
    }

    private void copyNative(final MixedTypeBigArray mixedTypeArray) {
        for (int i = 0; i < Math.min(index, compressedData.length); i++) {
            final long compressed = this.compressedData[i];
            mixedTypeArray.addNative(pre(compressed), nodeKind(compressed), dbName(compressed));
        }
        if (next != null) {
            next.copyNative(mixedTypeArray);
        }
    }

    public BigArray add(final Value value) {
        if (!(value instanceof DBNode)) {
            // switch array type
            final MixedTypeBigArray mixedTypeArray = new MixedTypeBigArray(this.size());
            copyNative(mixedTypeArray);
            mixedTypeArray.add(value);
            return mixedTypeArray;
        }
        if (index < ARRAY_MAX_SIZE) {
            final DBNode dbNode = (DBNode) value;
            compressedData[index++] = compress(dbNode);

            // TODO remove
            final long check = compress(dbNode);
            if (!dbName(check).equals(dbNode.data().meta.name) || pre(check) != dbNode.pre()
                    || nodeKind(check) != dbNode.kind()) {
                System.out.println("Failed");
            }
            // TODO remove code above

        } else {
            next.add(value);
        }
        return this;
    }

    @Override
    public long size() {
        if (next != null) {
            return compressedData.length + next.size();
        }
        return compressedData.length;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof DBNodeBigArray) {
            final DBNodeBigArray ba = (DBNodeBigArray) obj;
            return Arrays.equals(compressedData, ba.compressedData) &&
                    ((next == null && ba.next == null) || (next != null && next.equals(ba.next)));
        }
        return false;
    }

    @Override
    public Item get(final QueryContext qc, final long index) {
        if (index < ARRAY_MAX_SIZE) {
            final int i = (int) index;
            final long compressed = compressedData[i];
            try {
                final Data d = qc.resources.database(dbName(compressed), new InputInfo("xpath", 0, 0));
                return new DBNode(d, pre(compressed), nodeKind(compressed));
            } catch (QueryException e) {
                throw new IllegalStateException("Node lookup failed. "
                        + "Index: " + index
                        + ", DB: " + dbName(compressed)
                        + ", PRE: " + pre(compressed)
                        + ", NK: " + nodeKind(compressed), e);
            }
        } else {
            return next.get(qc, index - ARRAY_MAX_SIZE);
        }
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(this.dbNameMapping);
        out.writeInt(compressedData.length);
        for (final long compressedDatum : compressedData) {
            out.writeLong(compressedDatum);
        }
        out.writeObject(this.next);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        this.dbNameMapping = (DbNameMapping) in.readObject();
        final int size = in.readInt();
        compressedData = new long[size];
        for (int i = 0; i < compressedData.length; i++) {
            compressedData[i] = in.readLong();
        }
        this.next = (DBNodeBigArray) in.readObject();
    }

    public Seq sequence(final QueryContext qc) {
        return new EntrySequence(qc, this);
    }

}
