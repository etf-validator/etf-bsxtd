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
package de.interactive_instruments.etf.bsxm.parser;

import java.util.HashMap;

import org.basex.query.QueryContext;
import org.basex.util.Token;
import org.jetbrains.annotations.NotNull;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class BxNamespaceHolder {
    private final HashMap<String, String> prefixNamespaceMapping = new HashMap<>();
    private final HashMap<String, String> namespacePrefixMapping = new HashMap<>();

    public BxNamespaceHolder() {}

    public BxNamespaceHolder define(final String prefix, final String namespaceUri) {
        this.prefixNamespaceMapping.put(prefix, namespaceUri);
        this.namespacePrefixMapping.put(namespaceUri, prefix);
        return this;
    }

    public String prefix(final String namespaceUri) {
        return this.namespacePrefixMapping.get(namespaceUri);
    }

    public String namespace(final String prefix) {
        return this.prefixNamespaceMapping.get(prefix);
    }

    public static BxNamespaceHolder init(@NotNull final QueryContext queryContext) {
        if (queryContext.context.data() != null) {
            final int gmlIdIndex = queryContext.context.data().nspaces.uriIdForPrefix("gml".getBytes(), 1,
                    queryContext.context.data());
            if (gmlIdIndex > 0) {
                return new BxNamespaceHolder().define("gml",
                        Token.string(queryContext.context.data().nspaces.uri(gmlIdIndex)));
            }
        }
        // fallback
        return new BxNamespaceHolder().define("gml", "http://www.opengis.net/gml/3.2");
    }
}
