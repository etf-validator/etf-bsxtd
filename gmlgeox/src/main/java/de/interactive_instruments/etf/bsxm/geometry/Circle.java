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
package de.interactive_instruments.etf.bsxm.geometry;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot> de)
 *
 */
public class Circle {

    private static final double TOLERANCE = 0.0000001;

    private Coordinate center;
    private double radius;

    public Circle(Coordinate center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    public static Circle from3Coordinates(Coordinate c1, Coordinate c2,
            Coordinate c3) throws InvalidCircleException {

        // see http://paulbourke.net/geometry/circlesphere/

        final Coordinate center;

        if (!checkPerpendicular(c1, c2, c3)) {

            center = calculateCenter(c1, c2, c3);

        } else if (!checkPerpendicular(c1, c3, c2)) {

            center = calculateCenter(c1, c3, c2);

        } else if (!checkPerpendicular(c2, c1, c3)) {

            center = calculateCenter(c2, c1, c3);

        } else if (!checkPerpendicular(c2, c3, c1)) {

            center = calculateCenter(c2, c3, c1);

        } else if (!checkPerpendicular(c3, c1, c2)) {

            center = calculateCenter(c3, c1, c2);

        } else if (!checkPerpendicular(c3, c2, c1)) {

            center = calculateCenter(c3, c2, c1);

        } else {

            throw new InvalidCircleException(
                    "Control coordinates are collinear and thus do not form a circle.");
        }

        double radius = Math.sqrt(
                Math.pow(center.x - c1.x, 2) + Math.pow(center.y - c1.y, 2));

        return new Circle(center, radius);
    }

    /**
     * Checks if the lines c1-c2 and c2-c3 are perpendicular to the x or y axis, using a certain tolerance. Takes into
     * account that there is a simple solution (for calculating the center of the circle) for the case of c1-c2 being
     * vertical and c2-c3 being horizontal.
     *
     * @param c1
     * @param c2
     * @param c3
     * @return <code>false</code> if c1-c2 is vertical and c2-c3 is horizontal (then there is a simple solution) or if none
     *         of the two lines is vertical or horizontal, else <code>true</code>
     */
    private static boolean checkPerpendicular(Coordinate c1, Coordinate c2,
            Coordinate c3) {

        double yDelta_a = c2.y - c1.y;
        double xDelta_a = c2.x - c1.x;
        double yDelta_b = c3.y - c2.y;
        double xDelta_b = c3.x - c2.x;

        if (Math.abs(xDelta_a) <= TOLERANCE
                && Math.abs(yDelta_b) <= TOLERANCE) {
            // line c1-c2 is vertical, line c2-c3 is horizontal
            return false;
        }

        // one of the lines c1-c2, c2-c3 is perpendicular to x or y axis
        return Math.abs(yDelta_a) <= TOLERANCE || Math.abs(xDelta_a) <= TOLERANCE
                || Math.abs(yDelta_b) <= TOLERANCE
                || Math.abs(xDelta_b) <= TOLERANCE;
    }

    private static Coordinate calculateCenter(Coordinate c1, Coordinate c2,
            Coordinate c3) throws InvalidCircleException {

        double yDelta_a = c2.y - c1.y;
        double xDelta_a = c2.x - c1.x;
        double yDelta_b = c3.y - c2.y;
        double xDelta_b = c3.x - c2.x;

        if (Math.abs(xDelta_a) <= TOLERANCE
                && Math.abs(yDelta_b) <= TOLERANCE) {

            // line c1-c2 is vertical, line c2-c3 is horizontal
            double centerX = 0.5 * (c2.x + c3.x);
            double centerY = 0.5 * (c1.y + c2.y);
            return new Coordinate(centerX, centerY);

        } else {

            double aSlope = yDelta_a / xDelta_a;
            double bSlope = yDelta_b / xDelta_b;

            if (Math.abs(aSlope - bSlope) <= TOLERANCE) {
                throw new InvalidCircleException(
                        "Control coordinates are collinear and thus do not form a circle.");
            }

            double centerX = (aSlope * bSlope * (c1.y - c3.y)
                    + bSlope * (c1.x + c2.x) - aSlope * (c2.x + c3.x))
                    / (2 * (bSlope - aSlope));
            double centerY = -1 * (centerX - (c1.x + c2.x) / 2) / aSlope
                    + (c1.y + c2.y) / 2;
            return new Coordinate(centerX, centerY);
        }

    }

    public Coordinate center() {
        return center;
    }

    public double radius() {
        return radius;
    }
}
