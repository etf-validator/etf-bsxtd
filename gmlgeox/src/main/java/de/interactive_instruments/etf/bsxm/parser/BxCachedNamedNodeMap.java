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

import static de.interactive_instruments.etf.bsxm.parser.BxCachedElement.notImplemented;
import static de.interactive_instruments.etf.bsxm.parser.BxCachedElement.readOnly;

import org.basex.api.dom.BXAttr;
import org.basex.api.dom.BXNode;
import org.basex.query.iter.BasicNodeIter;
import org.basex.query.value.node.ANode;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class BxCachedNamedNodeMap implements NamedNodeMap {

    private final BXAttr[] attributes;

    BxCachedNamedNodeMap(final BasicNodeIter attributes) {
        this.attributes = new BXAttr[((int) attributes.size())];
        int i = 0;
        ANode currentNode = attributes.next();
        while (currentNode != null) {
            this.attributes[i++] = (BXAttr) BXNode.get(currentNode.finish());
            currentNode = attributes.next();
        }
    }

    @Override
    public Node getNamedItem(final String name) {
        for (final BXAttr attribute : this.attributes) {
            if (attribute.getLocalName().equals(name)) {
                return attribute;
            }
        }
        return null;
    }

    @Override
    public Node item(final int index) {
        return this.attributes[index];
    }

    @Override
    public int getLength() {
        return this.attributes.length;
    }

    @Override
    public Node setNamedItem(final Node arg) throws DOMException {
        throw readOnly();
    }

    @Override
    public Node removeNamedItem(final String name) throws DOMException {
        throw readOnly();
    }

    @Override
    public Node getNamedItemNS(final String namespaceURI, final String localName) throws DOMException {
        throw notImplemented();
    }

    @Override
    public Node setNamedItemNS(final Node arg) throws DOMException {
        throw readOnly();
    }

    @Override
    public Node removeNamedItemNS(final String namespaceURI, final String localName) throws DOMException {
        throw readOnly();
    }
}
