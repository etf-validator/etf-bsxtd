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

import static de.interactive_instruments.etf.bsxm.validator.Message.*;
import static de.interactive_instruments.etf.bsxm.validator.ValidationReport.ERROR_QNM;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vividsolutions.jts.geom.Coordinate;

import org.basex.query.util.list.ANodeList;
import org.basex.query.value.node.FElem;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.Surface;
import org.deegree.geometry.primitive.patches.PolygonPatch;
import org.deegree.geometry.primitive.patches.SurfacePatch;
import org.deegree.geometry.primitive.segments.CubicSpline;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class ValidationResult {

    private byte result;
    private static byte[] coordinatesArgument = "coordinates".getBytes();
    private final List<FElem> messages;
    private final List<FElem> backupMessages;

    @Contract(pure = true)
    public ValidationResult() {
        this.result = 'V';
        this.messages = new ArrayList<>();
        this.backupMessages = new ArrayList<>();
    }

    void failSilently() {
        this.result = 'F';
    }

    void failWith(@NotNull final Exception e) {
        this.result = 'F';
        final ANodeList children = new ANodeList(1).add(
                Message.exception(e).toNodeList(null, null));
        this.backupMessages.add(new FElem(ERROR_QNM, null, children, null));
    }

    void addError(@NotNull final ElementContext elementContext, @NotNull final Message message) {
        this.result = 'F';
        final ANodeList children = new ANodeList(2).add(
                message.toNodeList(null, elementContext.getContextLocation()));
        this.messages.add(new FElem(ERROR_QNM, null, children, null));
    }

    void addError(@NotNull final ElementContext elementContext, @NotNull final Message message,
            @NotNull final org.deegree.geometry.Geometry affectedGeometry) {
        this.result = 'F';
        final ANodeList children = new ANodeList(3).add(
                message.toNodeList(
                        wrapCoordinates(coordinates(affectedGeometry)),
                        elementContext.getContextLocation()));
        this.messages.add(new FElem(ERROR_QNM, null, children, null));
    }

    void addError(@NotNull final ElementContext elementContext, @NotNull final Message message,
            final com.vividsolutions.jts.geom.Geometry affectedGeometry, final Coordinate coordProblem) {
        this.result = 'F';
        final ANodeList children = message.toNodeList(
                wrapCoordinates(coordinates(affectedGeometry, coordProblem)),
                elementContext.getContextLocation());
        this.messages.add(new FElem(ERROR_QNM, null, children, null));
    }

    private static FElem wrapCoordinates(final String coordinateStr) {
        return ValidationReport.argument(coordinatesArgument, coordinateStr.getBytes());
    }

    private static String coordinates(@NotNull final org.deegree.geometry.Geometry affectedGeometry) {
        if (affectedGeometry instanceof Curve) {
            final Points points = ((Curve) affectedGeometry).getControlPoints();
            return formatPoints(points);
        } else if (affectedGeometry instanceof Point) {
            return formatPoint(((Point) affectedGeometry));
        } else if (affectedGeometry instanceof Surface) {
            try {
                final List<? extends SurfacePatch> patches = ((Surface) affectedGeometry).getPatches();
                if (patches == null) {
                    return "";
                }
                final SurfacePatch p = patches.get(0);
                if (!(p instanceof PolygonPatch)) {
                    return "";
                }
                final PolygonPatch pp = (PolygonPatch) p;
                if (pp.getExteriorRing().getCurveSegments().get(0) instanceof CubicSpline) {
                    return formatPoints(((CubicSpline) pp.getExteriorRing().getMembers().get(0).getCurveSegments().get(0))
                            .getControlPoints());
                } else {
                    return formatPoints(((PolygonPatch) p).getExteriorRing().getControlPoints());
                }
            } catch (IllegalArgumentException e) {
                return "";
            }
        } else {
            Objects.requireNonNull(affectedGeometry, "Invalid call, affected points are null");
            throw new IllegalArgumentException(
                    "Cannot derive affected points from geometry type " + affectedGeometry.getClass().getName());
        }
    }

    private static String formatPoints(final Points points) {
        final StringBuilder builder = new StringBuilder(points.size() * points.getDimension() * 12);
        if (points.getDimension() == 3) {
            for (Point point : points) {
                builder.append(formatValue(point.get0())).append(',')
                        .append(formatValue(point.get1())).append(',')
                        .append(formatValue(point.get2())).append(' ');
            }
        } else {
            for (Point point : points) {
                builder.append(formatValue(point.get0())).append(',')
                        .append(formatValue(point.get1())).append(' ');
            }
        }
        if (builder.length() > 60) {
            builder.setLength(60);
            builder.append("...");
        } else {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    private static String coordinates(final com.vividsolutions.jts.geom.Geometry geometry, final Coordinate coordProblem) {
        if (coordProblem != null) {
            return formatCoordinate(coordProblem);
        }
        if (geometry != null && geometry.getCoordinates().length > 0) {
            final Coordinate[] coordinates = geometry.getCoordinates();
            final Coordinate coordBegin = coordinates[0];
            final Coordinate coordEnd = coordinates[coordinates.length - 1];
            return formatCoordinate(coordBegin) + " " + formatCoordinate(coordEnd);
        }
        return "";
    }

    public byte getResult() {
        return result;
    }

    public List<FElem> getMessages() {
        if (messages.isEmpty()) {
            return backupMessages;
        }
        return messages;
    }
}
