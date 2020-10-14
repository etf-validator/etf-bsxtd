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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.cs.persistence.CRSManager;
import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.segments.ArcString;
import org.deegree.geometry.primitive.segments.Circle;
import org.deegree.geometry.primitive.segments.LineStringSegment;
import org.deegree.geometry.standard.AbstractDefaultGeometry;
import org.deegree.geometry.standard.JTSGeometryPair;
import org.junit.jupiter.api.Test;

import de.interactive_instruments.etf.bsxm.geometry.IIGeometryFactory;

public final class DegreeArcTest {

    @Test
    public void testIntersects2() {
        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point arc1P1 = geomFactory.createPoint("arc1P1", 1.0, 0.0, crs);
        final Point arc1P2 = geomFactory.createPoint("arc1P2", 0.0, 1.0, crs);
        final Point arc1P3 = geomFactory.createPoint("arc1P3", -1.0, 0.0, crs);
        final List<Point> arcPointList = new ArrayList<>();
        arcPointList.add(arc1P1);
        arcPointList.add(arc1P2);
        arcPointList.add(arc1P3);
        final Curve arcCurve = geomFactory.createCurve("arc", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList)));

        final Point lsP1 = geomFactory.createPoint("lsP1", -1.0, 1.0, crs);
        final Point lsP2 = geomFactory.createPoint("lsP2", 1.0, 1.0, crs);
        final List<Point> lsPointList = new ArrayList<>();
        lsPointList.add(lsP1);
        lsPointList.add(lsP2);
        final Curve lsCurve = geomFactory.createCurve("LS", crs,
                geomFactory.createLineStringSegment(geomFactory.createPoints(lsPointList)));

        // final JTSGeometryPair jtsGeoms = JTSGeometryPair.createCompatiblePair((AbstractDefaultGeometry) arcCurve,
        // lsCurve);

        // System.out.println(jtsGeoms.first);
        // System.out.println(jtsGeoms.second);

        assertTrue(arcCurve.intersects(lsCurve));
    }

    @Test
    public void testIntersects3() {
        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point arc1P1 = geomFactory.createPoint("arc1P1", 1.0, 0.0, crs);
        final Point arc1P2 = geomFactory.createPoint("arc1P2", 0.0, 1.0, crs);
        final Point arc1P3 = geomFactory.createPoint("arc1P3", -1.0, 0.0, crs);
        final List<Point> arcPointList = new ArrayList<>();
        arcPointList.add(arc1P1);
        arcPointList.add(arc1P2);
        arcPointList.add(arc1P3);
        final Curve arcCurve = geomFactory.createCurve("arc", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList)));

        final Point lsP1 = geomFactory.createPoint("lsP1", 0.378310065149095, 1.094648792904952, crs);
        final Point lsP2 = geomFactory.createPoint("lsP2", 1.093793305137216, 0.260974580191861, crs);
        final List<Point> lsPointList = new ArrayList<>();
        lsPointList.add(lsP1);
        lsPointList.add(lsP2);
        final Curve lsCurve = geomFactory.createCurve("LS", crs,
                geomFactory.createLineStringSegment(geomFactory.createPoints(lsPointList)));

        assertTrue(arcCurve.intersects(lsCurve));
    }

    @Test
    public void testIntersects4() {
        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point arc1P1 = geomFactory.createPoint("arc1P1", 1.0, 0.0, crs);
        final Point arc1P2 = geomFactory.createPoint("arc1P2", 0.0, 1.0, crs);
        final Point arc1P3 = geomFactory.createPoint("arc1P3", -1.0, 0.0, crs);
        final List<Point> arcPointList = new ArrayList<>();
        arcPointList.add(arc1P1);
        arcPointList.add(arc1P2);
        arcPointList.add(arc1P3);
        final Curve arcCurve = geomFactory.createCurve("arc", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList)));

        final Point lsP1 = geomFactory.createPoint("lsP1", 0.635092918570922, 0.777252403790242, crs);
        final Point lsP2 = geomFactory.createPoint("lsP2", 0.468543903140246, 0.890943937416946, crs);
        final List<Point> lsPointList = new ArrayList<>();
        lsPointList.add(lsP1);
        lsPointList.add(lsP2);
        final Curve lsCurve = geomFactory.createCurve("LS", crs,
                geomFactory.createLineStringSegment(geomFactory.createPoints(lsPointList)));

        assertFalse(lsCurve.intersects(arcCurve));
    }

    @Test
    public void testEquals_ArcString() {
        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point center = geomFactory.createPoint("center", 0.0, 1.0, crs);

        final Point arc1P1 = geomFactory.createPoint("arc1P1", 1.0, 0.0, crs);
        final Point arc1P3 = geomFactory.createPoint("arc1P3", -1.0, 0.0, crs);
        final List<Point> arcPointList1 = new ArrayList<>();
        arcPointList1.add(arc1P1);
        arcPointList1.add(center);
        arcPointList1.add(arc1P3);
        final Curve arcCurve1_ccw = geomFactory.createCurve("arc1", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList1)));

        final Point arc2P1 = geomFactory.createPoint("arc2P1", -1.0, 0.0, crs);
        final Point arc2P3 = geomFactory.createPoint("arc2P3", 1.0, 0.0, crs);
        final List<Point> arcPointList2 = new ArrayList<>();
        arcPointList2.add(arc2P1);
        arcPointList2.add(center);
        arcPointList2.add(arc2P3);
        final Curve arcCurve2_cw = geomFactory.createCurve("arc2", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList2)));

        // Control point check
        assertTrue(arcCurve1_ccw.equals(arcCurve2_cw));
        final JTSGeometryPair jtsGeoms = JTSGeometryPair.createCompatiblePair((AbstractDefaultGeometry) arcCurve1_ccw,
                arcCurve2_cw);
        // Check linearization
        assertTrue(jtsGeoms.first.equalsNorm(jtsGeoms.second));
        assertTrue(jtsGeoms.first.equals(jtsGeoms.second));
    }

    @Test
    public void testLsOrientation() {
        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point a = geomFactory.createPoint("a", 2.0, 2.0, crs);
        final Point b = geomFactory.createPoint("b", 1.0, 1.0, crs);
        final Point c = geomFactory.createPoint("c", 1.0, 3.0, crs);

        final List<Point> ls_a_b_c = new ArrayList<>();
        ls_a_b_c.add(a);
        ls_a_b_c.add(b);
        ls_a_b_c.add(c);

        final Curve curveCw = geomFactory.createCurve("curveCw", crs,
                geomFactory.createLineStringSegment(geomFactory.createPoints(ls_a_b_c)));

        final List<Point> ls_c_b_a = new ArrayList<>();
        ls_c_b_a.add(c);
        ls_c_b_a.add(b);
        ls_c_b_a.add(a);

        final Curve curveCcw = geomFactory.createCurve("curveCcw", crs,
                geomFactory.createLineStringSegment(geomFactory.createPoints(ls_c_b_a)));
    }

    @Test
    public void testArcOrientation() {
        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point center = geomFactory.createPoint("arc_center", 2.0, 2.0, crs);
        final Point b = geomFactory.createPoint("arc_b", 1.0, 1.0, crs);
        final Point c = geomFactory.createPoint("arc_c", 1.0, 3.0, crs);

        final List<Point> arc_1_ccw = new ArrayList<>();
        arc_1_ccw.add(b);
        arc_1_ccw.add(center);
        arc_1_ccw.add(c);

        final Curve curveCcw = geomFactory.createCurve("curveCcw", crs,
                geomFactory.createArcString(geomFactory.createPoints(arc_1_ccw)));

        final List<Point> arc_2_cw = new ArrayList<>();
        arc_2_cw.add(c);
        arc_2_cw.add(center);
        arc_2_cw.add(b);

        final Curve curveCw = geomFactory.createCurve("curveCw", crs,
                geomFactory.createArcString(geomFactory.createPoints(arc_2_cw)));
    }

    @Test
    public void testCurveInterpolation_with_LineStringSegment_and_Arcs() {
        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point center = geomFactory.createPoint("arc_center", 2.0, 2.0, crs);
        final Point b = geomFactory.createPoint("arc_b", 1.0, 1.0, crs);
        final Point c = geomFactory.createPoint("arc_c", 1.0, 3.0, crs);

        final List<Point> arc_1_ccw = new ArrayList<>();
        arc_1_ccw.add(b);
        arc_1_ccw.add(center);
        arc_1_ccw.add(c);

        final List<Point> ls_c_a = new ArrayList<>();
        ls_c_a.add(c);
        ls_c_a.add(center);

        final List<Point> ls_a_b = new ArrayList<>();
        ls_a_b.add(center);
        ls_a_b.add(b);

        final Curve arcCurve1 = geomFactory.createCurve("arc_1_ccw", crs,
                geomFactory.createLineStringSegment(geomFactory.createPoints(ls_c_a)),
                geomFactory.createLineStringSegment(geomFactory.createPoints(ls_a_b)),
                geomFactory.createArcString(geomFactory.createPoints(arc_1_ccw)));
    }

    @Test
    public void testEquals_Circle() {

        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point g1P1 = geomFactory.createPoint("g1P1", 1.0, 0.0, crs);
        final Point g1P2 = geomFactory.createPoint("g1P2", 0.0, 1.0, crs);
        final Point g1P3 = geomFactory.createPoint("g1P3", -1.0, 0.0, crs);
        Circle circle1 = geomFactory.createCircle(g1P1, g1P2, g1P3);

        final Curve arcCurve1 = geomFactory.createCurve("c1", crs, circle1);

        final Point g2P1 = geomFactory.createPoint("g2P1", -1.0, 0.0, crs);
        final Point g2P2 = geomFactory.createPoint("g2P2", 0.0, 1.0, crs);
        final Point g2P3 = geomFactory.createPoint("g2P3", 1.0, 0.0, crs);
        Circle circle2 = geomFactory.createCircle(g2P1, g2P2, g2P3);

        final Curve arcCurve2 = geomFactory.createCurve("c2", crs, circle2);

        assertTrue(arcCurve1.equals(arcCurve2));
    }

    @Test
    public void testNotEqual() {

        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point center = geomFactory.createPoint("center", 0.0, 1.0, crs);

        final Point arc1P1 = geomFactory.createPoint("arc1P1", 1.0, 1.0, crs);
        final Point arc1P3 = geomFactory.createPoint("arc1P3", -1.0, 0.0, crs);
        final List<Point> arcPointList1 = new ArrayList<>();
        arcPointList1.add(arc1P1);
        arcPointList1.add(center);
        arcPointList1.add(arc1P3);
        final Curve arcCurve1 = geomFactory.createCurve("arc1", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList1)));

        final Point arc2P1 = geomFactory.createPoint("arc2P1", -1.0, -1.0, crs);
        final Point arc2P2 = geomFactory.createPoint("arc2P2", 0.0, 1.0, crs);
        final Point arc2P3 = geomFactory.createPoint("arc2P3", 1.0, 0.0, crs);
        final List<Point> arcPointList2 = new ArrayList<>();
        arcPointList2.add(arc2P1);
        arcPointList2.add(arc2P2);
        arcPointList2.add(arc2P3);
        final Curve arcCurve2 = geomFactory.createCurve("arc2", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList2)));

        // Control point check
        assertFalse(arcCurve1.equals(arcCurve2));
        final JTSGeometryPair jtsGeoms = JTSGeometryPair.createCompatiblePair((AbstractDefaultGeometry) arcCurve1,
                arcCurve2);
        // Check linearization
        assertFalse(jtsGeoms.first.equalsNorm(jtsGeoms.second));
        assertFalse(jtsGeoms.first.equals(jtsGeoms.second));
    }

    @Test
    public void testOverlaps1() {

        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point arc1P1 = geomFactory.createPoint("arc1P1", 1.0, 0.0, crs);
        final Point arc1P2 = geomFactory.createPoint("arc1P2", 0.0, 1.0, crs);
        final Point arc1P3 = geomFactory.createPoint("arc1P3", -1.0, 0.0, crs);
        final List<Point> arcPointList1 = new ArrayList<>();
        arcPointList1.add(arc1P1);
        arcPointList1.add(arc1P2);
        arcPointList1.add(arc1P3);

        final Point ls1P1 = geomFactory.createPoint("line1P1", -1.0, 0.0, crs);
        final Point ls1P2 = geomFactory.createPoint("line1P2", -1.0, -1.0, crs);
        final List<Point> line1PointList = new ArrayList<>();
        line1PointList.add(ls1P1);
        line1PointList.add(ls1P2);
        LineStringSegment line1 = geomFactory.createLineStringSegment(geomFactory.createPoints(line1PointList));

        final Curve curve1 = geomFactory.createCurve("c1", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList1)), line1);

        final Point arc2P1 = geomFactory.createPoint("arc2P1", -1.0, 0.0, crs);
        final Point arc2P2 = geomFactory.createPoint("arc2P2", 0.0, 1.0, crs);
        final Point arc2P3 = geomFactory.createPoint("arc2P3", 1.0, 0.0, crs);
        final List<Point> arcPointList2 = new ArrayList<>();
        arcPointList2.add(arc2P1);
        arcPointList2.add(arc2P2);
        arcPointList2.add(arc2P3);

        final Point ls2P1 = geomFactory.createPoint("line2P1", 1.0, 0.0, crs);
        final Point ls2P2 = geomFactory.createPoint("line2P2", 1.0, -1.0, crs);
        final List<Point> line2PointList = new ArrayList<>();
        line2PointList.add(ls2P1);
        line2PointList.add(ls2P2);
        LineStringSegment line2 = geomFactory.createLineStringSegment(geomFactory.createPoints(line2PointList));

        final Curve curve2 = geomFactory.createCurve("c2", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList2)), line2);

        assertTrue(curve1.overlaps(curve2));
    }

    /**
     * Check overlap when arc strings are actually line strings because the control points of the arcs are collinear.
     */
    @Test
    public void testOverlaps2_collinearPoints() {

        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        /* Collinear points */
        final Point arc1P1 = geomFactory.createPoint("arc1P1", 1.0, 0.0, crs);
        final Point arc1P2 = geomFactory.createPoint("arc1P2", 0.0, 0.0, crs);
        final Point arc1P3 = geomFactory.createPoint("arc1P3", -1.0, 0.0, crs);
        final List<Point> arcPointList1 = new ArrayList<>();
        arcPointList1.add(arc1P1);
        arcPointList1.add(arc1P2);
        arcPointList1.add(arc1P3);

        final Point ls1P1 = geomFactory.createPoint("line1P1", -1.0, 0.0, crs);
        final Point ls1P2 = geomFactory.createPoint("line1P2", -1.0, -1.0, crs);
        final List<Point> line1PointList = new ArrayList<>();
        line1PointList.add(ls1P1);
        line1PointList.add(ls1P2);
        LineStringSegment line1 = geomFactory.createLineStringSegment(geomFactory.createPoints(line1PointList));

        final Curve curve1 = geomFactory.createCurve("c1", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList1)), line1);

        /* Collinear points */
        final Point arc2P1 = geomFactory.createPoint("arc2P1", -1.0, 0.0, crs);
        final Point arc2P2 = geomFactory.createPoint("arc2P2", 0.0, 0.0, crs);
        final Point arc2P3 = geomFactory.createPoint("arc2P3", 1.0, 0.0, crs);
        final List<Point> arcPointList2 = new ArrayList<>();
        arcPointList2.add(arc2P1);
        arcPointList2.add(arc2P2);
        arcPointList2.add(arc2P3);

        final Point ls2P1 = geomFactory.createPoint("line2P1", 1.0, 0.0, crs);
        final Point ls2P2 = geomFactory.createPoint("line2P2", 1.0, -1.0, crs);
        final List<Point> line2PointList = new ArrayList<>();
        line2PointList.add(ls2P1);
        line2PointList.add(ls2P2);
        LineStringSegment line2 = geomFactory.createLineStringSegment(geomFactory.createPoints(line2PointList));

        final Curve curve2 = geomFactory.createCurve("c2", crs,
                geomFactory.createArcString(geomFactory.createPoints(arcPointList2)), line2);

        assertTrue(curve1.overlaps(curve2));
    }

    @Test
    public void testContains() {

        final ICRS crs = CRSManager.getCRSRef("EPSG:25832");
        final IIGeometryFactory geomFactory = new IIGeometryFactory();

        final Point ls1P1 = geomFactory.createPoint("line1P1", 1.0, -1.0, crs);
        final Point ls1P2 = geomFactory.createPoint("line1P2", 1.0, 0.0, crs);
        final List<Point> line1PointList = new ArrayList<>();
        line1PointList.add(ls1P1);
        line1PointList.add(ls1P2);
        LineStringSegment line1 = geomFactory.createLineStringSegment(geomFactory.createPoints(line1PointList));

        final Point center = geomFactory.createPoint("center", 0.0, 1.0, crs);
        final Point arc1P1 = geomFactory.createPoint("arc1P1", 1.0, 0.0, crs);
        final Point arc1P3 = geomFactory.createPoint("arc1P3", -1.0, 0.0, crs);
        final List<Point> arcPointList1 = new ArrayList<>();
        arcPointList1.add(arc1P1);
        arcPointList1.add(center);
        arcPointList1.add(arc1P3);
        ArcString arcString1 = geomFactory.createArcString(geomFactory.createPoints(arcPointList1));

        final Point ls2P1 = geomFactory.createPoint("line2P1", -1.0, 0.0, crs);
        final Point ls2P2 = geomFactory.createPoint("line2P2", -1.0, -1.0, crs);
        final List<Point> line2PointList = new ArrayList<>();
        line2PointList.add(ls2P1);
        line2PointList.add(ls2P2);
        LineStringSegment line2 = geomFactory.createLineStringSegment(geomFactory.createPoints(line2PointList));
        final Curve curve1 = geomFactory.createCurve("curve1_ls_arc_ls", crs, line1, arcString1, line2);

        final Curve curveArcString1 = geomFactory.createCurve("curve2_arc", crs, arcString1);
        assertTrue(curve1.contains(curveArcString1));

        final Point arc2P1 = geomFactory.createPoint("arc2P1", -1.0, 0.0, crs);
        final Point arc2P2 = geomFactory.createPoint("arc2P2", 0.0, 1.0, crs);
        final Point arc2P3 = geomFactory.createPoint("arc2P3", 1.0, 0.0, crs);
        final List<Point> arcPointList2 = new ArrayList<>();
        arcPointList2.add(arc2P1);
        arcPointList2.add(arc2P2);
        arcPointList2.add(arc2P3);
        ArcString arcString2 = geomFactory.createArcString(geomFactory.createPoints(arcPointList2));

        final Curve curveArcString2 = geomFactory.createCurve("curveArcString2", crs, arcString2);
        assertTrue(curve1.contains(curveArcString2));

        final Curve curve_line1 = geomFactory.createCurve("curveLine1", crs, line1);
        assertTrue(curve1.contains(curve_line1));

        final Curve curve_line2 = geomFactory.createCurve("curveLine2", crs, line2);
        assertTrue(curve1.contains(curve_line2));
    }

}
