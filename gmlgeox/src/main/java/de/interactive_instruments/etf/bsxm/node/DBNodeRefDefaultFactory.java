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

import org.basex.query.value.node.DBNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A factory to create flyweight DBNodeRef instances.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class DBNodeRefDefaultFactory implements DBNodeRefFactory {

    final String databaseNamePrefix;
    final int dbNameLength;

    @Contract("null -> fail")
    DBNodeRefDefaultFactory(final String databaseNamePrefix) {
        if (databaseNamePrefix == null || databaseNamePrefix.length() < 4) {
            throw new IllegalArgumentException("Invalid database name: '" + databaseNamePrefix + "'. "
                    + "Database names must be suffixed with a three digits index, i.e. DB-000");
        }
        final int length = databaseNamePrefix.length();
        for (int i = length - 1; i >= length - 3; i--) {
            if (databaseNamePrefix.charAt(i) < '0' || databaseNamePrefix.charAt(i) > '9') {
                throw new IllegalArgumentException("Invalid database name: '" + databaseNamePrefix + "'. "
                        + "Database names must be suffixed with a three digits index, i.e. DB-000");
            }
        }
        this.databaseNamePrefix = databaseNamePrefix.substring(0, length - 3);
        this.dbNameLength = length;
    }

    @NotNull
    @Override
    public StringBuilder getSBForDbNamePrefix() {
        final StringBuilder sb = new StringBuilder(this.databaseNamePrefix.length() + 3);
        sb.append(this.databaseNamePrefix);
        return sb;
    }

    @Contract(pure = true)
    @Override
    public String getDbNamePrefix() {
        return databaseNamePrefix;
    }

    @NotNull
    @Contract("_ -> new")
    @Override
    public DBNodeRef createDBNodeEntry(@NotNull final DBNode node) {
        final String name = node.data().meta.name;
        final byte dbIndex = (byte) ((name.charAt(dbNameLength - 1) - '0') +
                (name.charAt(dbNameLength - 2) - '0') * 10 +
                (name.charAt(dbNameLength - 3) - '0') * 100);
        return new DBNodeRef(node, dbIndex);
    }
}
