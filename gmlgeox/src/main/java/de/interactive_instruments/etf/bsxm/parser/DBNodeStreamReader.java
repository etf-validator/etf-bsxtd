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

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.basex.data.Data;
import org.basex.query.value.item.QNm;
import org.basex.query.value.node.ANode;
import org.basex.query.value.node.DBNode;
import org.basex.util.Token;
import org.w3c.dom.Node;

import de.interactive_instruments.IFile;

/**
 * A BaseX BXNode Stream Reader that operates on the BaseX Node model for best performance.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final public class DBNodeStreamReader implements XMLStreamReader {

    private final ANode rootNode;
    private int currentEvent = XMLStreamConstants.START_DOCUMENT;
    private ANode currentNode;
    private String cachedName;
    private String cachedPrefix;
    private final BxNamespaceHolder namespaceHolder;

    public DBNodeStreamReader(final ANode node, final BxNamespaceHolder namespaceHolder) {
        this.rootNode = node;
        currentNode = rootNode;
        this.namespaceHolder = namespaceHolder;
    }

    @Override
    public Object getProperty(final String name) throws IllegalArgumentException {
        return null;
    }

    @Override
    public int next() {
        this.cachedName = null;
        switch (currentEvent) {
        case START_DOCUMENT:
            return (currentEvent = XMLStreamConstants.START_ELEMENT);
        case END_DOCUMENT:
            throw new IllegalStateException("END_DOCUMENT");
        case START_ELEMENT:
            final ANode firstChild = currentNode.childIter().next();
            if (firstChild == null) {
                return (currentEvent = XMLStreamConstants.END_ELEMENT);
            }
            currentNode = firstChild;
            break;
        case END_ELEMENT:
            if (currentNode == rootNode) {
                return (currentEvent = END_DOCUMENT);
            }
        default: {
            final ANode next = currentNode.followingSiblingIter().next();
            // If sibling, let's just assign and fall through
            if (next != null) {
                currentNode = next;
                break;
            }
            currentNode = currentNode.parent();
            if (mapType(currentNode) == Node.ELEMENT_NODE) {
                return (currentEvent = END_ELEMENT);
            }
            return (currentEvent = END_DOCUMENT);
        }
        }

        switch (mapType(currentNode)) {
        case Node.CDATA_SECTION_NODE:
        case Node.COMMENT_NODE:
            return (currentEvent = CDATA);
        case Node.ELEMENT_NODE:
            return (currentEvent = START_ELEMENT);
        case Node.ENTITY_REFERENCE_NODE:
            return (currentEvent = ENTITY_REFERENCE);
        case Node.PROCESSING_INSTRUCTION_NODE:
            return (currentEvent = PROCESSING_INSTRUCTION);
        case Node.TEXT_NODE:
            return (currentEvent = CHARACTERS);
        default:
            throw new IllegalStateException("Cannot parse unknown type" + currentNode.nodeType());
        }
    }

    static short mapType(final ANode node) {
        switch (node.nodeType()) {
        case DOC:
            return Node.DOCUMENT_NODE;
        case ELM:
            return Node.ELEMENT_NODE;
        case TXT:
            return Node.TEXT_NODE;
        case ATT:
            return Node.ATTRIBUTE_NODE;
        case COM:
            return Node.COMMENT_NODE;
        case PI:
            return Node.PROCESSING_INSTRUCTION_NODE;
        default:
            return -1;
        }
    }

    @Override
    public void require(final int type, final String namespaceURI, final String localName) {
        // ignore
    }

    @Override
    public String getElementText() {
        return Token.string(this.currentNode.string());
    }

    @Override
    public int nextTag() {
        while (true) {
            int next = next();
            if (next == START_ELEMENT || next == END_ELEMENT) {
                return next;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return (currentEvent != END_DOCUMENT);
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public String getNamespaceURI(final String prefix) {
        throw new UnsupportedOperationException("getNamespaceURI(prefix) is not supported");
    }

    @Override
    public boolean isStartElement() {
        return this.currentEvent == XMLStreamConstants.START_ELEMENT;
    }

    @Override
    public boolean isEndElement() {
        return this.currentEvent == XMLStreamConstants.END_ELEMENT;
    }

    @Override
    public boolean isCharacters() {
        return this.currentEvent == XMLStreamConstants.CHARACTERS;
    }

    @Override
    public boolean isWhiteSpace() {
        // handled by BaseX
        return false;
    }

    @Override
    public String getAttributeValue(final String namespaceURI, final String localName) {
        final byte[] attr = currentNode.attribute(new QNm(localName, namespaceURI));
        return attr != null ? Token.string(attr) : null;
    }

    @Override
    public int getAttributeCount() {
        throw new UnsupportedOperationException("getAttributeCount() is not supported");
    }

    @Override
    public QName getAttributeName(final int index) {
        throw new UnsupportedOperationException("getAttributeName(index) is not supported");
    }

    @Override
    public String getAttributeNamespace(final int index) {
        throw new UnsupportedOperationException("getAttributeNamespace(index) is not supported");
    }

    @Override
    public String getAttributeLocalName(final int index) {
        throw new UnsupportedOperationException("getAttributeLocalName(index) is not supported");
    }

    @Override
    public String getAttributePrefix(final int index) {
        throw new UnsupportedOperationException("getAttributePrefix(index) is not supported");
    }

    @Override
    public String getAttributeType(final int index) {
        throw new UnsupportedOperationException("getAttributeType(index) is not supported");
    }

    @Override
    public String getAttributeValue(final int index) {
        throw new UnsupportedOperationException("getAttributeType(index) is not supported");
    }

    @Override
    public boolean isAttributeSpecified(final int index) {
        throw new UnsupportedOperationException("isAttributeSpecified(index) is not supported");
    }

    @Override
    public int getNamespaceCount() {
        throw new UnsupportedOperationException("getNamespaceCount() is not supported");
    }

    @Override
    public String getNamespacePrefix(final int index) {
        throw new UnsupportedOperationException("getNamespacePrefix(index) is not supported");
    }

    @Override
    public String getNamespaceURI(final int index) {
        throw new UnsupportedOperationException("getNamespaceURI(index) is not supported");
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        throw new UnsupportedOperationException("getNamespaceContext() is not supported");
    }

    @Override
    public int getEventType() {
        return this.currentEvent;
    }

    @Override
    public String getText() {
        return Token.string(currentNode.string());
    }

    @Override
    public char[] getTextCharacters() {
        return getText().toCharArray();
    }

    @Override
    public int getTextCharacters(final int sourceStart, final char[] target, final int targetStart, final int length) {
        throw new UnsupportedOperationException("getTextCharacters() is not supported");
    }

    @Override
    public int getTextStart() {
        throw new UnsupportedOperationException("getTextStart() is not supported");
    }

    @Override
    public int getTextLength() {
        return getText().length();
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public boolean hasText() {
        throw new UnsupportedOperationException("hasText() is not supported");
    }

    @Override
    public Location getLocation() {
        final String systemId = new IFile(this.currentNode.data().meta.original).getName();
        return new Location() {
            @Override
            public int getLineNumber() {
                return 0;
            }

            @Override
            public int getColumnNumber() {
                return -1;
            }

            @Override
            public int getCharacterOffset() {
                return -1;
            }

            @Override
            public String getPublicId() {
                return null;
            }

            @Override
            public String getSystemId() {
                return systemId;
            }
        };
    }

    void ensureCached() {
        if (cachedName == null) {
            final byte[] name = this.currentNode.data().name(((DBNode) this.currentNode).pre(), Data.ELEM);
            this.cachedName = Token.string(Token.local(name));
            this.cachedPrefix = Token.string(Token.prefix(name));
        }
    }

    @Override
    public QName getName() {
        ensureCached();
        return new QName(namespaceHolder.namespace(this.cachedPrefix), this.cachedName, this.cachedPrefix);
    }

    @Override
    public String getLocalName() {
        ensureCached();
        return this.cachedName;
    }

    @Override
    public String getNamespaceURI() {
        ensureCached();
        return namespaceHolder.namespace(this.cachedPrefix);
    }

    @Override
    public String getPrefix() {
        ensureCached();
        return this.cachedPrefix;
    }

    @Override
    public boolean hasName() {
        return (currentEvent == START_ELEMENT) || (currentEvent == END_ELEMENT);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public boolean isStandalone() {
        return false;
    }

    @Override
    public boolean standaloneSet() {
        return false;
    }

    @Override
    public String getCharacterEncodingScheme() {
        return null;
    }

    @Override
    public String getPITarget() {
        return "";
    }

    @Override
    public String getPIData() {
        return "";
    }
}
