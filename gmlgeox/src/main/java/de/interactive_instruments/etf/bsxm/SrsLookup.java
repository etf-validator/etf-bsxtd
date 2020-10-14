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

import org.basex.query.value.node.ANode;
import org.basex.query.value.type.Type;
import org.basex.util.Token;
import org.basex.util.hash.TokenIntMap;
import org.deegree.cs.CRSCodeType;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.cs.persistence.CRSManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.interactive_instruments.SUtils;

/**
 * Command to determine the SRS
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 * @author Johannes Echterhoff ( echterhoff aT interactive-instruments doT de )
 */
final public class SrsLookup {
    // Byte comparison
    private static final byte[] srsNameB = "srsName".getBytes();
    private static final byte[] boundedByB = "boundedBy".getBytes();
    private static final byte[] envelopeB = "Envelope".getBytes();
    private String standardSRS = null;
    private ICRS standardDeegreeSRS = null;

    void setStandardSRS(final String standardSRS) {
        if (SUtils.isNullOrEmpty(standardSRS)) {
            this.standardSRS = null;
            this.standardDeegreeSRS = null;
        } else {
            this.standardSRS = standardSRS;
            this.standardDeegreeSRS = CRSManager.get("default").getCRSByCode(CRSCodeType.valueOf(standardSRS));
        }
    }

    @Contract(pure = true)
    String getStandardSRS() {
        return standardSRS;
    }

    @Nullable
    public ICRS getSrsForGeometryNode(final ANode geometryNode) {
        final String srsName = determineSrsName(geometryNode);
        if (srsName != null) {
            return lookup(srsName);
        } else {
            return null;
        }
    }

    @Nullable
    ICRS getSrsForGeometryComponentNode(final ANode geometryComponentNode) {
        final String srsName = determineSrsNameForGeometryComponent(geometryComponentNode);
        if (srsName != null) {
            return lookup(srsName);
        } else {
            return null;
        }
    }

    private ICRS lookup(String srsName) {
        if (srsName.equals(standardSRS)) {
            // use pre-computed ICRS
            return standardDeegreeSRS;
        } else {
            return CRSManager.getCRSRef(srsName);
        }
    }

    @Nullable
    String determineSrsName(@NotNull final ANode geometryNode) {
        final byte[] srsDirect = geometryNode.attribute(srsNameB);
        // search for @srsName in geometry node first
        if (srsDirect != null) {
            return Token.string(srsDirect);
        }
        return determineSrsNameForGeometryComponent(geometryNode);
    }

    private @Nullable String searchSrsNameInAncestorBoundedBy(@NotNull ANode node) {
        // Search in ancestor for boundedBy/Envelope with @srsName
        for (final ANode ancestor : node.ancestorIter()) {
            for (final ANode ancestorChild : ancestor.childIter()) {
                if (ancestorChild.type.id() != Type.ID.COM && Token.eq(boundedByB, Token.local(ancestorChild.name()))) {
                    for (final ANode boundedByChild : ancestorChild.childIter()) {
                        if (boundedByChild.type.id() != Type.ID.COM
                                && Token.eq(envelopeB, Token.local(boundedByChild.name()))) {
                            final byte[] srs = boundedByChild.attribute(srsNameB);
                            if (srs != null) {
                                return Token.string(srs);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private String searchSrsNameInAncestors(@NotNull ANode node) {

        // Traverse the ancestor nodes. The following time-consuming steps should be
        // avoided by setting the default srs.
        for (final ANode ancestor : node.ancestorIter()) {
            final byte[] srs = ancestor.attribute(srsNameB);
            if (srs != null) {
                return Token.string(srs);
            }
        }
        return null;
    }

    private boolean attributeIndexHasSrsName(@NotNull ANode node) {
        // query the index (side effect: a non-existing index will be created)
        final int index = node.data().attrNames.index(srsNameB);
        final TokenIntMap values = node.data().attrNames.stats(index).values;
        // we will never find an srsName attribute
        return values != null && values.size() != 0;
    }

    @Nullable
    String determineSrsNameForGeometryComponent(@NotNull final ANode geometryComponentNode) {
        if (this.standardSRS != null) {
            return this.standardSRS;
        } else {
            // NOTE: DO NOT search for @srsName in the component itself
            // Check the attribute index. If it does NOT contain an srsName attribute,
            // then a search for such attributes in the wider XML structure (see the following steps) can be avoided.
            if (geometryComponentNode.data() != null && !attributeIndexHasSrsName(geometryComponentNode)) {
                return null;
            }
            final String srsName = searchSrsNameInAncestors(geometryComponentNode);
            if (srsName != null) {
                return srsName;
            } else {
                return searchSrsNameInAncestorBoundedBy(geometryComponentNode);
            }
        }
    }
}
