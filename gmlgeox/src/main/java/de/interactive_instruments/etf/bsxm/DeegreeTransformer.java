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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;
import org.basex.query.value.Value;
import org.basex.query.value.node.ANode;
import org.deegree.commons.xml.stax.XMLStreamReaderWrapper;
import org.deegree.cs.CRSCodeType;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.cs.exceptions.UnknownCRSException;
import org.deegree.cs.persistence.CRSManager;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.composite.CompositeCurve;
import org.deegree.geometry.composite.CompositeGeometry;
import org.deegree.geometry.multi.MultiGeometry;
import org.deegree.geometry.multi.MultiPoint;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.LineString;
import org.deegree.geometry.primitive.OrientableCurve;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.Ring;
import org.deegree.geometry.primitive.Solid;
import org.deegree.geometry.primitive.Surface;
import org.deegree.geometry.primitive.patches.PolygonPatch;
import org.deegree.geometry.primitive.patches.SurfacePatch;
import org.deegree.geometry.primitive.segments.CurveSegment;
import org.deegree.geometry.standard.curvesegments.DefaultArc;
import org.deegree.geometry.standard.curvesegments.DefaultArcByBulge;
import org.deegree.geometry.standard.curvesegments.DefaultArcString;
import org.deegree.geometry.standard.curvesegments.DefaultArcStringByBulge;
import org.deegree.geometry.standard.curvesegments.DefaultBSpline;
import org.deegree.geometry.standard.curvesegments.DefaultBezier;
import org.deegree.geometry.standard.curvesegments.DefaultCubicSpline;
import org.deegree.geometry.standard.curvesegments.DefaultGeodesic;
import org.deegree.geometry.standard.curvesegments.DefaultGeodesicString;
import org.deegree.geometry.standard.curvesegments.DefaultLineStringSegment;
import org.deegree.geometry.standard.points.PointsPoints;
import org.deegree.geometry.standard.points.PointsSubsequence;
import org.deegree.geometry.standard.primitive.DefaultCurve;
import org.deegree.gml.GMLInputFactory;
import org.deegree.gml.GMLStreamReader;
import org.deegree.gml.GMLVersion;
import org.jetbrains.annotations.NotNull;

import de.interactive_instruments.IFile;
import de.interactive_instruments.IoUtils;
import de.interactive_instruments.etf.bsxm.geometry.GeometricPoint;
import de.interactive_instruments.etf.bsxm.geometry.IIGeometryFactory;
import de.interactive_instruments.etf.bsxm.parser.BxNamespaceHolder;
import de.interactive_instruments.etf.bsxm.parser.DBNodeStreamReader;
import de.interactive_instruments.properties.PropertyUtils;

/**
 * Transforms Geometry objects into Deegree Geometry objects
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class DeegreeTransformer {

    public static final String ETF_GMLGEOX_SRSCONFIG_DIR = "etf.gmlgeox.srsconfig.dir";

    private final IIGeometryFactory geometryFactory;
    private final BxNamespaceHolder namespaceHolder;
    private final SrsLookup srsLookup;

    DeegreeTransformer(final IIGeometryFactory deegreeGeomFac, final BxNamespaceHolder namespaceHolder,
            final SrsLookup srsLookup) {
        this.geometryFactory = deegreeGeomFac;
        this.namespaceHolder = namespaceHolder;
        this.srsLookup = srsLookup;

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            if (CRSManager.get("default") == null || CRSManager.get("default")
                    .getCRSByCode(CRSCodeType.valueOf("http://www.opengis.net/def/crs/EPSG/0/5555")) == null) {
                loadGmlGeoXSrsConfiguration();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private void loadGmlGeoXSrsConfiguration() {
        final String srsConfigDirPath = PropertyUtils.getenvOrProperty(ETF_GMLGEOX_SRSCONFIG_DIR, null);
        final CRSManager crsMgr = new CRSManager();
        /*
         * If the configuration for EPSG 5555 can be accessed, the CRSManger is already configured by the test driver.
         */
        if (srsConfigDirPath != null) {
            final IFile srsConfigDirectory = new IFile(srsConfigDirPath, ETF_GMLGEOX_SRSCONFIG_DIR);

            try {
                srsConfigDirectory.expectDirIsWritable();
                crsMgr.init(srsConfigDirectory);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Could not load SRS configuration files from directory referenced from GmlGeoX property '"
                                + ETF_GMLGEOX_SRSCONFIG_DIR + "'. Reference is: " + srsConfigDirPath
                                + " Exception message is: " + e.getMessage(),
                        e);
            }
        } else {
            try {
                /*
                 * We use the same folder each time an instance of GmlGeoX is created. The configuration files will not be
                 * deleted upon exit. That shouldn't be a problem since we always use the same folder.
                 */
                final String tempDirPath = System.getProperty("java.io.tmpdir");
                final File tempDir = new File(tempDirPath, "gmlGeoXSrsConfig");

                if (tempDir.exists()) {
                    FileUtils.deleteQuietly(tempDir);
                }
                tempDir.mkdirs();

                IoUtils.copyResourceToFile(this, "/srsconfig/default.xml", new IFile(tempDir, "default.xml"));
                IoUtils.copyResourceToFile(this, "/srsconfig/deegree/d3/config/ntv2/beta2007.gsb",
                        new IFile(tempDir, "deegree/d3/config/ntv2/beta2007.gsb"));
                IoUtils.copyResourceToFile(this, "/srsconfig/deegree/d3/parser-files.xml",
                        new IFile(tempDir, "deegree/d3/parser-files.xml"));
                IoUtils.copyResourceToFile(this, "/srsconfig/deegree/d3/config/crs-definitions.xml",
                        new IFile(tempDir, "deegree/d3/config/crs-definitions.xml"));
                IoUtils.copyResourceToFile(this, "/srsconfig/deegree/d3/config/datum-definitions.xml",
                        new IFile(tempDir, "deegree/d3/config/datum-definitions.xml"));
                IoUtils.copyResourceToFile(this, "/srsconfig/deegree/d3/config/ellipsoid-definitions.xml",
                        new IFile(tempDir, "deegree/d3/config/ellipsoid-definitions.xml"));
                IoUtils.copyResourceToFile(this, "/srsconfig/deegree/d3/config/pm-definitions.xml",
                        new IFile(tempDir, "deegree/d3/config/pm-definitions.xml"));
                IoUtils.copyResourceToFile(this, "/srsconfig/deegree/d3/config/projection-definitions.xml",
                        new IFile(tempDir, "deegree/d3/config/projection-definitions.xml"));
                IoUtils.copyResourceToFile(this, "/srsconfig/deegree/d3/config/transformation-definitions.xml",
                        new IFile(tempDir, "deegree/d3/config/transformation-definitions.xml"));

                crsMgr.init(tempDir);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Exception occurred while extracting the SRS configuration files provided by GmlGeoX to a temporary "
                                + "directory and loading them from there. Exception message is: " + e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Reads a geometry from the given nod.
     *
     * @param aNode
     *            represents a GML geometry element
     * @return the geometry represented by the node
     * @throws GmlGeoXException
     *             tbd
     */
    public Geometry parseGeometry(ANode aNode) throws GmlGeoXException {

        final String namespaceURI = new String(aNode.qname().uri());

        if (namespaceURI == null
                || (!IIGmlConstants.isGML32Namespace(namespaceURI) && !IIGmlConstants.isGML31Namespace(namespaceURI))) {

            throw new GmlGeoXException("Cannot identify GML version from namespace '"
                    + (namespaceURI == null ? "<null>" : namespaceURI) + "'.");
        }

        final GMLVersion gmlVersion;
        if (IIGmlConstants.isGML32Namespace(namespaceURI)) {
            gmlVersion = GMLVersion.GML_32;
        } else if (IIGmlConstants.isGML31Namespace(namespaceURI)) {
            gmlVersion = GMLVersion.GML_31;
        } else {
            // cannot happen because we checked before
            throw new IllegalStateException();
        }

        final ICRS crs = srsLookup.getSrsForGeometryNode(aNode);
        final XMLStreamReader xmlStream = nodeToStreamReader(aNode);

        try {
            final GMLStreamReader gmlStream = GMLInputFactory.createGMLStreamReader(gmlVersion, xmlStream);
            gmlStream.setGeometryFactory(geometryFactory);
            gmlStream.setDefaultCRS(crs);
            return gmlStream.readGeometry();
        } catch (final XMLStreamException | UnknownCRSException e) {
            throw new GmlGeoXException(e);
        }

    }

    public IIGeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    XMLStreamReader nodeToStreamReader(final ANode node) {
        final String systemId = new IFile(node.data().meta.original).getName();
        return new XMLStreamReaderWrapper(new DBNodeStreamReader(node, this.namespaceHolder), systemId);
    }

    /**
     * Retrieves all basic curve components from the given geometry. Composite geometries - including curves - will be
     * broken up into their parts. Point based geometries will be ignored.
     *
     * @param geom
     *            tbd
     * @return A list with the curve components of the given geometry. Can be empty but not <code>null</code>.
     * @throws GmlGeoXException
     *             tbd
     */
    public Collection<Curve> getCurveComponents(final @NotNull Geometry geom) throws GmlGeoXException {
        if (geom instanceof DefaultCurve) {
            return Collections.singleton((DefaultCurve) geom);
        } else if (geom instanceof CompositeCurve) {
            final CompositeCurve cc = (CompositeCurve) geom;
            final List<Curve> result = new ArrayList<>(cc.size());
            for (int i = 0; i < cc.size(); i++) {
                result.addAll(getCurveComponents(cc.get(i)));
            }
            return result;
        } else if (geom instanceof LineString) {
            return Collections.singleton((LineString) geom);
        } else if (geom instanceof OrientableCurve) {
            /*
             * 2015-12-14 JE: special treatment is necessary because OrientableCurve.getJTSGeometry() returns null (with code
             * from deegree 3.4-pre22-SNAPSHOT).
             */
            final OrientableCurve oc = (OrientableCurve) geom;
            final Curve baseCurve = oc.getBaseCurve();
            return getCurveComponents(baseCurve);
        } else if (geom instanceof Ring) {
            final Ring r = (Ring) geom;
            final List<Curve> result = new ArrayList<>(r.getMembers().size());
            for (Curve c : r.getMembers()) {
                result.addAll(getCurveComponents(c));
            }
            return result;
        } else if (geom instanceof Surface) {
            // covers CompositeSurface, OrientableSurface, Polygon, ...
            final Surface s = (Surface) geom;
            final List<? extends SurfacePatch> patches = s.getPatches();
            final List<Curve> result = new ArrayList<>();
            for (SurfacePatch sp : patches) {
                final List<? extends Ring> boundaryRings;
                if (sp instanceof PolygonPatch) {
                    boundaryRings = ((PolygonPatch) sp).getBoundaryRings();
                } else {
                    throw new UnsupportedGeometryTypeException(
                            "Surface contains a surface patch that is not a polygon patch, a rectangle, or a triangle.");
                }
                for (Ring r : boundaryRings) {
                    result.addAll(getCurveComponents(r));
                }
            }
            return result;
        } else if (geom instanceof Point || geom instanceof MultiPoint) {
            // ignore
            return Collections.emptyList();
        } else if (geom instanceof Solid) {
            final Solid s = (Solid) geom;
            final List<Curve> result = new ArrayList<>();
            if (s.getExteriorSurface() != null) {
                result.addAll(getCurveComponents(s.getExteriorSurface()));
            }
            for (Surface surface : s.getInteriorSurfaces()) {
                result.addAll(getCurveComponents(surface));
            }
            return result;
        } else if (geom instanceof MultiGeometry) {
            @SuppressWarnings("rawtypes")
            final MultiGeometry mg = (MultiGeometry) geom;
            final List<Curve> result = new ArrayList<>(mg.size());
            for (int i = 0; i < mg.size(); i++) {
                result.addAll(getCurveComponents((Geometry) mg.get(i)));
            }
            return result;
        } else if (geom instanceof CompositeGeometry) {
            @SuppressWarnings("rawtypes")
            final CompositeGeometry cg = (CompositeGeometry) geom;
            final List<Curve> result = new ArrayList<>(cg.size());
            for (int i = 0; i < cg.size(); i++) {
                result.addAll(getCurveComponents((Geometry) cg.get(i)));
            }
            return result;
        } else {
            throw new GmlGeoXException("Determination of curve components for deegree geometry type '"
                    + geom.getClass().getName() + "' is not supported.");
        }
    }

    public ArrayList<ArrayList<Point>> getSetsOfContinuousLinearCurveSegments(final @NotNull ANode geomNode)
            throws GmlGeoXException {

        final Geometry geom_deegree = parseGeometry(geomNode);
        return getSetsOfContinuousLinearCurveSegments(geom_deegree);
    }

    public ArrayList<ArrayList<Point>> getSetsOfContinuousLinearCurveSegments(final @NotNull Geometry geom)
            throws GmlGeoXException {

        ArrayList<ArrayList<Point>> result_tmp = new ArrayList<>();

        if (geom instanceof DefaultCurve) {

            DefaultCurve curve = (DefaultCurve) geom;

            ArrayList<Point> points = new ArrayList<>();

            for (CurveSegment cs : curve.getCurveSegments()) {

                if (cs instanceof DefaultLineStringSegment) {
                    Points cps = ((DefaultLineStringSegment) cs).getControlPoints();
                    for (Point p : cps) {
                        points.add(p);
                    }
                } else {
                    // ignore - and start new set of Points!
                    if (!points.isEmpty()) {
                        result_tmp.add(points);
                        points = new ArrayList<>();
                    }
                }
            }
            if (!points.isEmpty()) {
                result_tmp.add(points);
            }

        } else if (geom instanceof CompositeCurve) {

            final CompositeCurve cc = (CompositeCurve) geom;

            ArrayList<ArrayList<Point>> pLists = new ArrayList<>();

            for (Curve c : cc) {
                pLists.addAll(getSetsOfContinuousLinearCurveSegments(c));
            }

            /*
             * Since this is a composite curve, merge all point lists whose start is equal to the end of the previous list
             */
            result_tmp = mergeConsecutivePointListsWithEqualStartAndEnd(pLists);

        } else if (geom instanceof LineString) {

            Points cps = ((LineString) geom).getControlPoints();

            ArrayList<Point> points = new ArrayList<>();
            for (Point p : cps) {
                points.add(p);
            }
            result_tmp.add(points);

        } else if (geom instanceof OrientableCurve) {

            final OrientableCurve oc = (OrientableCurve) geom;
            final Curve baseCurve = oc.getBaseCurve();

            ArrayList<ArrayList<Point>> tmp = getSetsOfContinuousLinearCurveSegments(baseCurve);

            if (oc.isReversed()) {

                /*
                 * Especially for cases where the orientable curve is part of a composite geometry, we need to ensure that the
                 * returned points are put in the right order. Since the orientable curve is reversed, we need to reverse the
                 * results of calling this method on the base curve.
                 */
                Collections.reverse(tmp);
                for (ArrayList<Point> list : tmp) {
                    Collections.reverse(list);
                }

            }

            result_tmp = tmp;

        } else if (geom instanceof Ring) {

            final Ring r = (Ring) geom;
            final List<Curve> curves = r.getMembers();

            ArrayList<ArrayList<Point>> pLists = new ArrayList<>();

            for (Curve c : curves) {
                pLists.addAll(getSetsOfContinuousLinearCurveSegments(c));
            }

            /*
             * We also need to consider the start/end point of the ring, which may also be relevant.
             */
            if (pLists.size() > 1) {
                /*
                 * If the last point of the last list (in pListsMerged) is equal to the first point of the first list, then add
                 * the second point of the first list (at the end of) the last list.
                 */
                ArrayList<Point> lastList = pLists.get(pLists.size() - 1);
                ArrayList<Point> firstList = pLists.get(0);
                if (lastList.get(lastList.size() - 1).equals(firstList.get(0))) {
                    lastList.add(firstList.get(1));
                }
            } else if (pLists.size() == 1) {
                /*
                 * If start and end point of the list are equal, add the successor of the start point (at the end of) the list.
                 */
                ArrayList<Point> list = pLists.get(0);
                if (list.get(list.size() - 1).equals(list.get(0))) {
                    list.add(list.get(1));
                }
            }

            /*
             * Since the ring is composed of curves, merge all point lists whose start is equal to the end of the previous list
             */
            ArrayList<ArrayList<Point>> pListsMerged = mergeConsecutivePointListsWithEqualStartAndEnd(pLists);

            result_tmp = pListsMerged;

        } else if (geom instanceof Surface) {

            // covers CompositeSurface, OrientableSurface, Polygon, ...
            final Surface s = (Surface) geom;
            final List<? extends SurfacePatch> patches = s.getPatches();

            for (SurfacePatch sp : patches) {
                final List<? extends Ring> boundaryRings;
                if (sp instanceof PolygonPatch) {
                    boundaryRings = ((PolygonPatch) sp).getBoundaryRings();
                } else {
                    throw new UnsupportedGeometryTypeException(
                            "Surface contains a surface patch that is not a polygon patch, a rectangle, or a triangle.");
                }
                for (Ring r : boundaryRings) {
                    result_tmp.addAll(getSetsOfContinuousLinearCurveSegments(r));
                }
            }

        } else if (geom instanceof Point || geom instanceof MultiPoint) {

            // ignore
            return result_tmp;

        } else if (geom instanceof Solid) {

            final Solid s = (Solid) geom;

            if (s.getExteriorSurface() != null) {
                result_tmp.addAll(getSetsOfContinuousLinearCurveSegments(s.getExteriorSurface()));
            }
            for (Surface surface : s.getInteriorSurfaces()) {
                result_tmp.addAll(getSetsOfContinuousLinearCurveSegments(surface));
            }

        } else if (geom instanceof MultiGeometry) {

            @SuppressWarnings("rawtypes")
            final MultiGeometry mg = (MultiGeometry) geom;
            for (int i = 0; i < mg.size(); i++) {
                result_tmp.addAll(getSetsOfContinuousLinearCurveSegments((Geometry) mg.get(i)));
            }

        } else {
            throw new GmlGeoXException(
                    "Determination of sets of continuous linear curve segments for deegree geometry type '"
                            + geom.getClass().getName() + "' is not supported.");
        }

        /*
         * Now remove consecutive points in a point list that are equal to their predecessor.
         */
        ArrayList<ArrayList<Point>> result = new ArrayList<>();

        for (ArrayList<Point> points : result_tmp) {
            ArrayList<Point> cleanPointList = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                if (i == 0) {
                    cleanPointList.add(p);
                } else {
                    Point pBefore = points.get(i - 1);
                    if (!p.equals(pBefore)) {
                        cleanPointList.add(p);
                    }
                }
            }
            result.add(cleanPointList);
        }

        return result;
    }

    private ArrayList<ArrayList<Point>> mergeConsecutivePointListsWithEqualStartAndEnd(
            ArrayList<ArrayList<Point>> pLists) {

        if (pLists.size() <= 1) {
            return pLists;
        } else {
            ArrayList<ArrayList<Point>> result = new ArrayList<>();

            ArrayList<Point> points = new ArrayList<>();

            for (int i = 0; i < pLists.size(); i++) {

                ArrayList<Point> currentPoints = pLists.get(i);

                if (i == 0) {
                    points.addAll(currentPoints);
                } else {
                    ArrayList<Point> previousPoints = pLists.get(i - 1);
                    if (previousPoints.get(previousPoints.size() - 1).equals(currentPoints.get(0))) {
                        // previous point list continued by current point list: merge
                        points.addAll(currentPoints.subList(1, currentPoints.size()));
                    } else {
                        result.add(points);
                        points = new ArrayList<>();
                        points.addAll(currentPoints);
                    }
                }
            }

            if (!points.isEmpty()) {
                result.add(points);
            }

            return result;
        }
    }

    /**
     * Retrieves all basic curve components from the given geometry node. Composite geometries - including curves - will be
     * broken up into their parts. Point based geometries will be ignored.
     *
     * @param geomNode
     *            GML geometry node
     * @return A list with the curve components of the given geometry. Can be empty but not <code>null</code>.
     * @throws GmlGeoXException
     *             tbd
     */
    public Collection<Curve> getCurveComponents(final @NotNull ANode geomNode) throws GmlGeoXException {

        final Geometry geom_deegree = parseGeometry(geomNode);
        return getCurveComponents(geom_deegree);
    }

    public List<Curve> getCurveComponents(final @NotNull Value geomNodes) throws GmlGeoXException {

        List<Curve> result = new ArrayList<>();

        @SuppressWarnings("rawtypes")
        final Collection otherGeomNodesObjectList = GmlGeoX.toObjectCollection(geomNodes);
        for (Object o : otherGeomNodesObjectList) {
            if (!(o instanceof ANode)) {
                throw new GmlGeoXException(
                        "Calling getCurveComponents(Value) with a parameter that is not an ANode is illegal.");
            } else {
                result.addAll(getCurveComponents((ANode) o));
            }
        }

        return result;
    }

    /**
     * Gets the control points of the deegree geometry parsed from the given geometry node. Does not add new points through
     * interpolation.
     *
     * @param geometry
     *            represents a GML geometry
     * @param searchBehavior
     *            defines the behavior for determining control points; can be empty but not <code>null</code>; by default,
     *            all control points will be returned
     * @return a collection of unique control points
     * @throws GmlGeoXException
     *             if an exception occurred
     */
    public Points getControlPoints(final ANode geometry, final EnumSet<ControlPointSearchBehavior> searchBehavior)
            throws GmlGeoXException {

        Geometry geom = parseGeometry(geometry);

        return getControlPoints(geom, searchBehavior);
    }

    @SuppressWarnings("rawtypes")
    public Points getControlPoints(final Geometry geometry, final EnumSet<ControlPointSearchBehavior> searchBehavior)
            throws GmlGeoXException {

        Points points;

        if (geometry instanceof Curve) {
            points = getControlPoints((Curve) geometry, searchBehavior);
        } else if (geometry instanceof Point) {
            points = getControlPoints((Point) geometry, searchBehavior);
        } else if (geometry instanceof Surface) {
            points = getControlPoints((Surface) geometry, searchBehavior);
        } else if (geometry instanceof MultiPoint) {
            points = getControlPoints((MultiPoint) geometry, searchBehavior);
        } else if (geometry instanceof MultiGeometry) {
            points = getControlPoints((MultiGeometry) geometry, searchBehavior);
        } else {
            throw new IllegalArgumentException("Computing geometric points from a geometry of type "
                    + geometry.getGeometryType().name() + " is not supported.");
        }

        if (points.size() == 0) {
            throw new IllegalArgumentException("Unable to parse any geometric points from a geometry of type "
                    + geometry.getGeometryType().name());
        }

        return uniquePoints(points);
    }

    private Points uniquePoints(final Points points) {
        final Set<GeometricPoint> pointSet = new HashSet<>();

        for (Point p : points) {
            pointSet.add(new GeometricPoint(p));
        }

        final List<Point> uniquePointList = new ArrayList<>(pointSet);
        return geometryFactory.createPoints(uniquePointList);
    }

    private Points getControlPoints(final Surface surface, final EnumSet<ControlPointSearchBehavior> searchBehavior)
            throws GmlGeoXException {
        final List<Points> pointsList = new ArrayList<>();
        final Collection<Curve> curveComponents = getCurveComponents(surface);

        for (Curve curveComponent : curveComponents) {
            pointsList.add(getControlPoints(curveComponent, searchBehavior));
        }
        return new PointsPoints(pointsList);
    }

    @SuppressWarnings("rawtypes")
    private Points getControlPoints(final MultiGeometry multiGeometry,
            final EnumSet<ControlPointSearchBehavior> searchBehavior) throws GmlGeoXException {
        final List<Points> pointsList = new ArrayList<>();
        final Collection<Curve> curveComponents = getCurveComponents(multiGeometry);

        for (Curve curveComponent : curveComponents) {
            pointsList.add(getControlPoints(curveComponent, searchBehavior));
        }
        return new PointsPoints(pointsList);
    }

    private Points getControlPoints(final Point point, final EnumSet<ControlPointSearchBehavior> searchBehavior) {
        final List<Point> pointList = new ArrayList<>(1);
        pointList.add(point);
        return geometryFactory.createPoints(pointList);
    }

    private Points getControlPoints(final MultiPoint multiPoint,
            final EnumSet<ControlPointSearchBehavior> searchBehavior) {
        final List<Point> pointList = new ArrayList<>(1);
        pointList.addAll(multiPoint);
        return geometryFactory.createPoints(pointList);
    }

    /**
     * Identifies all control points of the given curve.
     *
     * @param curve
     *            Curve geometry from which to retrieve the control points
     * @return the control points of the given curve
     * @throws IllegalArgumentException
     *             If the curve contains an unsupported type of curve segment
     */
    public Points getControlPoints(final Curve curve) throws IllegalArgumentException {

        EnumSet<ControlPointSearchBehavior> searchBehavior = EnumSet.noneOf(ControlPointSearchBehavior.class);
        return getControlPoints(curve, searchBehavior);
    }

    /**
     * Identifies control points of the given curve, as determined by the search behavior parameter.
     * <p>
     * NOTE: Getting the control points from a deegree Curve via Curve.getControlPoints() is, according to the javadoc of
     * that method, only safe for linearly interpolated curves. Therefore, we use a different way to compute the control
     * points.
     *
     * @param curve
     *            Curve geometry from which to retrieve the control points
     * @param searchBehavior
     *            defines the behavior for determining control points; can be empty but not <code>null</code>; by default,
     *            all control points will be returned
     * @return the control points of the given curve
     * @throws IllegalArgumentException
     *             If the curve contains an unsupported type of curve segment
     */
    public Points getControlPoints(final Curve curve, final @NotNull EnumSet<ControlPointSearchBehavior> searchBehavior)
            throws IllegalArgumentException {

        final List<CurveSegment> curveSegments = curve.getCurveSegments();

        final List<Points> pointsList = new ArrayList<>(curveSegments.size());
        boolean first = true;

        for (CurveSegment cs : curve.getCurveSegments()) {

            if (searchBehavior.contains(ControlPointSearchBehavior.IGNORE_NON_LINEAR_SEGMENTS)
                    && !(cs instanceof DefaultLineStringSegment)) {
                continue;
            }

            Points p;

            if (cs instanceof DefaultLineStringSegment) {
                p = ((DefaultLineStringSegment) cs).getControlPoints();
            } else if (cs instanceof DefaultArcString) {

                if (searchBehavior.contains(ControlPointSearchBehavior.IGNORE_ARC_MID_POINT)) {
                    final Points controlPoints = ((DefaultArcString) cs).getControlPoints();
                    final List<Point> filteredControlPointList = new ArrayList<>();
                    for (int i = 0; i < controlPoints.size(); i++) {
                        // ignore mid points
                        if (i % 2 == 0) {
                            filteredControlPointList.add(controlPoints.get(i));
                        }
                    }
                    p = geometryFactory.createPoints(filteredControlPointList);
                } else {
                    p = ((DefaultArcString) cs).getControlPoints();
                }

            } else if (cs instanceof DefaultArc) {

                if (searchBehavior.contains(ControlPointSearchBehavior.IGNORE_ARC_MID_POINT)) {
                    final List<Point> controlPointList = new ArrayList<>();
                    controlPointList.add(((DefaultArc) cs).getPoint1());
                    controlPointList.add(((DefaultArc) cs).getPoint3());
                    p = geometryFactory.createPoints(controlPointList);
                } else {
                    p = ((DefaultArc) cs).getControlPoints();
                }

            } else if (cs instanceof DefaultArcStringByBulge) {

                /*
                 * NOTE: In an ArcStringByBulge, the control point sequence consists of the start and end points of each arc.
                 * Therefore, we do not need to take ControlPointSearchBehavior.IGNORE_ARC_MID_POINT into account.
                 */
                p = ((DefaultArcStringByBulge) cs).getControlPoints();

            } else if (cs instanceof DefaultArcByBulge) {

                /*
                 * NOTE: In an ArcByBulge, the control point sequence consists of the start and end points of the arc.
                 * Therefore, we do not need to take ControlPointSearchBehavior.IGNORE_ARC_MID_POINT into account.
                 */
                p = ((DefaultArcByBulge) cs).getControlPoints();

            } else if (cs instanceof DefaultBSpline) {
                p = ((DefaultBSpline) cs).getControlPoints();
            } else if (cs instanceof DefaultBezier) {
                p = ((DefaultBezier) cs).getControlPoints();
            } else if (cs instanceof DefaultCubicSpline) {
                p = ((DefaultCubicSpline) cs).getControlPoints();
            } else if (cs instanceof DefaultGeodesicString) {
                p = ((DefaultGeodesicString) cs).getControlPoints();
            } else if (cs instanceof DefaultGeodesic) {
                p = ((DefaultGeodesic) cs).getControlPoints();
            } else {
                throw new IllegalArgumentException("Computing control points from a curve segment of type "
                        + cs.getSegmentType().name() + " is not supported.");
            }

            if (first) {
                pointsList.add(p);
                first = false;
            } else {
                /*
                 * starting with the second segment, skip the first point (as it *must* be identical to last point of the last
                 * segment)
                 */
                pointsList.add(new PointsSubsequence(p, 1));
            }
        }

        return new PointsPoints(pointsList);
    }
}
