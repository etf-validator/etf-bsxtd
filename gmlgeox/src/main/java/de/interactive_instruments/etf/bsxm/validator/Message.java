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
package de.interactive_instruments.etf.bsxm.validator;

import static de.interactive_instruments.etf.bsxm.validator.ValidationReport.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import com.vividsolutions.jts.geom.Coordinate;

import org.basex.query.util.list.ANodeList;
import org.basex.query.value.item.QNm;
import org.basex.query.value.node.FAttr;
import org.basex.query.value.node.FElem;
import org.basex.util.Token;
import org.deegree.geometry.primitive.Point;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class Message {

    private static final QNm MSG_QNM = new QNm(ETF_PREFIX, "message", NS_ETF);
    private static final QNm MSG_REF_QNM = new QNm("ref", NS_ETF);
    private static final byte[] ORIGINAL_ARGUMENT = "original".getBytes();

    private static final NumberFormat COORD_FORMAT = new DecimalFormat("0.000#######",
            new DecimalFormatSymbols(Locale.ENGLISH));

    static String formatValue(double value) {
        return COORD_FORMAT.format(value);
    }

    private static final ResourceBundle bundle = ResourceBundle.getBundle(
            "gmlgeox-messages", Locale.ENGLISH);

    private final String translatedMessage;
    private final String messageId;
    private final String[] arguments;

    private Message(final String messageId, final String[] arguments, final String translatedMessage) {
        this.messageId = "TR." + messageId;
        this.translatedMessage = translatedMessage;
        this.arguments = arguments;
    }

    static Message exception(final Exception e) {
        return new Message("exception." + e.getClass().getName(), null, e.getMessage());
    }

    static Message translate(final String messageId) {
        return new Message(messageId, null, bundle.getString(messageId));
    }

    private static String[] toStrs(final Object[] strs) {
        final String[] newStrs = new String[strs.length];
        for (int i = 0; i < strs.length; i++) {
            newStrs[i] = strs[i] == null ? "" : strs[i].toString();
        }
        return newStrs;
    }

    static Message translate(final String messageId, final Object... arguments) {
        final MessageFormat mf = new MessageFormat(bundle.getString(messageId), bundle.getLocale());
        return new Message(
                messageId,
                toStrs(arguments),
                mf.format(Objects.requireNonNull(arguments,
                        "Message arguments are null")));
    }

    static String formatPoint(final Point point) {
        final StringBuilder sb = new StringBuilder().append(formatValue(point.get0()));
        if (Double.isNaN(point.get1())) {
            return sb.toString();
        }
        if (Double.isNaN(point.get2())) {
            return sb.append(',').append(formatValue(point.get1())).toString();
        } else {
            return sb.append(',').append(formatValue(point.get1())).append(',').append(formatValue(point.get2())).toString();
        }
    }

    static String formatCoordinate(final Coordinate coordinate) {
        final StringBuilder sb = new StringBuilder().append(formatValue(coordinate.x));
        if (Double.isNaN(coordinate.y)) {
            return sb.toString();
        }
        if (Double.isNaN(coordinate.z)) {
            return sb.append(',').append(formatValue(coordinate.y)).toString();
        } else {
            return sb.append(',').append(formatValue(coordinate.y)).append(',').append(formatValue(coordinate.z)).toString();
        }
    }

    ANodeList toNodeList(final FElem affectedCoordinates, final FElem[] location) {
        final ANodeList children;
        final int affectedCoordinatesSize = affectedCoordinates != null ? 1 : 0;
        final int locationSize = location != null ? location.length : 0;
        final int size = affectedCoordinatesSize + locationSize + 1;
        if (this.arguments != null) {
            children = new ANodeList(this.arguments.length + size);
            children.add(ValidationReport.argument(ORIGINAL_ARGUMENT, translatedMessage.getBytes()));
            for (int i = 0; i < this.arguments.length; i++) {
                children.add(ValidationReport.argument(
                        Token.token(i), this.arguments[i].getBytes()));
            }
        } else {
            children = new ANodeList(size).add(ValidationReport.argument(
                    ORIGINAL_ARGUMENT, translatedMessage.getBytes()));
        }
        for (int i = 0; i < locationSize; i++) {
            children.add(location[i]);
        }
        if (affectedCoordinates != null) {
            children.add(affectedCoordinates);
        }

        final ANodeList refAttribute = new ANodeList(1).add(
                new FAttr(MSG_REF_QNM, this.messageId.getBytes()));
        return new ANodeList(1).add(
                new FElem(MSG_QNM, null, children, refAttribute));
    }

    @Override
    public String toString() {
        return translatedMessage;
    }
}
