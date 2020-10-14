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
package de.interactive_instruments.etf.bsxm.node;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.value.node.DBNode;
import org.basex.util.InputInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * This flyweight class holds BaseX information to quickly access a node in the database. The instances are stored in
 * the spatial index.
 *
 * The {@link DBNodeRefLookup } command must be used to retrieve the BaseX
 *
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot> de)
 * @author Jon Herrmann (herrmann <at> interactive-instruments <dot> de)
 */
final public class DBNodeRef implements Comparable<DBNodeRef> {

    /**
     * We are compressing three values as 8 bytes to save memory (and hard disk storage when serialized). The first 32 bits
     * are for the pre value, the next 16 bit for the nodeKind value and the last 16 bits are reserved for the database
     * index (but as in TopoX we only use one byte right now).
     */
    private final long compressedData;

    DBNodeRef(@NotNull final DBNode dbNode, final byte dbIndex) {
        compressedData = (((long) dbNode.pre()) << 32) | (dbNode.kind() << 16 | dbIndex & 0xFFFFFFFFL);
    }

    @Contract(pure = true)
    private DBNodeRef(final long compressedData) {
        this.compressedData = compressedData;
    }

    @Contract(pure = true)
    private int getPre() {
        return (int) (compressedData >> 32);
    }

    @NotNull
    private String getDBname(@NotNull final DBNodeRefFactory callback) {
        final String dbIndexStr = Integer.toString((((int) compressedData) & 0xFFFF));
        final StringBuilder sb = callback.getSBForDbNamePrefix();
        final int pads = 3 - dbIndexStr.length();
        if (pads > 0) {
            for (int i = pads - 1; i >= 0; i--) {
                sb.append('0');
            }
        }
        return sb.append(dbIndexStr).toString();
    }

    @Contract(pure = true)
    private int getNodeKind() {
        return ((int) compressedData) >>> 16;
    }

    @NotNull
    @Contract("_, _ -> new")
    DBNode resolve(@NotNull final QueryContext queryContext, final DBNodeRefFactory callback) {
        try {
            return new DBNode(
                    queryContext.resources.database(this.getDBname(callback), new InputInfo("xpath", 0, 0)),
                    getPre(),
                    getNodeKind());
        } catch (QueryException e) {
            throw new IllegalStateException("Could not query DBNode. "
                    + "Pre: " + getPre() + " DBName: " + getDBname(callback) + " Kind: " + getNodeKind() + " Compressed data: "
                    + compressedData);
        }
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static DBNodeRef create(final long compressedData) {
        return new DBNodeRef(compressedData);
    }

    @Contract(pure = true)
    long getNativeData() {
        return this.compressedData;
    }

    @Contract(pure = true)
    @Override
    public int hashCode() {
        int hash = 48527 >>> ((int) compressedData & 0xFFFF);
        hash ^= hash * 194977 * (int) (compressedData >> 32);
        hash *= 194977;
        return hash;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return this.compressedData == ((DBNodeRef) obj).compressedData;
    }

    @Contract(pure = true)
    @Override
    public int compareTo(@NotNull final DBNodeRef o) {
        return Long.compare(compressedData, o.compressedData);
    }
}
