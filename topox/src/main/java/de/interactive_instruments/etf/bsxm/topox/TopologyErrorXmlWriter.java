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
package de.interactive_instruments.etf.bsxm.topox;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.exceptions.ExcUtils;
import gnu.trove.TLongIntHashMap;

/**
 * Object for persisting topological errors in a XML file
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class TopologyErrorXmlWriter implements TopologyErrorCollector {

    private static final String TOPOX_ERROR_NS = "http://www.interactive-instruments.de/etf/topology-error/1.0";
    private static final String TOPOX_ERROR_NS_PREFIX = "ete";
    private final String themeName;
    private final XMLStreamWriter writer;
    // -1 indicates that the writer is not ready
    private int counter = -1;
    private final TLongIntHashMap coordinateHashToPreviousErrorId;

    public TopologyErrorXmlWriter(final String themeName, final XMLStreamWriter writer) {
        this.themeName = themeName;
        this.writer = writer;
        this.coordinateHashToPreviousErrorId = new TLongIntHashMap(128);
    }

    private void ensureReady() {
        if (counter < 0) {
            throw new IllegalStateException(
                    "Error writing topological error: the writer is not initialized or has already been released");
        }
    }

    @Override
    public void init() {
        try {
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement(TOPOX_ERROR_NS_PREFIX, "TopologicalErrors", TOPOX_ERROR_NS);
            writer.setPrefix(TOPOX_ERROR_NS_PREFIX, TOPOX_ERROR_NS);
            writer.writeNamespace(TOPOX_ERROR_NS_PREFIX, TOPOX_ERROR_NS);
            writer.writeAttribute("name", themeName);
            counter = 0;
        } catch (final XMLStreamException e) {
            throw new IllegalStateException("Initialization failed: ", e);
        }
    }

    @Override
    public void collectError(final TopologyErrorType topologyErrorType, final String... parameter) {
        ensureReady();
        try {
            // error element
            writer.writeStartElement("e");
            // id
            writer.writeAttribute("i", Integer.toString(++counter));
            // errorType
            writer.writeAttribute("t", topologyErrorType.toString());

            collectParameters(parameter);

            writer.writeEndElement();
        } catch (final XMLStreamException e) {
            final String message = themeName + " " +
                    topologyErrorType.toString() + " : " + SUtils.concatStr(" ", parameter);
            throw new IllegalStateException("Error writing topological error: " + message, e);
        }
    }

    @Override
    public void collectError(final TopologyErrorType topologyErrorType, final double x, final double y,
            final String... parameter) {
        ensureReady();
        try {
            // error element
            writer.writeStartElement("e");
            // id
            writer.writeAttribute("i", Integer.toString(++counter));
            // errorType
            writer.writeAttribute("t", topologyErrorType.toString());

            final long coordinateHash = TopologyBuilder.calcCoordHashCode(x, y);
            final int previousErrorI = coordinateHashToPreviousErrorId.get(coordinateHash);
            if (previousErrorI != 0) {
                // previous error
                writer.writeAttribute("p", Integer.toString(previousErrorI));
            } else {
                coordinateHashToPreviousErrorId.put(coordinateHash, counter);
            }
            writer.writeStartElement("X");
            writer.writeCharacters(Double.toString(x));
            writer.writeEndElement();

            writer.writeStartElement("Y");
            writer.writeCharacters(Double.toString(y));
            writer.writeEndElement();

            collectParameters(parameter);

            writer.writeEndElement();
        } catch (final XMLStreamException e) {
            final String message = themeName + " " +
                    topologyErrorType.toString() + " : " + SUtils.concatStr(" ", parameter);
            throw new IllegalStateException("Error writing topological error: " + message, e);
        }
    }

    private void collectParameters(final String... parameter) throws XMLStreamException {
        if (parameter != null) {
            for (int i = 0; i < parameter.length; i++) {
                writer.writeStartElement(parameter[i]);
                writer.writeCharacters(parameter[++i]);
                writer.writeEndElement();
            }
        }
    }

    @Override
    public void release() {
        try {
            // TopologicalErrors
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        } catch (final XMLStreamException e) {
            ExcUtils.suppress(e);
        }
    }
}
