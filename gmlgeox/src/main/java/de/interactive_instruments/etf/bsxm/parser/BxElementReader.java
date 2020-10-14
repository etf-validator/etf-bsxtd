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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;

import org.basex.query.value.node.ANode;
import org.basex.query.value.type.NodeType;
import org.basex.util.Token;
import org.w3c.dom.Element;

import de.interactive_instruments.IFile;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class BxElementReader implements BxReader {

    private final ANode rootNode;
    private ANode currentNode;
    private BxElementHandler.ElementVisitResult currentState;
    private int depth;
    private final BxElementHandler defaultHandler;
    private final Set<String> registeredElementNames = new HashSet<>();
    private final List<BxCachedElement> elementStack = new ArrayList<>();
    private final BxNamespaceHolder namespaceHolder;

    public BxElementReader(final ANode node, final BxElementHandler defaultHandler, final BxNamespaceHolder namespaceHolder) {
        this.rootNode = node;
        this.currentNode = rootNode;
        this.currentState = BxElementHandler.ElementVisitResult.CONTINUE;
        this.defaultHandler = defaultHandler;
        defaultHandler.elementsToRegister().stream().map(
                qName -> qName.getLocalPart()).forEach(registeredElementNames::add);
        this.namespaceHolder = namespaceHolder;
    }

    private BxCachedElement createPreparedElementFromCurrent() {
        final String name = Token.string(Token.local(currentNode.name()));
        final String prefix = Token.string(Token.prefix(currentNode.name()));
        final boolean needsToBeHandled = registeredElementNames.contains(name);
        return new BxCachedElement(elementStack.get(elementStack.size() - 1), currentNode, prefix, name, needsToBeHandled,
                namespaceHolder);
    }

    private BxCachedElement createPreparedElementFromCurrentAndAddToStack() {
        final BxCachedElement element = createPreparedElementFromCurrent();
        elementStack.add(element);
        return element;
    }

    private void fireStart(final BxCachedElement node) {
        if (node.mustBeHandled()) {
            currentState = defaultHandler.onStart(node, this);
        }
    }

    private void fireEnd() {
        while (elementStack.size() - 1 >= depth) {
            final BxCachedElement oldElement = elementStack.remove(elementStack.size() - 1);
            if (oldElement.mustBeHandled()) {
                defaultHandler.onEnd(oldElement, this);
            }
        }
    }

    public void read() {
        // create first element
        final String name = Token.string(Token.local(currentNode.name()));
        final String prefix = Token.string(Token.prefix(currentNode.name()));
        final boolean needsToBeHandled = registeredElementNames.contains(name);
        final BxCachedElement element = new BxCachedElement(null, currentNode, prefix, name, needsToBeHandled, namespaceHolder);
        elementStack.add(element);
        fireStart(element);
        next();
        while (currentNode != null && rootNode != currentNode) {
            next();
        }
    }

    private void next() {
        // get first child
        final ANode firstChild;
        if (currentState.equals(BxElementHandler.ElementVisitResult.SKIP_SUBTREE)) {
            firstChild = null;
            currentState = BxElementHandler.ElementVisitResult.CONTINUE;
        } else {
            firstChild = getFirstChildElement(currentNode);
        }
        if (firstChild != null) {
            depth++;
            currentNode = firstChild;
            fireStart(createPreparedElementFromCurrentAndAddToStack());
        } else if (depth == 0) {
            currentNode = rootNode;
            fireEnd();
        } else {
            final ANode nextSibling = getNextSiblingdElement(currentNode);
            if (nextSibling == null) {
                // the current node has no other siblings, go to its
                // parent nodes sibling
                depth--;
                ANode parent = currentNode.parent();
                ANode parentSibling = getNextSiblingdElement(parent);
                fireEnd();
                while (parentSibling == null && depth > 0) {
                    depth--;
                    parent = parent.parent();
                    parentSibling = getNextSiblingdElement(parent);
                    fireEnd();
                }

                if (depth > 0) {
                    currentNode = parentSibling;
                    fireStart(createPreparedElementFromCurrent());
                } else {
                    currentNode = rootNode;
                }
            } else {
                fireEnd();
                currentNode = nextSibling;
                fireStart(createPreparedElementFromCurrent());
            }
        }
    }

    private ANode getFirstChildElement(final ANode node) {
        ANode firstChild = node.childIter().next();
        while (firstChild != null && !firstChild.nodeType().eq(NodeType.ELM)) {
            firstChild = firstChild.followingSiblingIter().next();
        }
        return firstChild;
    }

    private ANode getNextSiblingdElement(final ANode node) {
        ANode sibling = node.followingSiblingIter().next();
        while (sibling != null && !sibling.nodeType().eq(NodeType.ELM)) {
            sibling = node.followingSiblingIter().next();
        }
        return sibling;
    }

    @Override
    public XMLStreamReader createSubStreamReader(final Element element) {
        return new DBNodeStreamReader(((BxCachedElement) element).getNode(), this.namespaceHolder);
    }

    @Override
    public String getSystemId() {
        return new IFile(this.currentNode.data().meta.original).getName();
    }
}
