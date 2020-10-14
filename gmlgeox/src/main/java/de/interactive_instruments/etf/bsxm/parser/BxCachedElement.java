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

import static org.basex.util.Token.*;

import org.basex.api.dom.*;
import org.basex.io.IO;
import org.basex.query.value.node.ANode;
import org.basex.query.value.type.NodeType;
import org.basex.util.Token;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;

/**
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class BxCachedElement implements Element {

    private final String name;
    private final String prefix;
    private final BxCachedElement parent;
    private final ANode dbNode;
    private final boolean needsToBeHandled;
    private final BxNamespaceHolder namespaceHolder;
    private NamedNodeMap cachedNamedNodeMap;

    @Contract(pure = true)
    BxCachedElement(final BxCachedElement parent, final ANode dbNode, final String prefix, final String name,
            final boolean needsToBeHandled, final BxNamespaceHolder namespaceHolder) {
        this.dbNode = dbNode;
        this.parent = parent;
        this.needsToBeHandled = needsToBeHandled;
        this.namespaceHolder = namespaceHolder;
        this.prefix = prefix;
        this.name = name;
    }

    public String getNodeName() {
        return !"".equals(prefix) ? prefix + ":" + name : name;
    }

    public String getLocalName() {
        return name;
    }

    public String getNamespaceURI() {
        return namespaceHolder.namespace(prefix);
    }

    public String getTagName() {
        return getNodeName();
    }

    public BxCachedElement getParentNode() {
        return parent;
    }

    public ANode getNode() {
        return dbNode;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(final Object obj) {
        return obj.equals(dbNode) || obj instanceof BxCachedElement && ((BxCachedElement) obj).dbNode.equals(dbNode);
    }

    public boolean mustBeHandled() {
        return needsToBeHandled;
    }

    @Nullable
    @Override
    public final BXNode getLastChild() {
        ANode node = null;
        for (final ANode n : dbNode.childIter())
            node = n;
        return node != null ? BXNode.get(node) : null;
    }

    @Override
    public NodeList getChildNodes() {
        // todo optimize
        return dbNode.toJava().getChildNodes();
    }

    @Override
    public NamedNodeMap getAttributes() {
        if (cachedNamedNodeMap == null) {
            cachedNamedNodeMap = new BxCachedNamedNodeMap(dbNode.attributeIter());
        }
        return cachedNamedNodeMap;
    }

    public final BxCachedElement cloneNode(final boolean deep) {
        return this;
    }

    // delegates

    @Override
    public final String getBaseURI() {
        return IO.get(string(dbNode.baseURI())).url();
    }

    @Override
    public BXNode getFirstChild() {
        return BXNode.get(dbNode.childIter().next());
    }

    @Override
    public String getNodeValue() {
        return null;
    }

    @Override
    public String getAttribute(final String name) {
        final ANode n = attribute(name);
        return n != null ? Token.string(n.string()) : "";
    }

    @Override
    public final short getNodeType() {
        return Node.ELEMENT_NODE;
    }

    @Override
    public final short compareDocumentPosition(final Node node) {
        return (short) Integer.compare(dbNode.diff(((BXNode) node).getNode()), 0);
    }

    @Override
    public BXNode getNextSibling() {
        return BXNode.get(dbNode.followingSiblingIter().next());
    }

    @Override
    public BXNode getPreviousSibling() {
        return BXNode.get(dbNode.precedingSiblingIter().next());
    }

    @Override
    public final boolean hasAttributes() {
        return getAttributes().getLength() != 0;
    }

    @Override
    public final Object getFeature(final String feature, final String version) {
        return null;
    }

    @Override
    public final String getPrefix() {
        return null;
    }

    @Override
    public final String getTextContent() {
        return string(dbNode.string());
    }

    @Override
    public final BXNode appendChild(final Node node) {
        throw readOnly();
    }

    @Override
    public final Object getUserData(final String name) {
        return null;
    }

    @Override
    public final boolean isSupported(final String name, final String version) {
        return false;
    }

    @Override
    public final BXNode insertBefore(final Node node, final Node ref) {
        throw readOnly();
    }

    @Override
    public final boolean isDefaultNamespace(final String uri) {
        throw notImplemented();
    }

    @Override
    public final boolean isEqualNode(final Node cmp) {
        throw notImplemented();
    }

    @Override
    public final String lookupNamespaceURI(final String prefix) {
        throw notImplemented();
    }

    @Override
    public final String lookupPrefix(final String uri) {
        throw notImplemented();
    }

    @Override
    public final void normalize() {
        throw readOnly();
    }

    @Override
    public final BXNode removeChild(final Node node) {
        throw readOnly();
    }

    @Override
    public final BXNode replaceChild(final Node node, final Node old) {
        throw readOnly();
    }

    @Override
    public final void setNodeValue(final String value) {
        throw readOnly();
    }

    @Override
    public final void setPrefix(final String prefix) {
        throw readOnly();
    }

    @Override
    public final void setTextContent(final String value) {
        throw readOnly();
    }

    @Override
    public final Object setUserData(final String name, final Object value,
            final UserDataHandler handler) {
        throw readOnly();
    }

    @Override
    public final boolean hasChildNodes() {
        return getFirstChild() != null;
    }

    @Override
    public final boolean isSameNode(final Node node) {
        return node instanceof BxCachedElement && ((BxCachedElement) node).dbNode.is(dbNode);
    }

    @Override
    public BXDoc getOwnerDocument() {
        ANode n = dbNode;
        for (ANode p; (p = n.parent()) != null;)
            n = p;
        return n.type == NodeType.DOC ? (BXDoc) BXNode.get(n) : null;
    }

    @Override
    public boolean hasAttribute(final String name) {
        return attribute(name) != null;
    }

    @Override
    public String getAttributeNS(final String uri, final String name) {
        throw notImplemented();
    }

    @Override
    public BXAttr getAttributeNode(final String name) {
        return (BXAttr) BXNode.get(attribute(name));
    }

    @Override
    public BXAttr getAttributeNodeNS(final String uri, final String name) {
        throw notImplemented();
    }

    @Override
    public NodeList getElementsByTagName(final String name) {
        return ((BXElem) this.dbNode.toJava()).getElementsByTagName(name);
    }

    @Override
    public NodeList getElementsByTagNameNS(final String uri, final String name) {
        throw notImplemented();
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        throw notImplemented();
    }

    @Override
    public boolean hasAttributeNS(final String uri, final String name) {
        throw notImplemented();
    }

    @Override
    public void removeAttribute(final String name) {
        throw readOnly();
    }

    @Override
    public void removeAttributeNS(final String uri, final String name) {
        throw readOnly();
    }

    @Override
    public BXAttr removeAttributeNode(final Attr oldAttr) {
        throw readOnly();
    }

    @Override
    public void setAttribute(final String name, final String value) {
        throw readOnly();
    }

    @Override
    public void setAttributeNS(final String uri, final String name, final String value) {
        throw readOnly();
    }

    @Override
    public BXAttr setAttributeNode(final Attr node) {
        throw readOnly();
    }

    @Override
    public BXAttr setAttributeNodeNS(final Attr node) {
        throw readOnly();
    }

    @Override
    public void setIdAttribute(final String name, final boolean id) {
        throw readOnly();
    }

    @Override
    public void setIdAttributeNS(final String uri, final String name, final boolean id) {
        throw readOnly();
    }

    @Override
    public void setIdAttributeNode(final Attr node, final boolean id) {
        throw readOnly();
    }

    private ANode attribute(final String name) {
        final byte[] nm = Token.token(name);
        for (final ANode n : dbNode.attributeIter())
            if (Token.eq(nm, n.name()))
                return n.finish();
        return null;
    }

    static UnsupportedOperationException notImplemented() {
        return new UnsupportedOperationException();
    }

    static DOMException readOnly() {
        return new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                "DOM implementation is read-only.");
    }
}
