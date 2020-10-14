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

import static de.interactive_instruments.etf.bsxm.geometry.GeometricPoint.toPosListString;
import static de.interactive_instruments.etf.bsxm.index.GeometryCache.DEFAULT_SPATIAL_INDEX;
import static de.interactive_instruments.etf.bsxm.validator.GeometryValidator.VALIDATE_ALL;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.util.GeometryExtracter;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.basex.query.QueryException;
import org.basex.query.QueryModule;
import org.basex.query.value.Value;
import org.basex.query.value.item.Item;
import org.basex.query.value.item.Jav;
import org.basex.query.value.item.QNm;
import org.basex.query.value.node.ANode;
import org.basex.query.value.node.DBNode;
import org.basex.query.value.node.FElem;
import org.basex.query.value.seq.Empty;
import org.deegree.commons.xml.XMLParsingException;
import org.deegree.cs.persistence.CRSManager;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.primitive.Curve;
import org.deegree.geometry.primitive.Point;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.etf.bsxm.geometry.Circle;
import de.interactive_instruments.etf.bsxm.geometry.IIGeometryFactory;
import de.interactive_instruments.etf.bsxm.geometry.InvalidCircleException;
import de.interactive_instruments.etf.bsxm.index.GeometryCache;
import de.interactive_instruments.etf.bsxm.node.DBNodeRef;
import de.interactive_instruments.etf.bsxm.node.DBNodeRefFactory;
import de.interactive_instruments.etf.bsxm.node.DBNodeRefLookup;
import de.interactive_instruments.etf.bsxm.parser.BxNamespaceHolder;
import de.interactive_instruments.etf.bsxm.validator.GeometryValidator;

/**
 * This module supports the validation of geometries as well as computing the spatial relationship between geometries.
 *
 * <p>
 * NOTE 1: the validation and spatial relationship methods only support specific sets of geometry types - please see the
 * documentation of the respective methods for details on which geometry types are supported.
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
final public class GmlGeoX extends QueryModule implements Externalizable {

    private static final Pattern INTERSECTIONPATTERN = Pattern.compile("[0-2*TF]{9}");
    public static final String POINT_INDEX_NAME = "detectNearestPoints_PointIndex";

    public static final byte[] GMLGEOX_RESULT_NS = "https://modules.etf-validator.net/gmlgeox/result".getBytes();
    public static final byte[] GMLGEOX_RESULT_PREFIX = "geoxr".getBytes();

    private final com.vividsolutions.jts.geom.GeometryFactory jtsFactory = new com.vividsolutions.jts.geom.GeometryFactory();
    private IIGeometryFactory geometryFactory;
    private JtsTransformer jtsTransformer;
    private DeegreeTransformer deegreeTransformer;
    private DBNodeRefLookup dbNodeRefLookup;
    private DBNodeRefFactory dbNodeRefFactory;
    private SrsLookup srsLookup;
    private GeometryValidator geometryValidator;
    private GeometryCache geometryCache;

    private static final Logger logger = LoggerFactory.getLogger(GmlGeoX.class);
    private static final boolean debug = logger.isDebugEnabled();

    private int count = 0;
    private int count2 = 0;

    public GmlGeoX() {

    }

    @Requires(Permission.NONE)
    public void init(final String databaseName) {
        final BxNamespaceHolder bxNamespaceHolder = BxNamespaceHolder.init(queryContext);
        this.srsLookup = new SrsLookup();
        this.geometryFactory = new IIGeometryFactory();
        this.geometryCache = new GeometryCache();
        this.deegreeTransformer = new DeegreeTransformer(this.geometryFactory, bxNamespaceHolder, this.srsLookup);
        this.jtsTransformer = new JtsTransformer(this.deegreeTransformer, this.jtsFactory, this.srsLookup);
        this.dbNodeRefFactory = DBNodeRefFactory.create(databaseName);
        this.dbNodeRefLookup = new DBNodeRefLookup(this.queryContext, this.dbNodeRefFactory);
        this.geometryValidator = new GeometryValidator(this.srsLookup, this.jtsTransformer, this.deegreeTransformer,
                bxNamespaceHolder);
    }

    /**
     * Loads SRS configuration files from the given directory, to be used when looking up SRS names for creating geometry
     * objects.
     *
     * @param databaseName
     *            tbd
     * @param configurationDirectoryPathName
     *            Path to a directory that contains SRS configuration files
     * @throws QueryException
     *             in case that the SRS configuration directory does not exist, is not a directory, cannot be read, or an
     *             exception occurred while loading the configuration files
     */
    @Requires(Permission.NONE)
    public void init(final String databaseName, final String configurationDirectoryPathName) throws QueryException {
        init(databaseName);
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            final File configurationDirectory = new File(configurationDirectoryPathName);
            if (!configurationDirectory.exists() || !configurationDirectory.isDirectory()
                    || !configurationDirectory.canRead()) {
                throw new GmlGeoXException(
                        "Given path name does not identify a directory that exists and that can be read.");
            } else {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                CRSManager crsMgr = new CRSManager();
                crsMgr.init(configurationDirectory);
            }

        } catch (Exception e) {
            throw new GmlGeoXException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Retrieve the Well-Known-Text representation of a given JTS geometry.
     *
     * @param geom
     *            a JTS geometry
     * @return the WKT representation of the given geometry, or '&lt;null&gt;' if the geometry is <code>null</code>.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public String toWKT(com.vividsolutions.jts.geom.Geometry geom) {
        if (geom == null) {
            return "<null>";
        } else {
            return geom.toText();
        }
    }

    /**
     * Parse the Well-Known-Text to a JTS geometry.
     *
     * @param geomWkt
     *            geometry as well-known-text
     * @return the JTS geometry parsed from the wkt, or <code>null</code> if the geometry wkt is <code>null</code>.
     * @throws QueryException
     *             if an exception occurred while parsing the WKT string
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry fromWKT(final @NotNull String geomWkt) throws QueryException {

        WKTReader reader = new WKTReader(jtsFactory);
        try {
            return reader.read(geomWkt);
        } catch (ParseException e) {
            throw new GmlGeoXException("Could not parse the parameter as WKT.", e);
        }
    }

    /**
     * Flattens the given geometry if it is a geometry collection. Members that are not geometry collections are added to
     * the result. Thus, MultiPoint, -LineString, and -Polygon will also be flattened. Contained geometry collections are
     * recursively scanned for relevant members.
     *
     * @param geom
     *            a JTS geometry, can be a JTS GeometryCollection
     * @return a sequence of JTS geometry objects that are not collection types; can be empty but not null
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry[] flattenAllGeometryCollections(
            com.vividsolutions.jts.geom.Geometry geom) {

        if (geom == null) {
            return new com.vividsolutions.jts.geom.Geometry[]{};
        } else {
            final List<com.vividsolutions.jts.geom.Geometry> geoms;
            if (geom instanceof GeometryCollection) {
                final GeometryCollection col = (GeometryCollection) geom;
                geoms = new ArrayList<>(col.getNumGeometries());
                for (int i = 0; i < col.getNumGeometries(); i++) {
                    geoms.addAll(Arrays.asList(flattenAllGeometryCollections(col.getGeometryN(i))));
                }
            } else {
                geoms = Collections.singletonList(geom);
            }
            return geoms.toArray(new com.vividsolutions.jts.geom.Geometry[0]);
        }
    }

    /**
     * Only dissolves GeometryCollection objects, not MultiPoint, -LineString, or -Polygon.
     *
     * @param geom
     *            the geometry to flatten
     * @return the resulting set of geometry objects (none of direct type GeometryCollection)
     */
    public com.vividsolutions.jts.geom.Geometry[] flattenGeometryCollections(
            com.vividsolutions.jts.geom.Geometry geom) {

        if (geom == null) {
            return new com.vividsolutions.jts.geom.Geometry[]{};
        } else {
            final List<com.vividsolutions.jts.geom.Geometry> geoms;
            if (geom instanceof GeometryCollection && !(geom instanceof com.vividsolutions.jts.geom.MultiLineString
                    || geom instanceof com.vividsolutions.jts.geom.MultiPoint
                    || geom instanceof com.vividsolutions.jts.geom.MultiPolygon)) {
                final GeometryCollection col = (GeometryCollection) geom;
                geoms = new ArrayList<>(col.getNumGeometries());
                for (int i = 0; i < col.getNumGeometries(); i++) {
                    geoms.addAll(Arrays.asList(flattenGeometryCollections(col.getGeometryN(i))));
                }
            } else {
                geoms = Collections.singletonList(geom);
            }
            return geoms.toArray(new com.vividsolutions.jts.geom.Geometry[0]);
        }
    }

    /**
     * Identify all geometries contained in the given geometry, that have the given dimension. Note that Point and
     * MultiPoint have dimension 0, LineString and MultiLineString have dimension 1, and Polygon and MultiPolygon have
     * dimension 2.
     *
     * @param geom
     *            the geometry - typically a collection - to investigate; must not be <code>null</code>
     * @param dimension
     *            the dimension of geometries to return (value must be 0, 1, or 2)
     * @return the geometries with the specified dimension; can be empty
     * @throws GmlGeoXException
     *             if parameter values are incorrect
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry[] geometriesWithDimension(com.vividsolutions.jts.geom.Geometry geom,
            int dimension) throws GmlGeoXException {

        if (geom == null) {
            throw new GmlGeoXException("Parameter geom must not be null.");
        } else if (dimension < 0 || dimension > 2) {
            throw new GmlGeoXException("Parameter dimension must be 0, 1, or 2.");
        }

        com.vividsolutions.jts.geom.Geometry[] geoms = flattenGeometryCollections(geom);

        List<com.vividsolutions.jts.geom.Geometry> result = new ArrayList<>();

        for (com.vividsolutions.jts.geom.Geometry g : geoms) {

            if (g.getDimension() == dimension) {
                result.add(g);
            }
        }

        return result.toArray(new com.vividsolutions.jts.geom.Geometry[result.size()]);
    }

    /**
     * Identifies points of the given line, where the segment that ends in a point and the following segment that starts
     * with that point form a change in direction whose angular value is within a given interval.
     *
     * @param geom
     *            A LineString which shall be checked for directional changes whose value is within the given interval.
     * @param minAngle
     *            Minimum directional change to be considered, in degrees. 0<=minAngle<=maxAngle<=180
     * @param maxAngle
     *            Maximum directional change to be considered, in degrees. 0<=minAngle<=maxAngle<=180
     * @return The point(s) where the line has a directional change within the given change interval. Can be
     *         <code>null</code> in case that the given geometry is <code>null</code> or only has one segment.
     * @throws QueryException
     *             If the given geometry is not a LineString, or the minimum and maximum values are incorrect.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Point[] directionChanges(com.vividsolutions.jts.geom.Geometry geom,
            Object minAngle, Object maxAngle) throws QueryException {

        if (geom == null) {

            return null;

        } else if (!(geom instanceof LineString || geom instanceof MultiLineString)) {
            throw new GmlGeoXException("Geometry must be a LineString. Found: " + geom.getClass().getName());
        } else {

            double min;
            double max;

            if (minAngle instanceof Number && maxAngle instanceof Number) {

                min = ((Number) minAngle).doubleValue();
                max = ((Number) maxAngle).doubleValue();

                if (min < 0 || max < 0 || min > 180 || max > 180 || min > max) {
                    throw new GmlGeoXException("0 <= minAngle <= maxAngle <= 180. Found minAngle '" + minAngle
                            + "', maxAngle '" + maxAngle + "'.");
                }
            } else {
                throw new GmlGeoXException("minAngle and maxAngle must be numbers");
            }

            Coordinate[] coords = geom.getCoordinates();

            if (coords.length < 3) {

                return null;

            } else {

                final GeometryFactory fac = geom.getFactory();
                final List<com.vividsolutions.jts.geom.Point> result = new ArrayList<>(coords.length / 3);
                for (int i = 0; i < coords.length - 2; i++) {
                    final Coordinate coord1 = coords[i];
                    final Coordinate coord2 = coords[i + 1];
                    final Coordinate coord3 = coords[i + 2];

                    final double angleVector1to2 = Angle.angle(coord1, coord2);
                    final double angleVector2to3 = Angle.angle(coord2, coord3);

                    final double diff_rad = Angle.diff(angleVector1to2, angleVector2to3);

                    final double diff_deg = Angle.toDegrees(diff_rad);

                    if (diff_deg >= min && diff_deg <= max) {
                        com.vividsolutions.jts.geom.Point p = fac.createPoint(coord2);
                        result.add(p);
                    }
                }

                if (result.isEmpty()) {
                    return null;
                } else {
                    return result.toArray(new com.vividsolutions.jts.geom.Point[0]);
                }
            }
        }
    }

    /**
     * Identifies points of the given line, where the segment that ends in a point and the following segment that starts
     * with that point form a change in direction whose angular value is greater than the given limit.
     *
     * @param geom
     *            A LineString which shall be checked for directional changes that are greater than the given limit.
     * @param limitAngle
     *            Angular value of directional change that defines the limit, in degrees. 0 <= limitAngle <= 180
     * @return The point(s) where the line has a directional change that is greater than the given limit. Can be
     *         <code>null</code> in case that the given geometry is <code>null</code> or only has one segment.
     * @throws QueryException
     *             If the given geometry is not a LineString, or the limit value is incorrect.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Point[] directionChangesGreaterThanLimit(
            com.vividsolutions.jts.geom.Geometry geom, Object limitAngle) throws QueryException {

        if (geom == null) {
            return null;
        } else if (!(geom instanceof LineString || geom instanceof MultiLineString)) {
            throw new GmlGeoXException("Geometry must be a LineString. Found: " + geom.getClass().getName());
        } else {

            double limit;

            if (limitAngle instanceof Number) {

                limit = ((Number) limitAngle).doubleValue();

                if (limit < 0 || limit > 180) {
                    throw new GmlGeoXException("0 <= limitAngle <= 180. Found limitAngle '" + limitAngle + "'.");
                }
            } else {
                throw new GmlGeoXException("limitAngle must be a number");
            }

            final Coordinate[] coords = geom.getCoordinates();

            if (coords.length < 3) {
                return null;
            } else {

                final GeometryFactory fac = geom.getFactory();

                final List<com.vividsolutions.jts.geom.Point> result = new ArrayList<>(coords.length / 3);

                for (int i = 0; i < coords.length - 2; i++) {

                    Coordinate coord1 = coords[i];
                    Coordinate coord2 = coords[i + 1];
                    Coordinate coord3 = coords[i + 2];

                    double angleVector1to2 = Angle.angle(coord1, coord2);
                    double angleVector2to3 = Angle.angle(coord2, coord3);

                    double diff_rad = Angle.diff(angleVector1to2, angleVector2to3);

                    double diff_deg = Angle.toDegrees(diff_rad);

                    if (diff_deg > limit) {
                        com.vividsolutions.jts.geom.Point p = fac.createPoint(coord2);
                        result.add(p);
                    }
                }

                if (result.isEmpty()) {
                    return null;
                } else {
                    return result.toArray(new com.vividsolutions.jts.geom.Point[0]);
                }
            }
        }
    }

    /**
     * Validates the given (GML geometry) node, using all available tests.
     *
     * <p>
     * See the documentation of the {@link #validate(ANode, String)} method for a description of the supported geometry
     * types and tests.
     *
     * @param node
     *            Node
     * @return a mask with the test results, encoded as characters - one at each position (1-based index) of the available
     *         tests. 'V' indicates that the test passed, i.e. that the geometry is valid according to that test. 'F'
     *         indicates that the test failed. 'S' indicates that the test was skipped.
     */
    @Requires(Permission.NONE)
    public String validate(ANode node) {
        return geometryValidator.validateWithSimplifiedResults(node, VALIDATE_ALL);
    }

    /**
     * Validates the given (GML geometry) node.
     *
     * <p>
     * By default validation is only performed for the following GML geometry elements: Point, Polygon, Surface, Curve,
     * LinearRing, MultiPolygon, MultiGeometry, MultiSurface, MultiCurve, Ring, and LineString. The set of GML elements to
     * validate can be modified via the following methods: {@link #registerGmlGeometry(String)},
     * {@link #unregisterGmlGeometry(String)}, and {@link #unregisterAllGmlGeometries()}. These methods are also available
     * for XQueries.
     *
     * <p>
     * The validation tasks to perform can be specified via the given mask. The mask is a simple string, where the character
     * '1' at the position of a specific test (assuming a 1-based index) specifies that the test shall be performed. If the
     * mask does not contain a character at the position of a specific test (because the mask is empty or the length is
     * smaller than the position), then the test will be executed.
     *
     * <p>
     * The following tests are available:
     *
     * <p>
     *
     * <table border="1">
     * <tr>
     * <th>Position</th>
     * <th>Test Name</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>General Validation</td>
     * <td>This test validates the given geometry using the validation functionality of both deegree and JTS. More
     * specifically:
     * <p>
     * <p>
     * <span style="text-decoration: underline;"><strong>deegree based validation:</strong></span>
     * </p>
     * <ul>
     * <li>primitive geometry (point, curve, ring, surface):
     * <ul>
     * <li>point: no specific validation</li>
     * <li>curve:
     * <ul>
     * <li>duplication of successive control points (only for linear curve segments)</li>
     * <li>segment discontinuity</li>
     * <li>self intersection (based on JTS isSimple())</li>
     * </ul>
     * </li>
     * <li>ring:
     * <ul>
     * <li>Same as curve.</li>
     * <li>In addition, test if ring is closed</li>
     * </ul>
     * </li>
     * <li>surface:
     * <ul>
     * <li>only checks PolygonPatch, individually:</li>
     * <li>applies ring validation to interior and exterior rings</li>
     * <li>checks ring orientation (ignored for GML 3.1):
     * <ul>
     * <li>must be counter-clockwise for exterior ring</li>
     * <li>must be clockwise for interior ring</li>
     * </ul>
     * </li>
     * <li>interior ring intersects exterior</li>
     * <li>interior ring outside of exterior ring</li>
     * <li>interior rings intersection</li>
     * <li>interior rings are nested</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * <li>composite geometry: member geometries are validated individually</li>
     * <li>multi geometry: member geometries are validated individually</li>
     * </ul>
     * <p>
     * NOTE: There's some overlap with JTS validation. The following invalid situations are reported by the JTS validation:
     * </p>
     * <ul>
     * <li>curve self intersection</li>
     * <li>interior ring intersects exterior</li>
     * <li>interior ring outside of exterior ring</li>
     * <li>interior rings intersection</li>
     * <li>interior rings are nested</li>
     * <li>interior rings touch</li>
     * <li>interior ring touches exterior</li>
     * </ul>
     * <p>
     * <span style="text-decoration: underline;"><strong>JTS based validation</strong></span>:
     * </p>
     * <ul>
     * <li>Point:
     * <ul>
     * <li>invalid coordinates</li>
     * </ul>
     * </li>
     * <li>LineString:
     * <ul>
     * <li>invalid coordinates</li>
     * <li>too few points</li>
     * </ul>
     * </li>
     * <li>LinearRing:
     * <ul>
     * <li>invalid coordinates</li>
     * <li>closed ring</li>
     * <li>too few points</li>
     * <li>no self intersecting rings</li>
     * </ul>
     * </li>
     * <li>Polygon
     * <ul>
     * <li>invalid coordinates</li>
     * <li>closed ring</li>
     * <li>too few points</li>
     * <li>consistent area</li>
     * <li>no self intersecting rings</li>
     * <li>holes in shell</li>
     * <li>holes not nested</li>
     * <li>connected interiors</li>
     * </ul>
     * </li>
     * <li>MultiPoint:
     * <ul>
     * <li>invalid coordinates</li>
     * </ul>
     * </li>
     * <li>MultiLineString:
     * <ul>
     * <li>Each contained LineString is validated on its own.</li>
     * </ul>
     * </li>
     * <li>MultiPolygon:
     * <ul>
     * <li>Per polygon:
     * <ul>
     * <li>invalid coordinates</li>
     * <li>closed ring</li>
     * <li>holes in shell</li>
     * <li>holes not nested</li>
     * </ul>
     * </li>
     * <li>too few points</li>
     * <li>consistent area</li>
     * <li>no self intersecting rings</li>
     * <li>shells not nested</li>
     * <li>connected interiors</li>
     * </ul>
     * </li>
     * <li>GeometryCollection:
     * <ul>
     * <li>Each member of the collection is validated on its own.</li>
     * </ul>
     * </li>
     * </ul>
     * <p>
     * General description of checks performed by JTS:
     * </p>
     * <ul>
     * <li>invalid coordinates: x and y are neither NaN or infinite)</li>
     * <li>closed ring: tests if ring is closed; empty rings are closed by definition</li>
     * <li>too few points: tests if length of coordinate array - after repeated points have been removed - is big enough
     * (e.g. &gt;= 4 for a ring, &gt;= 2 for a line string)</li>
     * <li>no self intersecting rings: Check that there is no ring which self-intersects (except of course at its
     * endpoints); required by OGC topology rules</li>
     * <li>consistent area: Checks that the arrangement of edges in a polygonal geometry graph forms a consistent area.
     * Includes check for duplicate rings.</li>
     * <li>holes in shell: Tests that each hole is inside the polygon shell (i.e. hole rings do not cross the shell
     * ring).</li>
     * <li>holes not nested: Tests that no hole is nested inside another hole.</li>
     * <li>connected interiors: Check that the holes do not split the interior of the polygon into at least two pieces.</li>
     * <li>shells not nested: Tests that no element polygon is wholly in the interior of another element polygon (of a
     * MultiPolygon).</li>
     * </ul>
     * </td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>Polygon Patch Connectivity</td>
     * <td>Checks that multiple polygon patches within a single surface are connected.</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>Repetition of Position in CurveSegments</td>
     * <td>Checks that consecutive positions within a CurveSegment are not equal.</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>isSimple</td>
     * <td>
     * <p>
     * Tests whether a geometry is simple, based on JTS Geometry.isSimple(). In general, the OGC Simple Features
     * specification of simplicity follows the rule: A Geometry is simple if and only if the only self-intersections are at
     * boundary points.
     * </p>
     * <p>
     * Simplicity is defined for each JTS geometry type as follows:
     * </p>
     * <ul>
     * <li>Polygonal geometries are simple if their rings are simple (i.e., their rings do not self-intersect).
     * <ul>
     * <li>Note: This does not check if different rings of the geometry intersect, meaning that isSimple cannot be used to
     * fully test for (invalid) self-intersections in polygons. The JTS validity check fully tests for self-intersections in
     * polygons, and is part of the general validation in GmlGeoX.</li>
     * </ul>
     * </li>
     * <li>Linear geometries are simple iff they do not self-intersect at points other than boundary points.</li>
     * <li>Zero-dimensional (point) geometries are simple if and only if they have no repeated points.</li>
     * <li>Empty geometries are always simple, by definition.</li>
     * </ul>
     * </td>
     * </tr>
     * </table>
     *
     * <p>
     * Examples:
     *
     * <ul>
     * <li>The mask '0100' indicates that only the 'Polygon Patch Connectivity' test shall be performed.
     * <li>The mask '1110' indicates that all tests except the isSimple test shall be performed .
     * </ul>
     *
     * @param node
     *            the GML geometry to validate
     * @param testMask
     *            test mask
     * @return a mask with the test results, encoded as characters - one at each position (1-based index) of the available
     *         tests. 'V' indicates that the test passed, i.e. that the geometry is valid according to that test. 'F'
     *         indicates that the test failed. 'S' indicates that the test was skipped. Example: the string 'SVFF' shows
     *         that the first test was skipped, while the second test passed and the third and fourth failed.
     */
    @Requires(Permission.NONE)
    public String validate(final ANode node, final String testMask) {
        return geometryValidator.validateWithSimplifiedResults(node, testMask.getBytes());
    }

    /**
     * Validates the given (GML geometry) node, using all available tests.
     *
     * <p>
     * See the documentation of the {@link #validate(ANode, String)} method for a description of the supported geometry
     * types and tests.
     *
     * @param node
     *            Node
     * @return a DOM element with the validation result (for details about its structure, see the description of the result
     *         in method {@link #validateAndReport(ANode, String)})
     */
    @Requires(Permission.NONE)
    public FElem validateAndReport(ANode node) {
        return geometryValidator.validate(node, VALIDATE_ALL);
    }

    /**
     * Validates the given (GML geometry) node. The validation tasks to perform are specified via the given mask.
     *
     * <p>
     * See the documentation of the {@link #validate(ANode, String)} method for a description of the supported geometry
     * types, tests, and the test mask.
     *
     * @param node
     *            The GML geometry element to validate.
     * @param testMask
     *            Defines which tests shall be executed.
     * @return a DOM element, with the validation result and validation message (providing further details about any
     *         errors). The validation result is encoded as a sequence of characters - one at each position (1-based index)
     *         of the available tests. 'V' indicates that the test passed, i.e. that the geometry is valid according to that
     *         test. 'F' indicates that the test failed. 'S' indicates that the test was skipped. Example: the string 'SVFF'
     *         shows that the first test was skipped, while the second test passed and the third and fourth failed.
     *
     *         <pre>
     * {@code
     *         <ggeo:ValidationResult xmlns:ggeo=
     * "de.interactive_instruments.etf.bsxm.GmlGeoX">
     *           <ggeo:valid>false</ggeo:valid>
     *           <ggeo:result>VFV</ggeo:result>
     *           <ggeo:errors>
     *             <etf:message
     *               xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
     *               ref="TR.gmlgeox.validation.geometry.jts.5">
     *               <etf:argument token=
    "original">Invalid polygon. Two rings of the polygonal geometry intersect.</etf:argument>
     *               <etf:argument token="ID">DETHL56P0000F1TJ</etf:argument>
     *               <etf:argument token="context">Surface</etf:argument>
     *               <etf:argument token=
    "coordinates">666424.2393405803,5614560.422015165</etf:argument>
     *             </etf:message>
     *           </ggeo:errors>
     *         </ggeo:ValidationResult>
     * }
     * </pre>
     *
     * Where:
     * <ul>
     * <li>ggeo:valid - contains the boolean value indicating if the object passed all tests (defined by the testMask).
     * <li>ggeo:result - contains a string that is a mask with the test results, encoded as characters - one at each
     * position (1-based index) of the available tests. 'V' indicates that the test passed, i.e. that the geometry is valid
     * according to that test. 'F' indicates that the test failed. 'S' indicates that the test was skipped.
     * <li>ggeo:message (one for each message produced during validation) contains:
     * <ul>
     * <li>an XML attribute 'type' that indicates the severity level of the message ('FATAL', 'ERROR', 'WARNING', or
     * 'NOTICE')
     * <li>the actual validation message as text content
     * </ul>
     * </ul>
     *
     */
    @Requires(Permission.NONE)
    public FElem validateAndReport(ANode node, String testMask) {
        return geometryValidator.validate(node, testMask.getBytes());
    }

    /**
     * Tests if the first geometry contains the second geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geom1
     *            represents the first geometry, encoded as a GML geometry element
     * @param geom2
     *            represents the second geometry, encoded as a GML geometry element
     * @return <code>true</code> if the first geometry contains the second one, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean contains(ANode geom1, ANode geom2) throws QueryException {
        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.CONTAINS);
        } catch (Exception e) {
            throw new GmlGeoXException("Exception while executing contains(Value,Value). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry contains a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean contains(Value arg1, Value arg2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(arg1, arg2, SpatialRelOp.CONTAINS, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing contains(Value,Value,boolean). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if the first geometry contains the second geometry.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @return <code>true</code> if the first geometry contains the second one, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean containsGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.CONTAINS);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing containsGeomGeom(Geometry,Geometry). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Compute the DE-9IM intersection matrix that describes how geom1 relates to geom2.
     *
     * @param geom1
     *            the first (JTS) geometry
     * @param geom2
     *            the second (JTS) geometry
     * @return the DE-9IM intersection matrix that describes how geom1 relates to geom2
     */
    @Requires(Permission.NONE)
    @Deterministic
    public String intersectionMatrix(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) {
        return geom1.relate(geom2).toString();
    }

    /**
     * Tests if one geometry contains a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean containsGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.CONTAINS, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing containsGeomGeom(Geometry,Geometry,boolean). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if the first geometry crosses the second geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geom1
     *            represents the first geometry, encoded as a GML geometry element
     * @param geom2
     *            represents the second geometry, encoded as a GML geometry element
     * @return <code>true</code> if the first geometry crosses the second one, else <code>false</code> .
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean crosses(ANode geom1, ANode geom2) throws QueryException {
        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.CROSSES);
        } catch (Exception e) {
            throw new GmlGeoXException("Exception while executing crosses(Value,Value). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry crosses a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean crosses(Value arg1, Value arg2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(arg1, arg2, SpatialRelOp.CROSSES, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing crosses(Value,Value,boolean). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if the first geometry crosses the second geometry.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @return <code>true</code> if the first geometry crosses the second one, else <code>false</code> .
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean crossesGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.CROSSES);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing crossesGeomGeom(Geometry,Geometry). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if one geometry crosses a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean crossesGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.CROSSES, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing crossesGeomGeom(Geometry,Geometry,boolean). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if the first geometry equals the second geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geom1
     *            represents the first geometry, encoded as a GML geometry element
     * @param geom2
     *            represents the second geometry, encoded as a GML geometry element
     * @return <code>true</code> if the first geometry equals the second one, else <code>false</code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean equals(ANode geom1, ANode geom2) throws QueryException {
        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.EQUALS);
        } catch (Exception e) {
            throw new GmlGeoXException("Exception while executing equals(Value,Value). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry equals a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean equals(Value arg1, Value arg2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(arg1, arg2, SpatialRelOp.EQUALS, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing equals(Value,Value,boolean). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if the first geometry equals the second geometry.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @return <code>true</code> if the first geometry equals the second one, else <code>false</code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean equalsGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.EQUALS);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing equalsGeomGeom(Geometry,Geometry). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if one geometry equals a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean equalsGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.EQUALS, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing equalsGeomGeom(Geometry,Geometry,boolean). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if the first geometry intersects the second geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geom1
     *            represents the first geometry, encoded as a GML geometry element
     * @param geom2
     *            represents the second geometry, encoded as a GML geometry element
     * @return <code>true</code> if the first geometry intersects the second one, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean intersects(ANode geom1, ANode geom2) throws QueryException {
        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.INTERSECTS);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing intersects(Value,Value). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if one geometry intersects a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean intersects(Value arg1, Value arg2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(arg1, arg2, SpatialRelOp.INTERSECTS, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing intersects(Value,Value,boolean). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if the first geometry intersects the second geometry.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @return <code>true</code> if the first geometry intersects the second one, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean intersectsGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.INTERSECTS);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing intersectsGeomGeom(Geometry,Geometry). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry intersects a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean intersectsGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.INTERSECTS, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing intersectsGeomGeom(Geometry,Geometry,boolean). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Determine the name of the SRS that applies to the given geometry element. The SRS is looked up as follows (in order):
     *
     * <ol>
     * <li>If the element itself has an 'srsName' attribute, then the value of that attribute is returned.
     * <li>Otherwise, if a standard SRS is defined (see {@link #setStandardSRS(String)}), it is used.
     * <li>Otherwise, if an ancestor of the given element has the 'srsName' attribute, then the value of that attribute from
     * the first ancestor (i.e., the nearest) is used.
     * <li>Otherwise, if an ancestor of the given element has a child element with local name 'boundedBy', which itself has
     * a child with local name 'Envelope', and that child has an 'srsName' attribute, then the value of that attribute from
     * the first ancestor (i.e., the nearest) that fulfills the criteria is used.
     * </ol>
     *
     * NOTE: The lookup is independent of a specific GML namespace.
     *
     * @param geometryNode
     *            a gml geometry node
     * @return the value of the applicable 'srsName' attribute, if found, otherwise <code>null</code>
     */
    @Requires(Permission.NONE)
    @Deterministic
    public String determineSrsName(final ANode geometryNode) {
        return this.srsLookup.determineSrsName(geometryNode);
    }

    /**
     * Determine the name of the SRS that applies to the given geometry component element (e.g. a curve segment). The SRS is
     * looked up as follows (in order):
     *
     * <ol>
     * <li>If a standard SRS is defined (see {@link #setStandardSRS(String)}), it is used.
     * <li>Otherwise, if an ancestor of the given element has the 'srsName' attribute, then the value of that attribute from
     * the first ancestor (i.e., the nearest) is used.
     * <li>Otherwise, if an ancestor of the given element has a child element with local name 'boundedBy', which itself has
     * a child with local name 'Envelope', and that child has an 'srsName' attribute, then the value of that attribute from
     * the first ancestor (i.e., the nearest) that fulfills the criteria is used.
     * </ol>
     *
     * NOTE: The lookup is independent of a specific GML namespace.
     *
     * @param geometryComponentNode
     *            a gml geometry component node (e.g. Arc or Circle)
     * @return the value of the applicable 'srsName' attribute, if found, otherwise <code>null</code>
     */
    @Requires(Permission.NONE)
    @Deterministic
    public String determineSrsNameForGeometryComponent(final ANode geometryComponentNode) {
        return this.srsLookup.determineSrsNameForGeometryComponent(geometryComponentNode);
    }

    /**
     * Parse a geometry.
     *
     * @param v
     *            - either a geometry node or a JTS geometry
     * @return a JTS geometry
     * @throws QueryException
     *             If an exception occurred while parsing the geometry.
     */
    public com.vividsolutions.jts.geom.Geometry parseGeometry(Value v) throws QueryException {
        if (v instanceof ANode) {
            ANode node = (ANode) v;
            return jtsTransformer.toJTSGeometry(node);
        } else if (v instanceof Jav && ((Jav) v).toJava() instanceof com.vividsolutions.jts.geom.Geometry) {
            return (com.vividsolutions.jts.geom.Geometry) ((Jav) v).toJava();
        } else {
            throw new GmlGeoXException("First argument is neither a single node nor a JTS geometry.");
        }
    }

    /**
     * Compares two objects that represent geometries. If one of these objects is an instance of BaseX Empty,
     * <code>false</code> will be returned. Cannot compare JTS GeometryCollection objects that are real collections (so not
     * one of the subtypes: MultiPoint, MultiLineString, MultiPolygon) - unless both are empty (then the result is
     * <code>false</code>).
     *
     * @param geom1x
     * @param geom2x
     * @param op
     * @return
     * @throws QueryException
     */
    private boolean applySpatialRelationshipOperation(final Object geom1x, final Object geom2x, final SpatialRelOp op)
            throws QueryException {

        if (geom1x == null || geom2x == null || geom1x instanceof Empty || geom2x instanceof Empty) {
            return false;
        }

        if ((geom1x instanceof GeometryCollection
                && JtsTransformer.isGeometryCollectionButNotASubtype((GeometryCollection) geom1x))
                || (geom2x instanceof GeometryCollection
                        && JtsTransformer.isGeometryCollectionButNotASubtype((GeometryCollection) geom2x))) {
            if (geom1x instanceof GeometryCollection && !((GeometryCollection) geom1x).isEmpty()) {
                throw new GmlGeoXException(
                        "GmlGeoX.applySpatialRelationshipOperation(..) - First argument is a non-empty geometry collection. This is not supported by this method.");
            } else if (geom2x instanceof GeometryCollection && !((GeometryCollection) geom2x).isEmpty()) {
                throw new GmlGeoXException(
                        "GmlGeoX.applySpatialRelationshipOperation(..) - Second argument is a non-empty geometry collection. This is not supported by this method.");
            }

            return false;
        }

        final com.vividsolutions.jts.geom.Geometry g1 = getCachedGeometryFromNodeOrTransform(geom1x);
        final com.vividsolutions.jts.geom.Geometry g2 = getCachedGeometryFromNodeOrTransform(geom2x);

        return op.call(g1, g2);
    }

    private boolean applySpatialRelationshipOperation(final Object arg1, final Object arg2, final SpatialRelOp op,
            boolean matchAll) throws QueryException {

        try {

            if (arg1 == null || arg2 == null || arg1 instanceof Empty || arg2 instanceof Empty
                    || (arg1 instanceof GeometryCollection && ((GeometryCollection) arg1).isEmpty())
                    || (arg2 instanceof GeometryCollection && ((GeometryCollection) arg2).isEmpty())) {

                return false;

            } else {

                final Collection<Object> arg1_list = toObjectCollection(arg1);
                final Collection<Object> arg2_list = toObjectCollection(arg2);

                boolean allMatch = true;

                outer: for (Object o1 : arg1_list) {
                    for (Object o2 : arg2_list) {

                        if (matchAll) {
                            if (!applySpatialRelationshipOperation(o1, o2, op)) {
                                allMatch = false;
                                break outer;
                            }
                            // check the next geometry pair to see if it also satisfies the spatial
                            // relationship
                        } else {
                            if (applySpatialRelationshipOperation(o1, o2, op)) {
                                return true;
                            }
                            // check the next geometry pair to see if it satisfies the spatial relationship
                        }
                    }
                }
                if (matchAll) {
                    return allMatch;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception occurred while applying spatial relationship operation (with multiple geometries to compare). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * @param o
     *            A Value, an Object[], a JTS geometry (also a geometry collection) or anything else
     * @return a list of single objects (Value of size > 1 is flattened to a list of Items, a JTS geometry collection is
     *         flattened as well)
     */
    static Collection toObjectCollection(final @NotNull Object o) {
        if (o instanceof Value) {
            final Value v = (Value) o;
            if (v.size() > 1) {
                final List<Object> result = new ArrayList<>((int) v.size());
                for (Item i : v) {
                    result.addAll(toObjectCollection(i));
                }
                return result;
            } else if (v instanceof Jav) {
                return Collections.singleton(((Jav) o).toJava());
            } else {
                return Collections.singleton(o);
            }
        } else if (o instanceof Object[]) {
            final List<Object> result = new ArrayList<>(((Object[]) o).length);
            for (Object os : (Object[]) o) {
                result.addAll(toObjectCollection(os));
            }
            return result;
        } else {
            if (o instanceof com.vividsolutions.jts.geom.Geometry) {
                final com.vividsolutions.jts.geom.Geometry geom = (com.vividsolutions.jts.geom.Geometry) o;
                return JtsTransformer.toFlattenedJTSGeometryCollection(geom);
            } else {
                return Collections.singleton(o);
            }
        }
    }

    /**
     * Tests if the first and the second geometry are disjoint.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geom1
     *            represents the first geometry, encoded as a GML geometry element
     * @param geom2
     *            represents the second geometry, encoded as a GML geometry element
     * @return <code>true</code> if the first and the second geometry are disjoint, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isDisjoint(ANode geom1, ANode geom2) throws QueryException {
        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.ISDISJOINT);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing isDisjoint(Value,Value). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if one geometry is disjoint with a list of geometries. Whether a match is required for all or just one of these
     * is controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isDisjoint(Value arg1, Value arg2, boolean matchAll) throws QueryException {
        try {
            return applySpatialRelationshipOperation(arg1, arg2, SpatialRelOp.ISDISJOINT, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing isDisjoint(Value,Value,boolean). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if the first and the second geometry are disjoint.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @return <code>true</code> if the first and the second geometry are disjoint, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isDisjointGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.ISDISJOINT);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing isDisjointGeomGeom(Geometry,Geometry). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry is disjoint with a list of geometries. Whether a match is required for all or just one of these
     * is controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isDisjointGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.ISDISJOINT, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing isDisjointGeomGeom(Geometry,Geometry,boolean). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if the first geometry is within the second geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geom1
     *            represents the first geometry, encoded as a GML geometry element
     * @param geom2
     *            represents the second geometry, encoded as a GML geometry element
     * @return <code>true</code> if the first geometry is within the second geometry, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isWithin(ANode geom1, ANode geom2) throws QueryException {
        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.ISWITHIN);
        } catch (Exception e) {
            throw new GmlGeoXException("Exception while executing isWithin(Value,Value). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry is within a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isWithin(Value arg1, Value arg2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(arg1, arg2, SpatialRelOp.ISWITHIN, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing isWithin(Value,Value,boolean). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if the first geometry is within the second geometry.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @return <code>true</code> if the first geometry is within the second geometry, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isWithinGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.ISWITHIN);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing isWithinGeomGeom(Geometry,Geometry). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if one geometry is within a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isWithinGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.ISWITHIN, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing isWithinGeomGeom(Geometry,Geometry,boolean). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if the first geometry overlaps the second geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geom1
     *            represents the first geometry, encoded as a GML geometry element
     * @param geom2
     *            represents the second geometry, encoded as a GML geometry element
     * @return <code>true</code> if the first geometry overlaps the second one, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean overlaps(ANode geom1, ANode geom2) throws QueryException {
        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.OVERLAPS);
        } catch (Exception e) {
            throw new GmlGeoXException("Exception while executing overlaps(Value,Value). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry overlaps a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean overlaps(Value arg1, Value arg2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(arg1, arg2, SpatialRelOp.OVERLAPS, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing overlaps(Value,Value,boolean). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if the first geometry overlaps the second geometry.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @return <code>true</code> if the first geometry intersects the second one, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean overlapsGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.OVERLAPS);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing overlapsGeomGeom(Geometry,Geometry). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if one geometry overlaps a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean overlapsGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.OVERLAPS, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing overlapsGeomGeom(Geometry,Geometry,boolean). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if the first geometry touches the second geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geom1
     *            represents the first geometry, encoded as a GML geometry element
     * @param geom2
     *            represents the second geometry, encoded as a GML geometry element
     * @return <code>true</code> if the first geometry touches the second one, else <code>false</code> .
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean touches(ANode geom1, ANode geom2) throws QueryException {
        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.TOUCHES);
        } catch (Exception e) {
            throw new GmlGeoXException("Exception while executing touches(Value,Value). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry touches a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean touches(Value arg1, Value arg2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(arg1, arg2, SpatialRelOp.TOUCHES, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing touches(Value,Value,boolean). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if the first geometry touches the second geometry.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @return <code>true</code> if the first geometry intersects the second one, else <code>false
     *     </code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean touchesGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.TOUCHES);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing touchesGeomGeom(Geometry,Geometry). Message is: " + e.getMessage(), e);
        }
    }

    /**
     * Tests if one geometry touches a list of geometries. Whether a match is required for all or just one of these is
     * controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean touchesGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, boolean matchAll) throws QueryException {

        try {
            return applySpatialRelationshipOperation(geom1, geom2, SpatialRelOp.TOUCHES, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing touchesGeomGeom(Geometry,Geometry,boolean). Message is: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Adds the name of a GML geometry element to the set of elements for which validation is performed.
     *
     * @param gmlGeometry
     *            name (simple, i.e. without namespace (prefix)) of a GML geometry element to validate.
     */
    @Requires(Permission.NONE)
    public void registerGmlGeometry(final String gmlGeometry) {
        geometryValidator.registerGmlGeometry(gmlGeometry);
    }

    /**
     * Set the standard SRS to use for a geometry if no srsName attribute is directly defined for it. Setting a standard SRS
     * can improve performance, but should only be done if all geometry elements without srsName attribute have the same
     * SRS.
     *
     * @param srsName
     *            name of the SRS to assign to a geometry if it does not have an srsName attribute itself.
     * @throws QueryException
     *             tbd
     */
    @Requires(Permission.NONE)
    public void setStandardSRS(final String srsName) throws QueryException {
        if (StringUtils.isBlank(srsName)) {
            throw new GmlGeoXException("Given parameter value is blank.");
        } else {
            this.srsLookup.setStandardSRS(srsName);
        }
    }

    /**
     * Set the maximum number of points to be created when interpolating an arc. Default is 1000. The lower the maximum
     * error (set via {@link #setMaxErrorForInterpolation(double)}), the higher the number of points needs to be. Arc
     * interpolation will never create more than the configured maximum number of points. However, the interpolation will
     * also never create more points than needed to achieve the maximum error. In order to achieve interpolations with a
     * very low maximum error, the maximum number of points needs to be increased accordingly.
     *
     * @param maxNumPoints
     *            maximum number of points to be created when interpolating an arc
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void setMaxNumPointsForInterpolation(int maxNumPoints) throws QueryException {
        if (maxNumPoints <= 0) {
            throw new GmlGeoXException("Given parameter value must be greater than zero. Was: " + maxNumPoints + ".");
        } else {
            this.geometryFactory.setMaxNumPoints(maxNumPoints);
        }
    }

    /**
     * Set the maximum error (e.g. 0.00000001 - default setting is 0.00001), i.e. the maximum difference between an arc and
     * the interpolated line string - that shall be achieved when creating new arc interpolations. The lower the error
     * (maximum difference), the more interpolation points will be needed. However, note that a maximum for the number of
     * such points exists. It can be set via {@link #setMaxNumPointsForInterpolation(int)} (default value is stated in the
     * documentation of that method).
     *
     * @param maxError
     *            the maximum difference between an arc and the interpolated line
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void setMaxErrorForInterpolation(double maxError) throws QueryException {
        if (maxError <= 0) {
            throw new GmlGeoXException("Given parameter value must be greater than zero. Was: " + maxError + ".");
        } else {
            this.geometryFactory.setMaxError(maxError);
        }
    }

    /**
     * Removes the name of a GML geometry element from the set of elements for which validation is performed.
     *
     * @param nodeName
     *            name (simple, i.e. without namespace (prefix)) of a GML geometry element to remove from validation.
     */
    @Requires(Permission.NONE)
    public void unregisterGmlGeometry(String nodeName) {
        geometryValidator.unregisterGmlGeometry(nodeName);
    }

    /**
     * Removes all names of GML geometry elements that are currently registered for validation.
     */
    @Requires(Permission.NONE)
    public void unregisterAllGmlGeometries() {
        geometryValidator.unregisterAllGmlGeometries();
    }

    /**
     * @return the currently registered GML geometry element names (comma separated)
     */
    @Requires(Permission.NONE)
    public String registeredGmlGeometries() {
        return geometryValidator.registeredGmlGeometries();
    }

    private com.vividsolutions.jts.geom.Geometry unionGeom(List<com.vividsolutions.jts.geom.Geometry> geoms)
            throws QueryException {

        try {
            com.vividsolutions.jts.geom.GeometryCollection gc = jtsTransformer.toJTSGeometryCollection(geoms, true);

            return gc.union();
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Create the union of the given geometry nodes.
     *
     * @param val
     *            a single or collection of geometry nodes.
     * @return the union of the geometries - can be a JTS geometry collection
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry union(final Value val) throws QueryException {

        try {

            // first get all values
            final List<Item> items = new ArrayList<>((int) val.size());

            for (Item i : val) {
                items.add(i);
            }

            /* Now, create unions from partitions of the list of items. */
            final List<com.vividsolutions.jts.geom.Geometry> unions = new ArrayList<>((int) Math.sqrt(items.size()));

            int x = (int) (Math.ceil((double) items.size() / 1000) - 1);

            for (int groupIndex = 0; groupIndex <= x; groupIndex++) {

                final int groupStart = (x - groupIndex) * 1000;
                int groupEnd = ((x - groupIndex) + 1) * 1000;

                if (groupEnd > items.size()) {
                    groupEnd = items.size();
                }

                final List<Item> itemsSublist = items.subList(groupStart, groupEnd);

                final List<com.vividsolutions.jts.geom.Geometry> geomsInSublist = new ArrayList<>(itemsSublist.size());

                for (Item i : itemsSublist) {
                    final com.vividsolutions.jts.geom.Geometry geom;
                    if (i instanceof DBNode) {
                        geom = getOrCacheGeometry((DBNode) i);
                    } else {
                        geom = jtsTransformer.toJTSGeometry(i);
                    }
                    geomsInSublist.add(geom);
                }

                com.vividsolutions.jts.geom.GeometryCollection sublistGc = jtsTransformer
                        .toJTSGeometryCollection(geomsInSublist, true);

                unions.add(sublistGc.union());
            }

            /* Finally, create a union from the list of unions. */
            com.vividsolutions.jts.geom.GeometryCollection unionsGc = jtsTransformer.toJTSGeometryCollection(unions,
                    true);

            return unionsGc.union();

        } catch (Exception e) {
            throw new GmlGeoXException("Exception occurred while applying union(Value)). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Check if a JTS geometry is empty.
     *
     * @param geom
     *            the geometry to check
     * @return <code>true</code> if the geometry is <code>null</code> or empty, else <code>false
     *     </code>.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isEmptyGeom(com.vividsolutions.jts.geom.Geometry geom) {
        return geom == null || geom.isEmpty();
    }

    /**
     * Checks that the second control point of each arc in the given $arcStringNode is positioned in the middle third of
     * that arc.
     *
     * @param arcStringNode
     *            A gml:Arc or gml:ArcString element
     * @return The coordinate of the second control point of the first invalid arc, or <code>null
     *     </code> if all arcs are valid.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public Coordinate checkSecondControlPointInMiddleThirdOfArc(ANode arcStringNode) throws QueryException {

        final Coordinate[] controlPoints = jtsTransformer.parseArcStringControlPoints(arcStringNode);

        for (int i = 2; i < controlPoints.length; i = i + 2) {

            final Coordinate c1 = controlPoints[i - 2];
            final Coordinate c2 = controlPoints[i - 1];
            final Coordinate c3 = controlPoints[i];

            Circle circle;
            try {
                circle = Circle.from3Coordinates(c1, c2, c3);
            } catch (InvalidCircleException e) {
                return c2;
            }
            final Coordinate center = circle.center();
            final double d12 = Angle.angleBetweenOriented(c1, center, c2);
            final double d13 = Angle.angleBetweenOriented(c1, center, c3);

            final boolean controlPointsOrientedClockwise;

            if (d12 >= 0 && d13 >= 0) {

                if (d12 > d13) {
                    // CW
                    controlPointsOrientedClockwise = true;
                } else {
                    // CCW
                    controlPointsOrientedClockwise = false;
                }
            } else if (d12 >= 0 && d13 < 0) {
                // CCW
                controlPointsOrientedClockwise = false;
            } else if (d12 < 0 && d13 >= 0) {
                // CW
                controlPointsOrientedClockwise = true;
            } else {
                // d12 < 0 && d13 < 0
                if (d12 < d13) {
                    // CCW
                    controlPointsOrientedClockwise = false;
                } else {
                    // CW
                    controlPointsOrientedClockwise = true;
                }
            }

            final double fullAngle1to2;
            final double fullAngle1to3;

            if (controlPointsOrientedClockwise) {
                if (d12 >= 0) {
                    fullAngle1to2 = Math.PI * 2 - d12;
                } else {
                    fullAngle1to2 = Math.abs(d12);
                }
                if (d13 >= 0) {
                    fullAngle1to3 = Math.PI * 2 - d13;
                } else {
                    fullAngle1to3 = Math.abs(d13);
                }
            } else {
                if (d12 >= 0) {
                    fullAngle1to2 = d12;
                } else {
                    fullAngle1to2 = Math.PI * 2 - Math.abs(d12);
                }
                if (d13 >= 0) {
                    fullAngle1to3 = d13;
                } else {
                    fullAngle1to3 = Math.PI * 2 - Math.abs(d13);
                }
            }

            final double thirdOfFullAngle1to3 = fullAngle1to3 / 3;

            final double middleThirdStart = thirdOfFullAngle1to3;
            final double middleThirdEnd = fullAngle1to3 - thirdOfFullAngle1to3;

            if (middleThirdStart > fullAngle1to2 || middleThirdEnd < fullAngle1to2) {
                return c2;
            }
        }

        return null;
    }

    /**
     * Check that the three control points of a gml:Circle are at least a given amount of degrees apart from each other.
     *
     * @param circleNode
     *            A gml:Circle element, defined by three control points
     * @param minSeparationInDegree
     *            the minimum angle between each control point, in degree (0<=x<=120)
     * @return The coordinate of a control point which does not have the minimum angle to one of the other control points,
     *         or <code>null</code> if the angles between all points are greater than or equal to the minimum separation
     *         angle
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public Coordinate checkMinimumSeparationOfCircleControlPoints(ANode circleNode, Object minSeparationInDegree)
            throws QueryException {

        double minSepDeg;
        if (minSeparationInDegree instanceof Number) {
            minSepDeg = ((Number) minSeparationInDegree).doubleValue();
        } else {
            throw new GmlGeoXException("Parameter minSeparationInDegree must be a number.");
        }

        if (minSepDeg < 0 || minSepDeg > 120) {
            throw new GmlGeoXException("Invalid parameter minSeparationInDegree (must be >= 0 and <= 120).");
        }

        final Coordinate[] controlPoints = jtsTransformer.parseArcStringControlPoints(circleNode);

        Coordinate c1 = controlPoints[0];
        Coordinate c2 = controlPoints[1];
        Coordinate c3 = controlPoints[2];

        Circle circle;
        try {
            circle = Circle.from3Coordinates(c1, c2, c3);
        } catch (InvalidCircleException e) {
            return c1;
        }
        Coordinate center = circle.center();

        // NOTE: angle in radians (0,PI)
        double d12 = Angle.angleBetween(c1, center, c2);
        double d13 = Angle.angleBetween(c1, center, c3);
        double d23 = Angle.angleBetween(c2, center, c3);

        double minSeparation = Angle.toRadians(minSepDeg);

        if (d12 < minSeparation) {
            return c1;
        } else if (d13 < minSeparation) {
            return c1;
        } else if (d23 < minSeparation) {
            return c2;
        } else {
            return null;
        }
    }

    /**
     * Checks if a given geometry is closed. Only LineStrings and MultiLineStrings are checked.
     *
     * <p>
     * NOTE: Invokes the {@link #isClosedGeom(com.vividsolutions.jts.geom.Geometry, boolean)} method, with <code>true</code>
     * for the second parameter.
     *
     * @see #isClosedGeom(com.vividsolutions.jts.geom.Geometry, boolean)
     * @param geom
     *            the geometry to check
     * @return <code>true</code>, if the geometry is closed, else <code>false</code>
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isClosedGeom(com.vividsolutions.jts.geom.Geometry geom) throws QueryException {
        return isClosedGeom(geom, true);
    }

    /**
     * Checks if the geometry represented by the given node is closed. Only LineStrings and MultiLineStrings are checked.
     *
     * <p>
     * NOTE: Invokes the {@link #isClosed(ANode, boolean)} method, with <code>true</code> for the second parameter.
     *
     * @see #isClosed(ANode, boolean)
     * @param geom
     *            the geometry to check
     * @return <code>true</code>, if the geometry is closed, else <code>false</code>
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isClosed(ANode geom) throws QueryException {
        return isClosed(geom, true);
    }

    /**
     * Checks if a given geometry is closed. Points and MultiPoints are closed by definition (they do not have a boundary).
     * Polygons and MultiPolygons are never closed in 2D, and since operations in 3D are not supported, this method will
     * always return <code>false</code> if a polygon is encountered - unless the parameter onlyCheckCurveGeometries is set
     * to <code>true</code>. LinearRings are closed by definition. The remaining geometry types that will be checked are
     * LineString and MultiLineString. If a (Multi)LineString is not closed, this method will return <code>false</code>.
     *
     * @param geom
     *            the geometry to test
     * @param onlyCheckCurveGeometries
     *            <code>true</code> if only curve geometries (i.e., for JTS: LineString, LinearRing, and MultiLineString)
     *            shall be tested, else <code>false</code> (in this case, the occurrence of polygons will result in the
     *            return value <code>false</code>).
     * @return <code>true</code> if the given geometry is closed, else <code>false</code>
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isClosedGeom(com.vividsolutions.jts.geom.Geometry geom, boolean onlyCheckCurveGeometries)
            throws QueryException {

        final Collection<com.vividsolutions.jts.geom.Geometry> gc = jtsTransformer
                .toFlattenedJTSGeometryCollection(geom);

        for (com.vividsolutions.jts.geom.Geometry g : gc) {

            if (g instanceof com.vividsolutions.jts.geom.Point || g instanceof com.vividsolutions.jts.geom.MultiPoint) {

                /* points are closed by definition (they do not have a boundary) */

            } else if (g instanceof com.vividsolutions.jts.geom.Polygon
                    || g instanceof com.vividsolutions.jts.geom.MultiPolygon) {

                /*
                 * The JTS FAQ contains the following question and answer:
                 *
                 * Question: Does JTS support 3D operations?
                 *
                 * Answer: JTS does not provide support for true 3D geometry and operations. However, JTS does allow Coordinates
                 * to carry an elevation or Z value. This does not provide true 3D support, but does allow "2.5D" uses which are
                 * required in some geospatial applications.
                 *
                 * -------
                 *
                 * So, JTS does not support true 3D geometry and operations. Therefore, JTS cannot determine if a surface is
                 * closed. deegree does not seem to support this, either. In order for a surface to be closed, it must be a
                 * sphere or torus, possibly with holes. A surface in 2D can never be closed. Since we lack the ability to
                 * compute in 3D we assume that a (Multi)Polygon is not closed. If we do check geometries other than curves,
                 * then we return false.
                 */
                if (!onlyCheckCurveGeometries) {
                    return false;
                }
            } else if (g instanceof com.vividsolutions.jts.geom.MultiLineString) {

                com.vividsolutions.jts.geom.MultiLineString mls = (com.vividsolutions.jts.geom.MultiLineString) g;
                if (!mls.isClosed()) {
                    return false;
                }
            } else if (g instanceof com.vividsolutions.jts.geom.LineString) {
                /* NOTE: LinearRing is a subclass of LineString, and closed by definition */
                final com.vividsolutions.jts.geom.LineString ls = (com.vividsolutions.jts.geom.LineString) g;
                if (!ls.isClosed()) {
                    return false;
                }
            } else {
                // should not happen
                throw new GmlGeoXException("Unexpected geometry type encountered: " + g.getClass().getName());
            }
        }

        // all relevant geometries are closed
        return true;
    }

    /**
     * Checks if the geometry represented by the given node is closed. Points and MultiPoints are closed by definition (they
     * do not have a boundary). Polygons and MultiPolygons are never closed in 2D, and since operations in 3D are not
     * supported, this method will always return <code>false
     * </code> if a polygon is encountered - unless the parameter onlyCheckCurveGeometries is set to <code>true</code>.
     * LinearRings are closed by definition. The remaining geometry types that will be checked are LineString and
     * MultiLineString. If a (Multi)LineString is not closed, this method will return <code>false</code>.
     *
     * @param geomNode
     *            the geometry node to test
     * @param onlyCheckCurveGeometries
     *            <code>true</code> if only curve geometries (i.e., for JTS: LineString, LinearRing, and MultiLineString)
     *            shall be tested, else <code>false</code> (in this case, the occurrence of polygons will result in the
     *            return value <code>false</code>).
     * @return <code>true</code> if the geometry represented by the given node is closed, else <code>
     *     false</code>
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean isClosed(ANode geomNode, boolean onlyCheckCurveGeometries) throws QueryException {
        com.vividsolutions.jts.geom.Geometry geom = getOrCacheGeometry(geomNode);
        return isClosedGeom(geom, onlyCheckCurveGeometries);
    }

    /**
     * Identifies the holes contained in the geometry represented by the given geometry node and returns them as a JTS
     * geometry. If holes were found a union is built, to ensure that the result is a valid JTS Polygon or JTS MultiPolygon.
     * If no holes were found an empty JTS GeometryCollection is returned.
     *
     * @param geometryNode
     *            potentially existing holes will be extracted from the geometry represented by this node (the geometry can
     *            be a Polygon, MultiPolygon, or any other JTS geometry)
     * @return A geometry (JTS Polygon or MultiPolygon) with the holes contained in the given geometry. Can also be an empty
     *         JTS GeometryCollection but not <code>null</code>;
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry holes(ANode geometryNode) throws QueryException {

        com.vividsolutions.jts.geom.Geometry geom = getOrCacheGeometry(geometryNode);
        return holesGeom(geom);
    }

    /**
     * Identifies the holes contained in the given geometry and returns them as a JTS geometry. If holes were found a union
     * is built, to ensure that the result is a valid JTS Polygon or JTS MultiPolygon. If no holes were found an empty JTS
     * GeometryCollection is returned.
     *
     * @param geom
     *            potentially existing holes will be extracted from this geometry (can be a Polygon, MultiPolygon, or any
     *            other JTS geometry)
     * @return A geometry (JTS Polygon or MultiPolygon) with the holes contained in the given geometry. Can also be an empty
     *         JTS GeometryCollection but not <code>null</code>;
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry holesGeom(com.vividsolutions.jts.geom.Geometry geom) {

        if (isEmptyGeom(geom)) {

            return jtsTransformer.emptyJTSGeometry();

        } else {

            List<com.vividsolutions.jts.geom.Geometry> holes = computeHoles(geom);

            if (holes.isEmpty()) {
                return jtsTransformer.emptyJTSGeometry();
            } else {
                // create union of holes and return it
                return CascadedPolygonUnion.union(holes);
            }
        }
    }

    /**
     * Identifies the holes contained in the given geometry and returns them as polygons within a JTS geometry collection.
     *
     * @param geom
     *            potentially existing holes will be extracted from this geometry (can be a Polygon, MultiPolygon, or any
     *            other JTS geometry)
     * @return A JTS geometry collection with the holes (as polygons) contained in the given geometry. Can be empty but not
     *         <code>null</code>;
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry holesAsGeometryCollection(com.vividsolutions.jts.geom.Geometry geom) {

        if (isEmptyGeom(geom)) {

            return jtsTransformer.emptyJTSGeometry();

        } else {

            List<com.vividsolutions.jts.geom.Geometry> holes = computeHoles(geom);

            if (holes.isEmpty()) {
                return jtsTransformer.emptyJTSGeometry();
            } else {
                return jtsTransformer.toJTSGeometryCollection(holes, true);
            }
        }
    }

    private List<com.vividsolutions.jts.geom.Geometry> computeHoles(com.vividsolutions.jts.geom.Geometry geom) {

        final List<com.vividsolutions.jts.geom.Geometry> holes = new ArrayList<>();

        final List<com.vividsolutions.jts.geom.Polygon> extractedPolygons = new ArrayList<>();

        GeometryExtracter.extract(geom, com.vividsolutions.jts.geom.Polygon.class, extractedPolygons);

        if (!extractedPolygons.isEmpty()) {

            // get holes as polygons

            for (com.vividsolutions.jts.geom.Polygon polygon : extractedPolygons) {

                // check that polygon has holes
                if (polygon.getNumInteriorRing() > 0) {

                    // for each hole, convert it to a polygon
                    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                        com.vividsolutions.jts.geom.LineString ls = polygon.getInteriorRingN(i);
                        com.vividsolutions.jts.geom.Polygon holeAsPolygon = jtsTransformer.toJTSPolygon(ls);
                        holes.add(holeAsPolygon);
                    }
                }
            }
        }

        return holes;
    }

    /**
     * Check if a given geometry node is valid.
     *
     * @param geometryNode
     *            the geometry element
     * @return <code>true</code> if the given node represents a valid geometry, else <code>false
     *     </code>.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public boolean isValid(ANode geometryNode) throws QueryException {
        final String validationResult = validate(geometryNode);
        return validationResult.toLowerCase().indexOf('f') <= -1;
    }

    /**
     * Tests if the first geometry relates to the second geometry as defined by the given intersection pattern.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param arg1
     *            represents the first geometry, encoded as a GML geometry element
     * @param arg2
     *            represents the second geometry, encoded as a GML geometry element
     * @param intersectionPattern
     *            the pattern against which to check the intersection matrix for the two geometries
     *            (IxI,IxB,IxE,BxI,BxB,BxE,ExI,ExB,ExE)
     * @return <code>true</code> if the DE-9IM intersection matrix for the two geometries matches the
     *         <code>intersectionPattern</code>, else <code>false</code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean relate(final ANode arg1, final ANode arg2, String intersectionPattern) throws QueryException {
        try {
            checkIntersectionPattern(intersectionPattern);
            return applyRelate(arg1, arg2, intersectionPattern);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing relate(Value,Value,String). Message is: " + e.getMessage(), e);
        }
    }

    @Contract("null -> !null")
    private com.vividsolutions.jts.geom.Geometry getCachedGeometryFromNodeOrTransform(final Object dbNodeOrOther)
            throws QueryException {
        if (dbNodeOrOther instanceof DBNode) {
            return getOrCacheGeometry((DBNode) dbNodeOrOther);
        } else {
            return jtsTransformer.singleObjectToJTSGeometry(dbNodeOrOther);
        }
    }

    private boolean applyRelate(Object nodeOrJtsGeometry1, Object nodeOrJtsGeometry2, String intersectionPattern)
            throws QueryException {
        final com.vividsolutions.jts.geom.Geometry g1 = getCachedGeometryFromNodeOrTransform(nodeOrJtsGeometry1);
        final com.vividsolutions.jts.geom.Geometry g2 = getCachedGeometryFromNodeOrTransform(nodeOrJtsGeometry2);
        return g1.relate(g2, intersectionPattern);
    }

    /**
     * Tests if one geometry relates to a list of geometries as defined by the given intersection pattern. Whether a match
     * is required for all or just one of these is controlled via parameter.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param value1
     *            represents the first geometry, encoded as a GML geometry element
     * @param value2
     *            represents a list of geometries, encoded as GML geometry elements
     * @param intersectionPattern
     *            the pattern against which to check the intersection matrix for the geometries
     *            (IxI,IxB,IxE,BxI,BxB,BxE,ExI,ExB,ExE)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship defined by the
     *            <code>intersectionPattern</code> for all geometries in arg2, else <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean relate(final Value value1, final Value value2, String intersectionPattern, boolean matchAll)
            throws QueryException {
        if (value1 instanceof Empty || value2 instanceof Empty) {
            return false;
        }
        try {
            checkIntersectionPattern(intersectionPattern);
            return relateMatch(value1, value2, intersectionPattern, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing relate(Value,Value,String,boolean). Message is: " + e.getMessage(), e);
        }
    }

    private boolean relateMatch(Object arg1, Object arg2, String intersectionPattern, boolean matchAll)
            throws QueryException {
        try {
            final Collection<Object> arg1_list = toObjectCollection(arg1);
            final Collection<Object> arg2_list = toObjectCollection(arg2);

            boolean allMatch = true;
            outer: for (Object o1 : arg1_list) {
                for (Object o2 : arg2_list) {

                    if (matchAll) {
                        if (applyRelate(o1, o2, intersectionPattern)) {
                            /*
                             * check the next geometry pair to see if it also satisfies the spatial relationship
                             */
                        } else {
                            allMatch = false;
                            break outer;
                        }

                    } else {

                        if (applyRelate(o1, o2, intersectionPattern)) {
                            return true;
                        } else {
                            /*
                             * check the next geometry pair to see if it satisfies the spatial relationship
                             */
                        }
                    }
                }
            }

            if (matchAll) {
                return allMatch;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Tests if the first geometry relates to the second geometry as defined by the given intersection pattern.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents the second geometry, encoded as a JTS geometry object
     * @param intersectionPattern
     *            the pattern against which to check the intersection matrix for the two geometries
     *            (IxI,IxB,IxE,BxI,BxB,BxE,ExI,ExB,ExE)
     * @return <code>true</code> if the DE-9IM intersection matrix for the two geometries matches the
     *         <code>intersectionPattern</code>, else <code>false</code>.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean relateGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, String intersectionPattern) throws QueryException {

        try {
            checkIntersectionPattern(intersectionPattern);
            return applyRelate(geom1, geom2, intersectionPattern);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing relateGeomGeom(Geometry,Geometry,String). Message is: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Tests if one geometry relates to a list of geometries as defined by the given intersection pattern. Whether a match
     * is required for all or just one of these is controlled via parameter.
     *
     * @param geom1
     *            represents the first geometry, encoded as a JTS geometry object
     * @param geom2
     *            represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
     * @param intersectionPattern
     *            the pattern against which to check the intersection matrix for the geometries
     *            (IxI,IxB,IxE,BxI,BxB,BxE,ExI,ExB,ExE)
     * @param matchAll
     *            <code>true</code> if arg1 must fulfill the spatial relationship for all geometries in arg2, else
     *            <code>false</code>
     * @return <code>true</code> if the conditions are met, else <code>false</code>. <code>false
     *     </code> will also be returned if arg2 is empty.
     * @throws QueryException
     *             If an exception occurred while computing the spatial operation.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean relateGeomGeom(com.vividsolutions.jts.geom.Geometry geom1,
            com.vividsolutions.jts.geom.Geometry geom2, String intersectionPattern, boolean matchAll)
            throws QueryException {
        try {
            checkIntersectionPattern(intersectionPattern);
            return relateMatch(geom1, geom2, intersectionPattern, matchAll);
        } catch (Exception e) {
            throw new GmlGeoXException(
                    "Exception while executing relateGeomGeom(Geometry,Geometry,String,boolean). Message is: "
                            + e.getMessage(),
                    e);

        }
    }

    private void checkIntersectionPattern(String intersectionPattern) throws QueryException {
        if (intersectionPattern == null) {
            throw new GmlGeoXException("intersectionPattern is null.");
        } else {
            final Matcher m = INTERSECTIONPATTERN.matcher(intersectionPattern.trim());
            if (!m.matches()) {
                throw new GmlGeoXException(
                        "intersectionPattern does not match the regular expression, which is: [0-2\\\\*TF]{9}");
            }
        }
    }

    /**
     * Computes the intersection between the first and the second geometry node.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geometry1
     *            represents the first geometry
     * @param geometry2
     *            represents the second geometry
     * @return the point-set common to the two geometries
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry intersection(final ANode geometry1, final ANode geometry2)
            throws QueryException {
        try {
            final com.vividsolutions.jts.geom.Geometry geom1 = getOrCacheGeometry(geometry1);
            final com.vividsolutions.jts.geom.Geometry geom2 = getOrCacheGeometry(geometry2);
            return geom1.intersection(geom2);
        } catch (final Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Computes the intersection between the first and the second geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geometry1
     *            the first geometry
     * @param geometry2
     *            the second geometry
     * @return the point-set common to the two geometries
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry intersectionGeomGeom(
            final com.vividsolutions.jts.geom.Geometry geometry1, final com.vividsolutions.jts.geom.Geometry geometry2)
            throws QueryException {
        try {
            return geometry1.intersection(geometry2);
        } catch (final Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Computes the difference between the first and the second geometry node.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geometry1
     *            represents the first geometry
     * @param geometry2
     *            represents the second geometry
     * @return the closure of the point-set of the points contained in geometry1 that are not contained in geometry2.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry difference(final ANode geometry1, final ANode geometry2)
            throws QueryException {
        try {
            final com.vividsolutions.jts.geom.Geometry geom1 = getOrCacheGeometry(geometry1);
            final com.vividsolutions.jts.geom.Geometry geom2 = getOrCacheGeometry(geometry2);
            return geom1.difference(geom2);
        } catch (final Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * @see #boundaryGeom(com.vividsolutions.jts.geom.Geometry)
     * @param geometryNode
     *            the geometry element
     * @return the closure of the combinatorial boundary of this Geometry
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry boundary(final ANode geometryNode) throws QueryException {
        return boundaryGeom(getOrCacheGeometry(geometryNode));
    }

    /**
     * Returns the area of the geometry encoded in the given geometry node. Only areal geometries have a non-zero area.
     *
     * @see #areaGeom(com.vividsolutions.jts.geom.Geometry)
     * @param geometryNode
     *            the geometry element
     * @return the area of the geometry
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public double area(final ANode geometryNode) throws QueryException {
        com.vividsolutions.jts.geom.Geometry geom = getOrCacheGeometry(geometryNode);
        return geom.getArea();
    }

    /**
     * Returns the length of the given geometry. For a linear geometry, its length is returned. For an areal geometry, the
     * length of its perimeter is returned. For (multi-) point geometries, 0.0 is returned.
     *
     * @param jtsGeom
     *            the JTS geometry object
     * @return the length of the geometry
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public double lengthGeom(final com.vividsolutions.jts.geom.Geometry jtsGeom) throws QueryException {
        return jtsGeom.getLength();
    }

    /**
     * Returns the area of the given geometry. Only areal geometries have a non-zero area.
     *
     * @param geometry
     *            the geometry
     * @return the area of the geometry
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public double areaGeom(final com.vividsolutions.jts.geom.Geometry geometry) throws QueryException {
        try {
            if (geometry == null) {
                return 0;
            } else {
                return geometry.getArea();
            }
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Returns the centroid of the geometry represented by the given geometry node. The centroid is equal to the centroid of
     * the set of component geometries of highest dimension (since the lower-dimension geometries contribute zero "weight"
     * to the centroid). The centroid of an empty geometry is POINT EMPTY.
     *
     * @see #centroidGeom(com.vividsolutions.jts.geom.Geometry)
     * @param geometryNode
     *            the geometry element
     * @return the centroid of the geometry
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Point centroid(final ANode geometryNode) throws QueryException {
        return centroidGeom(getOrCacheGeometry(geometryNode));
    }

    /**
     * Returns the centroid of the given geometry. The centroid is equal to the centroid of the set of component geometries
     * of highest dimension (since the lower-dimension geometries contribute zero "weight" to the centroid). The centroid of
     * an empty geometry is POINT EMPTY.
     *
     * @param geometry
     *            the geometry
     * @return the centroid of the geometry
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Point centroidGeom(final com.vividsolutions.jts.geom.Geometry geometry)
            throws QueryException {
        try {
            return geometry.getCentroid();
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Returns the boundary, or an empty geometry of appropriate dimension if the given geometry is empty or has no boundary
     * (e.g. a curve whose end points are equal). (In the case of zero-dimensional geometries, an empty GeometryCollection
     * is returned.) For a discussion of this function, see the OpenGIS SimpleFeatures Specification. As stated in SFS
     * Section 2.1.13.1, "the boundary of a Geometry is a set of Geometries of the next lower dimension."
     *
     * @param geometry
     *            the geometry
     * @return the closure of the combinatorial boundary of this Geometry
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry boundaryGeom(final com.vividsolutions.jts.geom.Geometry geometry)
            throws QueryException {
        try {
            if (geometry == null) {
                return jtsTransformer.emptyJTSGeometry();
            } else {
                return geometry.getBoundary();
            }
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Computes the difference between the first and the second geometry.
     *
     * @param geometry1
     *            the first geometry
     * @param geometry2
     *            the second geometry
     * @return the closure of the point-set of the points contained in geometry1 that are not contained in geometry2.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry differenceGeomGeom(final com.vividsolutions.jts.geom.Geometry geometry1,
            final com.vividsolutions.jts.geom.Geometry geometry2) throws QueryException {
        try {
            return geometry1.difference(geometry2);
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    // public com.vividsolutions.jts.geom.Geometry detailedDifferenceGeomGeom(final
    // com.vividsolutions.jts.geom.Geometry
    // geometry1,
    // final com.vividsolutions.jts.geom.Geometry geometry2) throws QueryException {
    //
    // com.vividsolutions.jts.geom.Geometry[] g1geoms =
    // flattenAllGeometryCollections(
    // geometry1);
    // com.vividsolutions.jts.geom.Geometry[] g2geoms =
    // flattenAllGeometryCollections(
    // geometry2);
    //
    // List<com.vividsolutions.jts.geom.Geometry> result = new ArrayList<>();
    //
    // for (com.vividsolutions.jts.geom.Geometry g1g : g1geoms) {
    //
    // com.vividsolutions.jts.geom.Geometry diffResult = g1g;
    //
    // for (com.vividsolutions.jts.geom.Geometry g2g : g2geoms) {
    //
    // try {
    // diffResult = diffResult.difference(g2g);
    // } catch (Exception e) {
    // throw new QueryException(e.getClass().getName()
    // + " while computing the difference. Message is: " + e.getMessage());
    // }
    //
    // if (diffResult.isEmpty()) {
    // break;
    // }
    // }
    //
    // if (!diffResult.isEmpty()) {
    // result.add(diffResult);
    // }
    // }
    //
    // if (result.isEmpty()) {
    // return null;
    // } else {
    // return unionGeom(result);
    // }
    // }

    /**
     * Computes the envelope of a geometry.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geometryNode
     *            represents the geometry
     * @return The bounding box, an array { x1, y1, x2, y2 }
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public Object[] envelope(final ANode geometryNode) throws QueryException {
        /* Try lookup in envelope map first. */
        final DBNodeRef geometryNodeEntry = this.dbNodeRefFactory.createDBNodeEntry((DBNode) geometryNode);
        if (geometryCache.hasEnvelope(geometryNodeEntry)) {
            Envelope env = geometryCache.getEnvelope(geometryNodeEntry);
            final Object[] res = {env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()};
            return res;
        } else {
            /* Get JTS geometry and cache the envelope. */
            com.vividsolutions.jts.geom.Geometry geom = getOrCacheGeometry(geometryNode, geometryNodeEntry);
            geometryCache.addEnvelope(geometryNodeEntry, geom.getEnvelopeInternal());
            return envelopeGeom(geom);
        }
    }

    /**
     * Computes the envelope of a geometry.
     *
     * @param geometry
     *            the geometry
     * @return The bounding box, as an array { x1, y1, x2, y2 }
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public Object[] envelopeGeom(com.vividsolutions.jts.geom.Geometry geometry) throws QueryException {
        try {
            final Envelope env = geometry.getEnvelopeInternal();
            final Object[] res = {env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()};
            return res;
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Retrieves the end points of the curve represented by the geometry node.
     * <p>
     * NOTE: This is different to computing the boundary of a curve in case that the curve end points are equal (in that
     * case, the curve does not have a boundary).
     *
     * @param geomNode
     *            the geometry element
     * @return An array with the two end points of the curve geometry (node); can be empty if the given geometry nodes does
     *         not represent a single curve.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Point[] curveEndPoints(ANode geomNode) throws QueryException {

        try {

            final Geometry geom = deegreeTransformer.parseGeometry(geomNode);
            final List<com.vividsolutions.jts.geom.Point> res = new ArrayList<>();

            if (geom instanceof Curve) {
                final Curve curve = (Curve) geom;
                final Points curveControlPoints = deegreeTransformer.getControlPoints(curve);
                final Point pStart = curveControlPoints.get(0);
                final Point pEnd = curveControlPoints.get(curveControlPoints.size() - 1);
                res.add((com.vividsolutions.jts.geom.Point) jtsTransformer.toJTSGeometry(pStart));
                res.add((com.vividsolutions.jts.geom.Point) jtsTransformer.toJTSGeometry(pEnd));
            }

            return res.toArray(new com.vividsolutions.jts.geom.Point[res.size()]);

        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Retrieves the JTS representations of all curve components contained in the geometry node.
     *
     * @param geomNode
     *            the geometry element
     * @return An array with the JTS representations of the curve components of the geometry (node); can be empty if the
     *         given geometry node does not contain any curve component.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry[] curveComponents(ANode geomNode) throws QueryException {

        try {

            final Collection<Curve> curves = deegreeTransformer.getCurveComponents(geomNode);
            final List<com.vividsolutions.jts.geom.Geometry> res = new ArrayList<>();

            for (Curve c : curves) {
                res.add(jtsTransformer.toJTSGeometry(c));
            }

            return res.toArray(new com.vividsolutions.jts.geom.Geometry[res.size()]);

        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Searches the default spatial r-tree index for items whose minimum bounding box intersects with the rectangle defined
     * by the given coordinates.
     *
     * @param minx
     *            represents the minimum value on the first coordinate axis; a number
     * @param miny
     *            represents the minimum value on the second coordinate axis; a number
     * @param maxx
     *            represents the maximum value on the first coordinate axis; a number
     * @param maxy
     *            represents the maximum value on the second coordinate axis; a number
     * @return the node set of all items in the envelope
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public DBNode[] search(Object minx, Object miny, Object maxx, Object maxy) throws QueryException {
        return search(DEFAULT_SPATIAL_INDEX, minx, miny, maxx, maxy);
    }

    /**
     * Searches the named spatial r-tree index for items whose minimum bounding box intersects with the rectangle defined by
     * the given coordinates.
     *
     * @param indexName
     *            Identifies the index. <code>null</code> or the empty string identifies the default index.
     * @param minx
     *            represents the minimum value on the first coordinate axis; a number
     * @param miny
     *            represents the minimum value on the second coordinate axis; a number
     * @param maxx
     *            represents the maximum value on the first coordinate axis; a number
     * @param maxy
     *            represents the maximum value on the second coordinate axis; a number
     * @return the node set of all items in the envelope
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public DBNode[] search(final String indexName, Object minx, Object miny, Object maxx, Object maxy)
            throws QueryException {
        try {
            return performSearch(indexName, toDoubleOrZero(minx), toDoubleOrZero(miny), toDoubleOrZero(maxx),
                    toDoubleOrZero(maxy));
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    private static double toDoubleOrZero(final Object dbl) {
        if (dbl instanceof Number) {
            return ((Number) dbl).doubleValue();
        }
        return 0.0;
    }

    @NotNull
    private DBNode[] performSearch(String indexName, double x1, double y1, double x2, double y2) {
        final List<DBNode> nodelist = geometryCache.search(indexName, Geometries.rectangle(x1, y1, x2, y2),
                this.dbNodeRefLookup);

        if (debug && ++count % 5000 == 0) {
            String debugIndexName = indexName != null ? indexName : "default";
            logMemUsage("GmlGeoX#search " + count + ". Box: (" + x1 + ", " + y1 + ") (" + x2 + ", " + y2 + ")"
                    + ". Hits: " + nodelist.size() + " (in index '" + debugIndexName + "')");
        }
        return nodelist.toArray(new DBNode[0]);
    }

    /**
     * Searches the default spatial r-tree index for items whose minimum bounding box intersects with the the minimum
     * bounding box of the given geometry node.
     *
     * @param geometryNode
     *            the geometry element
     * @return the node set of all items in the envelope
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public DBNode[] search(ANode geometryNode) throws QueryException {
        return search(DEFAULT_SPATIAL_INDEX, geometryNode);
    }

    /**
     * Searches the named spatial r-tree index for items whose minimum bounding box intersects with the the minimum bounding
     * box of the given geometry node.
     *
     * @param indexName
     *            Identifies the index. <code>null</code> or the empty string identifies the default index.
     * @param geometryNode
     *            the geometry element
     * @return the node set of all items in the envelope
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public DBNode[] search(final String indexName, ANode geometryNode) throws QueryException {
        if (geometryNode == null) {
            return new DBNode[0];
        }
        /* Try lookup in envelope map first. */
        final DBNodeRef entry = dbNodeRefFactory.createDBNodeEntry((DBNode) geometryNode);
        if (geometryCache.hasEnvelope(entry)) {
            return search(indexName, geometryCache.getEnvelope(entry));
        } else {
            /* Get JTS geometry and cache the envelope. */
            final com.vividsolutions.jts.geom.Geometry geom = getOrCacheGeometry(geometryNode, entry);
            if (geom.isEmpty()) {
                throw new GmlGeoXException("Geometry determined for the given node is empty "
                        + "(ensure that the given node is a geometry node that represents a non-empty geometry). "
                        + "Cannot perform a search based upon an empty geometry.");
            }
            geometryCache.addEnvelope(entry, geom.getEnvelopeInternal());
            return searchGeom(indexName, geom);
        }
    }

    /**
     * Searches the default spatial r-tree index for items whose minimum bounding box intersects with the the minimum
     * bounding box of the given geometry.
     *
     * @param geom
     *            the geometry
     * @return the node set of all items in the envelope
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public DBNode[] searchGeom(com.vividsolutions.jts.geom.Geometry geom) throws QueryException {
        return searchGeom(DEFAULT_SPATIAL_INDEX, geom);
    }

    /**
     * Searches the named spatial r-tree index for items whose minimum bounding box intersects with the the minimum bounding
     * box of the given geometry.
     *
     * @param indexName
     *            Identifies the index. <code>null</code> or the empty string identifies the default index.
     * @param geom
     *            the geometry
     * @return the node set of all items in the envelope
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public DBNode[] searchGeom(final String indexName, com.vividsolutions.jts.geom.Geometry geom)
            throws QueryException {
        if (geom.isEmpty()) {
            throw new GmlGeoXException("Geometry is empty. Cannot perform a search based upon an empty geometry.");
        }
        return search(indexName, geom.getEnvelopeInternal());
    }

    private DBNode[] search(final String indexName, final Envelope env) throws QueryException {
        double x1 = env.getMinX();
        double x2 = env.getMaxX();
        double y1 = env.getMinY();
        double y2 = env.getMaxY();
        return performSearch(indexName, x1, y1, x2, y2);
    }

    /**
     * Returns all items in the default spatial r-tree index.
     *
     * @return the node set of all items in the index
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public DBNode[] search() throws QueryException {
        // Do we really search here???
        return searchInIndex(DEFAULT_SPATIAL_INDEX);
    }

    /**
     * Returns all items in the named spatial r-tree index.
     *
     * @param indexName
     *            Identifies the index. <code>null</code> or the empty string identifies the default index.
     * @return the node set of all items in the index
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public DBNode[] searchInIndex(final String indexName) throws QueryException {
        // Do we really search here???
        try {
            logMemUsage("GmlGeoX#search.start " + count + ".");
            final List<DBNode> nodelist = geometryCache.getAll(indexName, this.dbNodeRefLookup);
            String debugIndexName = indexName != null ? indexName : "default";
            logMemUsage(
                    "GmlGeoX#search " + count + ". Hits: " + nodelist.size() + " (in index '" + debugIndexName + "')");

            return nodelist.toArray(new DBNode[0]);
        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Logs memory information if Logger is enabled for the DEBUG level
     *
     * @param progress
     *            status string
     */
    private void logMemUsage(final String progress) {
        if (debug) {
            final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            memory.gc();
            logger.debug(progress + ". Memory: " + Math.round(memory.getHeapMemoryUsage().getUsed() / 1048576)
                    + " MB of " + Math.round(memory.getHeapMemoryUsage().getMax() / 1048576) + " MB.");
        }
    }

    /**
     * Set cache size for geometries. The cache will be reset.
     *
     * @param size
     *            the size of the geometry cache; default is 100000
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void cacheSize(final Object size) throws QueryException {
        final int newSize;
        if (size instanceof BigInteger) {
            newSize = ((BigInteger) size).intValue();
        } else if (size instanceof Integer) {
            newSize = (Integer) size;
        } else {
            throw new GmlGeoXException("Unsupported parameter type: " + size.getClass().getName());
        }
        geometryCache.resetCache(newSize);
    }

    /**
     * Get the current size of the geometry cache.
     *
     * @return the size of the geometry cache
     */
    @Requires(Permission.NONE)
    public int getCacheSize() {
        return geometryCache.getCacheSize();
    }

    /**
     * Indexes a feature geometry, using the default index.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param node
     *            represents the indexed item node (can be the gml:id of a GML feature element, or the element itself)
     * @param geometry
     *            represents the GML geometry to index
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void index(final ANode node, final ANode geometry) throws QueryException {
        index(DEFAULT_SPATIAL_INDEX, node, geometry);
    }

    /**
     * Removes the named spatial index. WARNING: Be sure you know what you are doing.
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void removeIndex(final String indexName) throws QueryException {
        geometryCache.removeIndex(indexName);
        /*
         * NOTE: We do not remove a possibly existing entry in geometryIndexEntriesByIndexName, for a case in which the spatial
         * index exists (e.g. from previous tests), but the query developer prepares the spatial index with new entries before
         * removing the 'old' index. The typical sequence, however, should be to remove the old index first, then prepare and
         * build the index with the new entries.
         */
    }

    /**
     * Indexes a feature geometry, using the named spatial index.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @param node
     *            represents the indexed item node (can be the gml:id of a GML feature element, or the element itself)
     * @param geometry
     *            represents the GML geometry to index
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void index(final String indexName, final ANode node, final ANode geometry) throws QueryException {

        if (node instanceof DBNode && geometry instanceof DBNode) {
            try {
                final com.vividsolutions.jts.geom.Geometry _geom = getOrCacheGeometry(geometry);
                final Envelope env = _geom.getEnvelopeInternal();

                if (!env.isNull()) {
                    final DBNodeRef nodeEntry = this.dbNodeRefFactory.createDBNodeEntry((DBNode) node);
                    if (env.getHeight() == 0.0 && env.getWidth() == 0.0) {
                        geometryCache.index(indexName, nodeEntry, Geometries.point(env.getMinX(), env.getMinY()));
                    } else {
                        geometryCache.index(indexName, nodeEntry,
                                Geometries.rectangle(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()));
                    }

                    // also cache the envelope
                    final DBNodeRef geomNodeEntry = this.dbNodeRefFactory.createDBNodeEntry((DBNode) geometry);
                    geometryCache.addEnvelope(geomNodeEntry, env);
                }

                if (debug && geometryCache.indexSize(indexName) % 5000 == 0) {
                    String debugIndexName = indexName != null ? indexName : "default";
                    logMemUsage("GmlGeoX#index progress (for index '" + debugIndexName + "'): "
                            + geometryCache.indexSize(indexName));
                }
            } catch (final XMLParsingException e) {
                throw new GmlGeoXException(e);
            }
        }
    }

    /**
     * Checks if the coordinates of the given {@code point} are equal (comparing x, y, and z) to the coordinates of one of
     * the points that define the given {@code geometry}.
     *
     * @param point
     *            The point whose coordinates are checked against the coordinates of the points of {@code geometry}
     * @param geometry
     *            The geometry whose points are checked to see if one of them has coordinates equal to that of {@code point}
     * @return <code>true</code> if the coordinates of the given {@code point} are equal to the coordinates of one of the
     *         points that define {@code geometry}, else <code>false</code>
     */
    @Requires(Permission.NONE)
    @Deterministic
    public boolean pointCoordInGeometryCoords(com.vividsolutions.jts.geom.Point point,
            com.vividsolutions.jts.geom.Geometry geometry) {
        final Coordinate pointCoord = point.getCoordinate();
        final Coordinate[] geomCoords = geometry.getCoordinates();
        for (Coordinate geomCoord : geomCoords) {
            if (pointCoord.equals3D(geomCoord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if for each curve of the given geomNode a minimum (defined by parameter minMatchesPerCurve) number of
     * identical curves (same control points - ignoring curve orientation) from the otherGeomsNodes exists.
     *
     * @param geomNode
     *            GML geometry node
     * @param otherGeomsNodes
     *            one or more database nodes representing GML geometries
     * @param minMatchesPerCurve
     *            the minimum number of matching identical curves that must be found for each curve from the geomNode
     * @return <code>null</code>, if all curves are matched correctly, otherwise the JTS geometry of the first curve from
     *         geomNode which is not covered by the required number of identical curves from otherGeomsNodes
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry curveUnmatchedByIdenticalCurvesMin(ANode geomNode,
            Value otherGeomsNodes, int minMatchesPerCurve) throws QueryException {

        if (geomNode == null) {
            throw new GmlGeoXException("Parameter geomNode must contain a database node.");
        }

        if (otherGeomsNodes == null || otherGeomsNodes.isEmpty()) {
            throw new GmlGeoXException("Parameter otherGeomsNodes must contain one or more database nodes.");
        }

        final Collection<Curve> curvesToMatch = deegreeTransformer.getCurveComponents(geomNode);
        final List<Curve> otherCurves = deegreeTransformer.getCurveComponents(otherGeomsNodes);

        if (minMatchesPerCurve <= 0) {
            throw new GmlGeoXException("Parameter minMatchesPerCurve must be greater than 0.");
        }

        try {

            final STRtree otherCurvesIndex = createSpatialIndex(otherCurves);

            for (Curve c : curvesToMatch) {

                final com.vividsolutions.jts.geom.Geometry c_jts = jtsTransformer.toJTSGeometry(c);

                boolean matchFound = isValidIdenticalCurveCoverage(c, c_jts, otherCurvesIndex, minMatchesPerCurve,
                        false);

                if (!matchFound) {
                    return jtsTransformer.toJTSGeometry(c_jts);
                }
            }

            return null;

        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Checks if for each curve of the given geomNode a maximum (defined by parameter maxMatchesPerCurve) number of
     * identical curves (same control points - ignoring curve orientation) from the otherGeomsNodes exists.
     *
     * @param geomNode
     *            GML geometry node
     * @param otherGeomsNodes
     *            one or more database nodes representing GML geometries
     * @param maxMatchesPerCurve
     *            the maximum number of matching identical curves that are allowed to be found for each curve from the
     *            geomNode
     * @return <code>null</code>, if all curves are matched correctly, otherwise the JTS geometry of the first curve from
     *         geomNode which is covered by more than the allowed number of identical curves from otherGeomsNodes
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry curveUnmatchedByIdenticalCurvesMax(ANode geomNode,
            Value otherGeomsNodes, int maxMatchesPerCurve) throws QueryException {

        if (geomNode == null) {
            throw new GmlGeoXException("Parameter geomNode must contain a database node.");
        }

        if (otherGeomsNodes == null || otherGeomsNodes.isEmpty()) {
            throw new GmlGeoXException("Parameter otherGeomsNodes must contain one or more database nodes.");
        }

        final Collection<Curve> curvesToMatch = deegreeTransformer.getCurveComponents(geomNode);
        final List<Curve> otherCurves = deegreeTransformer.getCurveComponents(otherGeomsNodes);

        if (maxMatchesPerCurve <= 0) {
            throw new GmlGeoXException("Parameter maxMatchesPerCurve must be greater than 0.");
        }

        try {

            final STRtree otherCurvesIndex = createSpatialIndex(otherCurves);

            for (Curve c : curvesToMatch) {

                final com.vividsolutions.jts.geom.Geometry c_jts = jtsTransformer.toJTSGeometry(c);

                boolean matchFound = isValidIdenticalCurveCoverage(c, c_jts, otherCurvesIndex, maxMatchesPerCurve,
                        true);

                if (!matchFound) {
                    return jtsTransformer.toJTSGeometry(c_jts);
                }
            }

            return null;

        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * @param curves
     * @return spatial index, where each item is of type ImmutablePair&lt;com.vividsolutions.jts.geom.Geometry, Curve&gt;
     * @throws UnsupportedGeometryTypeException
     */
    private STRtree createSpatialIndex(List<Curve> curves) throws UnsupportedGeometryTypeException {

        final STRtree curvesIndex = new STRtree();

        for (final Curve c : curves) {
            final com.vividsolutions.jts.geom.Geometry c_jts = jtsTransformer.toJTSGeometry(c);
            curvesIndex.insert(c_jts.getEnvelopeInternal(),
                    new ImmutablePair<com.vividsolutions.jts.geom.Geometry, Curve>(c_jts, c));
        }

        return curvesIndex;
    }

    /**
     * @param curve
     * @param c_jts
     *            JTS geometry of the curve
     * @param index
     * @param numberOfIdenticalCurvesToDetect
     * @param isMaxNumberToDetect
     *            <code>true</code>, if at most numberOfIdenticalCurvesToDetect matches are allowed, otherwise
     *            <code>false</code> (then at least numberOfIdenticalCurvesToDetect matches must be found)
     * @return
     * @throws UnsupportedGeometryTypeException
     */
    private boolean isValidIdenticalCurveCoverage(Curve curve, com.vividsolutions.jts.geom.Geometry c_jts,
            STRtree index, int numberOfIdenticalCurvesToDetect, boolean isMaxNumberToDetect)
            throws UnsupportedGeometryTypeException {

        final Points curveControlPoints = deegreeTransformer.getControlPoints(curve);

        // get other curves from spatial index to compare
        @SuppressWarnings("rawtypes")
        final List results = index.query(c_jts.getEnvelopeInternal());

        int matchCount = 0;

        for (Object o : results) {

            @SuppressWarnings("unchecked")
            final ImmutablePair<com.vividsolutions.jts.geom.Geometry, Curve> pair = (ImmutablePair<com.vividsolutions.jts.geom.Geometry, Curve>) o;
            final com.vividsolutions.jts.geom.Geometry otherCurve_jts = pair.left;

            if (c_jts.equals(otherCurve_jts)) {

                /*
                 * So the two JTS geometries (of the two curves) are spatially equal. However, we need to ensure that the
                 * control points are identical as well (ignoring orientation).
                 */

                final Curve otherCurve_deegree = pair.right;
                final Points otherCurveControlPoints = deegreeTransformer.getControlPoints(otherCurve_deegree);

                if (curveControlPoints.size() == otherCurveControlPoints.size()) {

                    /*
                     * NOTE: deegree.Point equals(..) implementation really just compares the coordinates. So no specific
                     * overhead in doing so.
                     */

                    boolean pointsMatch = true;
                    for (int i = 0; i < curveControlPoints.size(); i++) {
                        if (!curveControlPoints.get(i).equals(otherCurveControlPoints.get(i))) {
                            pointsMatch = false;
                            break;
                        }
                    }

                    if (!pointsMatch) {

                        // try with reversed order of control points from curve of other geometry
                        pointsMatch = true;
                        final int otherGeomCurveControlPointsSize = otherCurveControlPoints.size();
                        for (int i = 0; i < curveControlPoints.size(); i++) {
                            if (!curveControlPoints.get(i)
                                    .equals(otherCurveControlPoints.get(otherGeomCurveControlPointsSize - i - 1))) {
                                pointsMatch = false;
                                break;
                            }
                        }
                    }

                    if (pointsMatch) {
                        matchCount++;
                    }
                }
            }

            // determine if we can skip processing remaining results from index search
            if (isMaxNumberToDetect) {

                // at most numberOfIdenticalCurvesToDetect matches are allowed
                if (matchCount > numberOfIdenticalCurvesToDetect) {
                    return false;
                }

            } else {

                // at least numberOfIdenticalCurvesToDetect matches must be found
                if (matchCount >= numberOfIdenticalCurvesToDetect) {
                    return true;
                }
            }
        }

        if (isMaxNumberToDetect) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if for each curve of the given geomNode an identical curve (same control points - ignoring curve orientation)
     * from the otherGeomNodes exists.
     *
     * @param geomNode
     *            GML geometry node
     * @param otherGeomNodes
     *            one or more database nodes representing GML geometries
     * @return <code>null</code>, if full coverage was determined, otherwise the JTS geometry of the first curve from
     *         geomNode which is not covered by an identical curve from otherGeomNodes
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry determineIncompleteCoverageByIdenticalCurveComponents(ANode geomNode,
            Value otherGeomNodes) throws QueryException {

        try {

            /*
             * Ensure that other geometry nodes only consist of ANodes because we need to compare the original control points.
             */
            @SuppressWarnings("rawtypes")
            final Collection otherGeomNodesObjectList = toObjectCollection(otherGeomNodes);
            final Collection<ANode> otherGeomNodes_list = new ArrayList<>(otherGeomNodesObjectList.size());
            for (Object o : otherGeomNodesObjectList) {
                if (!(o instanceof ANode)) {
                    throw new IllegalArgumentException(
                            "Calling this function with an item in the second parameter that is not an ANode is illegal.");
                } else {
                    otherGeomNodes_list.add((ANode) o);
                }
            }

            /* Compute deegree and JTS geometries for the node given as first parameter. */
            final Geometry geom_deegree = deegreeTransformer.parseGeometry(geomNode);
            // try to get JTS geometry for the geometry node from cache
            com.vividsolutions.jts.geom.Geometry geom_jts = null;
            if (geomNode instanceof DBNode) {
                geom_jts = geometryCache.getGeometry(this.dbNodeRefFactory.createDBNodeEntry((DBNode) geomNode));
            }
            if (geom_jts == null) {
                geom_jts = jtsTransformer.toJTSGeometry(geom_deegree);
            }

            /*
             * Create a map of the curve components from the geometry (key: JTS geometry of a curve, value: deegree geometry of
             * that curve)
             */
            final Collection<Curve> geomCurves = deegreeTransformer.getCurveComponents(geom_deegree);
            final Map<com.vividsolutions.jts.geom.Geometry, Curve> geomCurvesMap = new HashMap<>(geomCurves.size());
            for (final Curve c : geomCurves) {
                geomCurvesMap.put(jtsTransformer.toJTSGeometry(c), c);
            }

            /*
             * Now parse and index the curves from the other geometry nodes (second parameter).
             */

            final STRtree otherGeomsCurvesIndex = new STRtree();

            for (ANode otherGeomNode : otherGeomNodes_list) {

                /*
                 * NOTE: We do not directly compute the deegree geometry here, because it is only necessary to do so if the JTS
                 * geometries are equal
                 */
                Geometry otherGeom_deegree = null;
                // try to get JTS geometry for the geometry node from cache
                com.vividsolutions.jts.geom.Geometry otherGeom_jts = null;
                if (otherGeomNode instanceof DBNode) {
                    otherGeom_jts = geometryCache
                            .getGeometry(this.dbNodeRefFactory.createDBNodeEntry((DBNode) otherGeomNode));
                }
                if (otherGeom_jts == null) {
                    otherGeom_deegree = deegreeTransformer.parseGeometry(otherGeomNode);
                    otherGeom_jts = jtsTransformer.toJTSGeometry(otherGeom_deegree);
                }

                /*
                 * Check if the other geometry intersects at all. If not, the other geometry can be ignored.
                 *
                 * TODO: We may not need the overall intersection check ... could be more performant to just use other features
                 * returned from a query of the spatial index, and then just create a spatial index of their curve components.
                 * To do so, we still need to build the JTS geometries of the components (to get their envelopes -
                 * deegree.Geometry.getEnvelope() does that as well). Avoiding the JTS.equals(..) operation to compare curves,
                 * and instead just compare the sequence of points - AND the type of curve segment - might also suffice. ...
                 * That (i.e., also checking curve segment types) may require that we compare the sequence of curve segments,
                 * instead of just the set of all control points.
                 */
                if (geom_jts.intersects(otherGeom_jts)) {

                    if (otherGeom_deegree == null) {
                        otherGeom_deegree = deegreeTransformer.parseGeometry(otherGeomNode);
                    }

                    final Collection<Curve> otherGeomCurves = deegreeTransformer.getCurveComponents(otherGeom_deegree);
                    for (final Curve oc : otherGeomCurves) {
                        final com.vividsolutions.jts.geom.Geometry oc_jts = jtsTransformer.toJTSGeometry(oc);
                        otherGeomsCurvesIndex.insert(oc_jts.getEnvelopeInternal(),
                                new ImmutablePair<com.vividsolutions.jts.geom.Geometry, Curve>(oc_jts, oc));
                    }
                }
            }

            /*
             * Now check that each curve of the geometry from the first parameter is matched by an identical curve from the
             * other geometries.
             */
            for (Entry<com.vividsolutions.jts.geom.Geometry, Curve> e : geomCurvesMap.entrySet()) {

                final com.vividsolutions.jts.geom.Geometry geomCurve_jts = e.getKey();
                final Curve geomCurve_deegree = e.getValue();
                final Points geomCurveControlPoints = deegreeTransformer.getControlPoints(geomCurve_deegree);

                // get other curves from spatial index to compare
                @SuppressWarnings("rawtypes")
                final List results = otherGeomsCurvesIndex.query(geomCurve_jts.getEnvelopeInternal());

                boolean fullMatchFound = false;
                for (Object o : results) {

                    @SuppressWarnings("unchecked")
                    final ImmutablePair<com.vividsolutions.jts.geom.Geometry, Curve> pair = (ImmutablePair<com.vividsolutions.jts.geom.Geometry, Curve>) o;
                    final com.vividsolutions.jts.geom.Geometry otherGeomCurve_jts = pair.left;

                    if (geomCurve_jts.equals(otherGeomCurve_jts)) {

                        /*
                         * So the two JTS geometries (of the two curves) are spatially equal. However, we need to ensure that
                         * the control points are identical as well (ignoring orientation).
                         */

                        final Curve otherGeomCurve_deegree = pair.right;
                        final Points otherGeomCurveControlPoints = deegreeTransformer
                                .getControlPoints(otherGeomCurve_deegree);

                        if (geomCurveControlPoints.size() == otherGeomCurveControlPoints.size()) {

                            /*
                             * NOTE: deegree.Point equals(..) implementation really just compares the coordinates. So no
                             * specific overhead in doing so.
                             */

                            boolean pointsMatch = true;
                            for (int i = 0; i < geomCurveControlPoints.size(); i++) {
                                if (!geomCurveControlPoints.get(i).equals(otherGeomCurveControlPoints.get(i))) {
                                    pointsMatch = false;
                                    break;
                                }
                            }

                            if (!pointsMatch) {

                                // try with reversed order of control points from curve of other geometry
                                pointsMatch = true;
                                final int otherGeomCurveControlPointsSize = otherGeomCurveControlPoints.size();
                                for (int i = 0; i < geomCurveControlPoints.size(); i++) {
                                    if (!geomCurveControlPoints.get(i).equals(
                                            otherGeomCurveControlPoints.get(otherGeomCurveControlPointsSize - i - 1))) {
                                        pointsMatch = false;
                                        break;
                                    }
                                }
                            }

                            if (pointsMatch) {
                                fullMatchFound = true;
                                break;
                            }
                        }
                    }
                }

                if (!fullMatchFound) {
                    return geomCurve_jts;
                }
            }

            // No unmatched curve found.
            return null;

        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Checks two geometries for interior intersection of curve components. If both geometries are point based, the result
     * will be <code>null</code> (since then there are no curves to check). Components of the first geometry are compared
     * with the components of the second geometry (using a spatial index to prevent unnecessary checks): If two components
     * are not equal (a situation that is allowed) then they are checked for an interior intersection, meaning that the
     * interiors of the two components intersect (T********) or - only when curves are compared - that the boundary of one
     * component intersects the interior of the other component (*T******* or ***T*****). If such a situation is detected,
     * the intersection of the two components will be returned and testing will stop (meaning that the result will only
     * provide information for one invalid intersection, not all intersections).
     *
     * @param geomNode1
     *            the node that represents the first geometry
     * @param geomNode2
     *            the node that represents the second geometry
     * @return The intersection of two components from the two geometries, where an invalid intersection was detected, or
     *         <code>null</code> if no such case exists.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry determineInteriorIntersectionOfCurveComponents(ANode geomNode1,
            ANode geomNode2) throws QueryException {

        try {

            // Determine if the two geometries intersect at all. Only if they do, a more
            // detailed computation is necessary.

            Geometry g1 = null;
            Geometry g2 = null;

            // try to get JTS geometry for first geometry node from cache
            com.vividsolutions.jts.geom.Geometry jtsg1 = null;
            if (geomNode1 instanceof DBNode) {
                jtsg1 = geometryCache.getGeometry(this.dbNodeRefFactory.createDBNodeEntry((DBNode) geomNode1));
            }
            if (jtsg1 == null) {
                g1 = deegreeTransformer.parseGeometry(geomNode1);
                jtsg1 = jtsTransformer.toJTSGeometry(g1);
            }

            // now the same for the second geometry node
            com.vividsolutions.jts.geom.Geometry jtsg2 = null;
            if (geomNode2 instanceof DBNode) {
                jtsg2 = geometryCache.getGeometry(this.dbNodeRefFactory.createDBNodeEntry((DBNode) geomNode2));
            }
            if (jtsg2 == null) {
                g2 = deegreeTransformer.parseGeometry(geomNode2);
                jtsg2 = jtsTransformer.toJTSGeometry(g2);
            }

            // If both geometries are points or multi-points, there cannot be a relevant
            // intersection.
            boolean g1IsPointGeom = jtsg1 instanceof com.vividsolutions.jts.geom.Point
                    || jtsg1 instanceof com.vividsolutions.jts.geom.MultiPoint;
            boolean g2IsPointGeom = jtsg2 instanceof com.vividsolutions.jts.geom.Point
                    || jtsg2 instanceof com.vividsolutions.jts.geom.MultiPoint;
            if (g1IsPointGeom && g2IsPointGeom) {
                return null;
            }

            // Check if the two geometries intersect at all. If not, we are done. Otherwise,
            // we need to check in more detail.
            if (!jtsg1.intersects(jtsg2)) {
                return null;
            }

            // Determine JTS geometries for relevant geometry components
            Collection<com.vividsolutions.jts.geom.Geometry> g1Components;
            if (jtsg1 instanceof com.vividsolutions.jts.geom.Point) {
                g1Components = Collections.singleton(jtsg1);
            } else if (jtsg1 instanceof com.vividsolutions.jts.geom.MultiPoint) {
                com.vividsolutions.jts.geom.MultiPoint mp = (com.vividsolutions.jts.geom.MultiPoint) jtsg1;
                g1Components = Arrays.asList(flattenAllGeometryCollections(mp));
            } else {
                if (g1 == null) {
                    g1 = deegreeTransformer.parseGeometry(geomNode1);
                }
                final Collection<Curve> curves = deegreeTransformer.getCurveComponents(g1);
                g1Components = new ArrayList<>(curves.size());
                for (final Curve c : curves) {
                    g1Components.add(jtsTransformer.toJTSGeometry(c));
                }
            }

            Collection<com.vividsolutions.jts.geom.Geometry> g2Components;
            if (jtsg2 instanceof com.vividsolutions.jts.geom.Point) {
                g2Components = Collections.singleton(jtsg2);
            } else if (jtsg2 instanceof com.vividsolutions.jts.geom.MultiPoint) {
                com.vividsolutions.jts.geom.MultiPoint mp = (com.vividsolutions.jts.geom.MultiPoint) jtsg2;
                g2Components = Arrays.asList(flattenAllGeometryCollections(mp));
            } else {
                if (g2 == null) {
                    g2 = deegreeTransformer.parseGeometry(geomNode2);
                }
                final Collection<Curve> curves = deegreeTransformer.getCurveComponents(g2);
                g2Components = new ArrayList<>(curves.size());
                for (final Curve c : curves) {
                    g2Components.add(jtsTransformer.toJTSGeometry(c));
                }
            }

            // Switch order of geometry arrays, if the second geometry is point based. We
            // want to create a
            // spatial index only for curve components.
            if (g2IsPointGeom) {
                final Collection<com.vividsolutions.jts.geom.Geometry> tmp = g1Components;
                g1Components = g2Components;
                g2Components = tmp;
            }

            // Create spatial index for curve components.
            final STRtree g2ComponentIndex = new STRtree();
            for (com.vividsolutions.jts.geom.Geometry g2CompGeom : g2Components) {
                g2ComponentIndex.insert(g2CompGeom.getEnvelopeInternal(), g2CompGeom);
            }

            // Now check for invalid interior intersections of components from the two
            // geometries.
            for (com.vividsolutions.jts.geom.Geometry g1CompGeom : g1Components) {

                // get g2 components from spatial index to compare
                @SuppressWarnings("rawtypes")
                final List g2Results = g2ComponentIndex.query(g1CompGeom.getEnvelopeInternal());

                for (Object o : g2Results) {
                    final com.vividsolutions.jts.geom.Geometry g2CompGeom = (com.vividsolutions.jts.geom.Geometry) o;
                    if (g1CompGeom.intersects(g2CompGeom)) {
                        // It is allowed that two curves are spatially equal. So only check for an
                        // interior intersection
                        // in case that the two geometry components are not spatially equal.
                        // The intersection matrix must be computed in any case (to determine that the
                        // two components
                        // are not equal). So we compute it once, and re-use it for tests on interior
                        // intersection.
                        IntersectionMatrix im = g1CompGeom.relate(g2CompGeom);
                        if (!im.isEquals(g1CompGeom.getDimension(), g2CompGeom.getDimension())
                                && (im.matches("T********") || (!(g1IsPointGeom || g2IsPointGeom)
                                        && (im.matches("*T*******") || im.matches("***T*****"))))) {
                            // invalid intersection detected
                            return g1CompGeom.intersection(g2CompGeom);
                        }
                    }
                }
            }

            // No invalid intersection found.
            return null;

        } catch (Exception e) {
            throw new GmlGeoXException(e);
        }
    }

    /**
     * Retrieve the geometry represented by a given node as a JTS geometry. First try the cache and if it is not in the
     * cache construct it from the XML.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geomNode
     *            the node that represents the geometry
     * @return the geometry of the node; can be an empty geometry if the node does not represent a geometry
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    @NotNull
    public com.vividsolutions.jts.geom.Geometry getOrCacheGeometry(final ANode geomNode) throws QueryException {
        if (debug && ++count2 % 5000 == 0) {
            logMemUsage("GmlGeoX#getGeometry.start " + count2);
        }
        com.vividsolutions.jts.geom.Geometry geom;
        if (geomNode instanceof DBNode) {
            final DBNodeRef geomNodeRef = dbNodeRefFactory.createDBNodeEntry((DBNode) geomNode);
            geom = geometryCache.getGeometry(geomNodeRef);
            if (geom == null) {
                geom = jtsTransformer.singleObjectToJTSGeometry(geomNode);
                geometryCache.cacheGeometry(geomNodeRef, geom);
                if (debug && geometryCache.getMissCount() % 10000 == 0) {
                    logger.debug("Cache misses: " + geometryCache.getMissCount() + " of " + geometryCache.getCount());
                }
            }
        } else {
            geom = jtsTransformer.singleObjectToJTSGeometry(geomNode);
        }
        if (debug && count2 % 5000 == 0) {
            logMemUsage("GmlGeoX#getGeometry.end " + count2);
        }

        return geom;
    }

    /**
     * Retrieve the the control points of the geometry represented by the given geometry node. Does not interpolate any
     * non-linear geometry components, and thus the result only contains points that are defined in the geometry node.
     * <p>
     * The behavior for searching control points can be set using parameter controlPointSearchBehavior. By default, all
     * control points defined by the geometry node are returned. If controlPointSearchBehavior contains the string (ignoring
     * case) 'IGNORE_ARC_MID_POINT', then the middle point of an arc (in Arc and ArcString) is ignored.
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geomNode
     *            the node that represents the geometry
     * @param controlPointSearchBehavior
     *            code(s) to influence the set of control points (multiple values separated by commas):
     *            'IGNORE_ARC_MID_POINT' - to ignore the middle point of an arc (in Arc and ArcString)
     * @return a list of unique control points of the geometry node, as JTS Point objects; can be empty if the node does not
     *         represent a geometry but not <code>null</code>
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public List<com.vividsolutions.jts.geom.Point> getControlPoints(final ANode geomNode,
            final String controlPointSearchBehavior) throws QueryException {

        EnumSet<ControlPointSearchBehavior> searchBehavior = EnumSet.noneOf(ControlPointSearchBehavior.class);

        if (StringUtils.isNotBlank(controlPointSearchBehavior)) {
            for (String sp : StringUtils.split(controlPointSearchBehavior, ", ")) {
                Optional<ControlPointSearchBehavior> osp = ControlPointSearchBehavior.fromString(sp);
                if (osp.isPresent()) {
                    searchBehavior.add(osp.get());
                } else {
                    throw new QueryException("Search behavior '" + sp
                            + "' is not supported. Only use one or more of the following value(s): "
                            + Arrays.stream(ControlPointSearchBehavior.values()).map(cpsb -> cpsb.getName())
                                    .collect(Collectors.joining(", ")));
                }
            }
        }

        Points points = deegreeTransformer.getControlPoints(geomNode, searchBehavior);

        final List<com.vividsolutions.jts.geom.Point> jtsPointsList = new ArrayList<>(points.size());

        for (Point point : points) {
            com.vividsolutions.jts.geom.Point geom = (com.vividsolutions.jts.geom.Point) jtsTransformer
                    .toJTSGeometry(point);
            jtsPointsList.add(geom);
        }

        return jtsPointsList;
    }

    /**
     * Identify points P in a 1D, 2D or 3D geometry that are connected to their neighbouring points N via linear curve
     * segments, and compute the angle of these lines as well as the distance of a point P to the imaginary line between its
     * two neighbouring points.
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param geomNode
     *            the node that represents the geometry
     * @return A DOM element with identified points; the element can be empty (if the geometry does not contain any relevant
     *         point). The element has the following (exemplary) structure:
     *
     *         <pre>
     * {@code
     * <geoxr:Result xmlns:geoxr="https://modules.etf-validator.net/gmlgeox/result">
     *   <geoxr:relevantPoint>
     *     <geoxr:Point>
     *       <!-- The well-known-text representation of the point. -->
     *       <geoxr:wkt>..</geoxr:wkt>
     *       <!-- The angle between the linear curve segments at this point. -->
     *       <geoxr:angleInDegree>..</geoxr:angleInDegree>
     *       <!-- The distance between the point and the imaginary line between its two neighbouring points. -->
     *       <geoxr:distance>..</geoxr:distance>
     *     </geoxr:Point>
     *   </geoxr:relevantPoint>
     *   ..
     * </geoxr:Result>
     * }
     * </pre>
     *
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public FElem determineDetailsOfPointsBetweenLinearCurveSegments(final ANode geomNode) throws QueryException {

        if (geomNode == null) {
            throw new GmlGeoXException("geomNode must not be null");
        }

        ArrayList<ArrayList<Point>> pointLists = deegreeTransformer.getSetsOfContinuousLinearCurveSegments(geomNode);

        final QNm Result_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "Result", GMLGEOX_RESULT_NS);
        final QNm RelevantPoint_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "relevantPoint", GMLGEOX_RESULT_NS);
        final QNm Point_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "Point", GMLGEOX_RESULT_NS);
        final QNm Wkt_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "wkt", GMLGEOX_RESULT_NS);
        final QNm AngleInDegree_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "angleInDegree", GMLGEOX_RESULT_NS);
        final QNm Distance_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "distance", GMLGEOX_RESULT_NS);

        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(Integer.MAX_VALUE);

        final FElem root = new FElem(Result_QNM);

        // now perform computations of point details
        for (ArrayList<Point> pointList : pointLists) {

            // ignore the first and the last point in the list
            for (int i = 1; i < pointList.size() - 1; i++) {

                final Point pDeg1 = pointList.get(i - 1);
                final Point pDeg2 = pointList.get(i);
                final Point pDeg3 = pointList.get(i + 1);

                com.vividsolutions.jts.geom.Point p1 = jtsTransformer.toJTSPoint(pDeg1);
                com.vividsolutions.jts.geom.Point p2 = jtsTransformer.toJTSPoint(pDeg2);
                com.vividsolutions.jts.geom.Point p3 = jtsTransformer.toJTSPoint(pDeg3);

                final Coordinate coord1 = p1.getCoordinate();
                final Coordinate coord2 = p2.getCoordinate();
                final Coordinate coord3 = p3.getCoordinate();

                final double angleVector1to2 = Angle.angle(coord1, coord2);
                final double angleVector2to3 = Angle.angle(coord2, coord3);

                final double diff_rad = Angle.diff(angleVector1to2, angleVector2to3);

                final double diff_deg = Angle.toDegrees(diff_rad);

                com.vividsolutions.jts.geom.LineString lineP1toP3 = jtsFactory
                        .createLineString(new Coordinate[]{coord1, coord3});

                double distanceP2ToLine = p2.distance(lineP1toP3);

                final FElem pointElem = new FElem(Point_QNM);

                final FElem distanceElem = new FElem(Distance_QNM);
                String formattedDistance = df.format(distanceP2ToLine);
                distanceElem.add(formattedDistance);

                final FElem angleElem = new FElem(AngleInDegree_QNM);
                String formattedAngle = df.format(diff_deg);
                angleElem.add(formattedAngle);

                final FElem wktElem = new FElem(Wkt_QNM);
                wktElem.add(p2.toText());

                pointElem.add(wktElem);
                pointElem.add(angleElem);
                pointElem.add(distanceElem);

                final FElem relevantPointElem = new FElem(RelevantPoint_QNM);
                relevantPointElem.add(pointElem);

                root.add(relevantPointElem);
            }
        }

        return root;
    }

    private com.vividsolutions.jts.geom.Geometry getOrCacheGeometry(final ANode geomNode, final DBNodeRef geomNodeRef)
            throws QueryException {
        if (debug && ++count2 % 5000 == 0) {
            logMemUsage("GmlGeoX#getGeometry.start " + count2);
        }
        com.vividsolutions.jts.geom.Geometry geom = geometryCache.getGeometry(geomNodeRef);
        if (geom == null) {
            geom = jtsTransformer.singleObjectToJTSGeometry(geomNode);
            if (geom != null) {
                // geom.setUserData(geomNodeRef);
                geometryCache.cacheGeometry(geomNodeRef, geom);
            }
            if (debug && geometryCache.getMissCount() % 10000 == 0) {
                logger.debug("Cache misses: " + geometryCache.getMissCount() + " of " + geometryCache.getCount());
            }
        }
        if (geom == null) {
            geom = jtsTransformer.emptyJTSGeometry();
        }
        if (debug && count2 % 5000 == 0) {
            logMemUsage("GmlGeoX#getGeometry.end " + count2);
        }

        return geom;
    }

    /**
     * Prepares spatial indexing of a feature geometry, for the default spatial index.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param node
     *            represents the node of the feature to be indexed
     * @param geometry
     *            represents the GML geometry to index
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void prepareSpatialIndex(final ANode node, final ANode geometry) throws QueryException {
        prepareSpatialIndex(DEFAULT_SPATIAL_INDEX, node, geometry);
    }

    /**
     * Prepares spatial indexing of a feature geometry, for the named spatial index.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @param node
     *            represents the node of the feature to be indexed
     * @param geometry
     *            represents the GML geometry to index
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void prepareSpatialIndex(final String indexName, final ANode node, final ANode geometry)
            throws QueryException {
        if (node instanceof DBNode && geometry instanceof DBNode) {
            final com.vividsolutions.jts.geom.Geometry _geom = getOrCacheGeometry(geometry);
            final Envelope env = _geom.getEnvelopeInternal();
            if (!env.isNull()) {
                // cache the index entry
                final DBNodeRef geometryNodeEntry;
                if (_geom.getUserData() != null) {
                    geometryNodeEntry = (DBNodeRef) _geom.getUserData();
                } else {
                    geometryNodeEntry = this.dbNodeRefFactory.createDBNodeEntry((DBNode) node);
                }
                final com.github.davidmoten.rtree.geometry.Geometry treeGeom;
                if (env.getHeight() == 0.0 && env.getWidth() == 0.0) {
                    treeGeom = Geometries.point(env.getMinX(), env.getMinY());
                } else {
                    treeGeom = Geometries.rectangle(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
                }
                geometryCache.prepareSpatialIndex(indexName, geometryNodeEntry, treeGeom);
                // also cache the envelope
                geometryCache.addEnvelope(geometryNodeEntry, env);
            }

            if (debug && geometryCache.indexSize(indexName) % 5000 == 0) {
                String debugIndexName = indexName != null ? indexName : "default";
                logMemUsage("GmlGeoX#index progress (for index '" + debugIndexName + "'): "
                        + geometryCache.indexSize(indexName));
            }
        }
    }

    /**
     * Prepares spatial indexing of the control points of $featuresToSearchIn, for the named spatial index "PointIndex".
     * Parses the control points of $featuresToSearchBy. The behavior for computing control points can be influenced by the
     * parameter controlPointSearchBehavior. For each control point, search the index "PointIndex" for a nearest point. If a
     * point within a distance which is strictly smaller than 'maxDistance' is found, detailed information will be returned
     * as a result DOM element. Points which are strictly smaller than 'minDistance' will not be detected as nearest points.
     * Identical points will also not be detected as nearest points.
     *
     * <p>
     * Requires the default spatial index to be initialized in order to retrieve the minimal bounding region.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param featuresToSearchBy
     *            as a sequence of GML feature nodes.
     * @param featuresToSearchByGeom
     *            $featuresToSearchByGeom as a sequence of the GML geometry nodes of $featuresToSearchBy.
     * @param featuresToSearchIn
     *            as a sequence of the GML geometry nodes of $featuresToSearchIn.
     * @param featuresToSearchInGeom
     *            as a sequence of the GML geometry nodes of $featuresToSearchIn.
     * @param controlPointSearchBehavior
     *            code(s) to influence the set of control points (multiple values separated by commas):
     *            'IGNORE_ARC_MID_POINT' - to ignore the middle point of an arc (in Arc and ArcString)
     * @param minDistance
     *            as the minimum distance to search nearest points around the points of $featuresToSearchBy.
     * @param maxDistance
     *            as the maximum distance to search nearest points around the points of $featuresToSearchBy.
     * @param limitErrors
     *            as the maximum number of features with errors to report.
     * @param tileLength
     *            as the coordinate length of each square tile, by which the input will be processed.
     * @param tileOverlap
     *            as the coordinate length, by which the tiles will overlap. In order to prevent errors at boundaries.
     * @return A DOM element with detailed information of the computed results; the element can be empty (if the geometry
     *         does not contain any relevant points). The element has the following (exemplary) structure:
     *
     *         {@code
     * <geoxr:geoxResults xmlns:geoxr=
    "https://modules.etf-validator.net/gmlgeox/result">
     *   <geoxr:result>
     *     <!-- The toString() representation of a DBNode which has a nearest point. -->
     *     <geoxr:featureWithNearest>db:open-pre("GMLGEOX-JUNIT-TEST-DB-000", 11)</geoxr:featureWithNearest>
     *     <!-- The point of featureWithNearest which has a nearest point. -->
     *     <geoxr:currentPointWithNearest>408331.407 5473380.787</geoxr:currentPointWithNearest>
     *     <!-- The toString() representation of a DBNode which is nearest to featureWithNearest. -->
     *     <geoxr:nearestFeature>db:open-pre("GMLGEOX-JUNIT-TEST-DB-000", 60)</geoxr:nearestFeature>
     *     <!-- The point of nearestFeature which is nearest to currentPointWithNearest. -->
     *     <geoxr:nearestPoint>408331.407 5473380.789</geoxr:nearestPoint>
     *     <!-- The distance between the currentPointWithNearest and the nearestPoint. -->
     *     <geoxr:distance>0.0020000003278255463</geoxr:distance>
     *   </geoxr:result>
     *   <geoxr:result>
     *   ..
     *   <geoxr:result>
     *   ..
     * </geoxr:geoxResults>
     * }
     *
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public FElem detectNearestPoints(final Value featuresToSearchBy, final Value featuresToSearchByGeom,
            final Value featuresToSearchIn, final Value featuresToSearchInGeom, final String controlPointSearchBehavior,
            final double minDistance, final double maxDistance, final int limitErrors, double tileLength,
            double tileOverlap) throws QueryException {
        final QNm GeoxResults_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "geoxResults", GMLGEOX_RESULT_NS);
        final FElem results_root = new FElem(GeoxResults_QNM);

        if (featuresToSearchBy.size() != featuresToSearchByGeom.size()
                || featuresToSearchIn.size() != featuresToSearchInGeom.size()) {
            throw new IllegalArgumentException(
                    "The number of features and the number of geometries to process must be equal.");
        }
        if (featuresToSearchBy.isEmpty() || featuresToSearchIn.isEmpty()) {
            return results_root;
        }
        Iterator<Item> featureBy_iterator = featuresToSearchBy.iterator();
        Iterator<Item> geomBy_iterator = featuresToSearchByGeom.iterator();
        Iterator<Item> featureIn_iterator = featuresToSearchIn.iterator();
        Iterator<Item> geomIn_iterator = featuresToSearchInGeom.iterator();

        final HashMap<DBNodeRef, DBNodeRef> geomByFeature = new HashMap<>();
        while (featureBy_iterator.hasNext() && geomBy_iterator.hasNext()) {
            final DBNode currentFeature = (DBNode) featureBy_iterator.next();
            final DBNode currentGeom = (DBNode) geomBy_iterator.next();
            prepareSpatialIndex("featuresToSearchBy", currentFeature, currentGeom);

            final DBNodeRef featureNodeEntry = this.dbNodeRefFactory.createDBNodeEntry(currentFeature);
            final DBNodeRef geomNodeEntry = this.dbNodeRefFactory.createDBNodeEntry(currentGeom);
            geomByFeature.put(featureNodeEntry, geomNodeEntry);
        }
        while (featureIn_iterator.hasNext() && geomIn_iterator.hasNext()) {
            final DBNode currentFeature = (DBNode) featureIn_iterator.next();
            final DBNode currentGeom = (DBNode) geomIn_iterator.next();
            prepareSpatialIndex("featuresToSearchIn", currentFeature, currentGeom);

            final DBNodeRef featureNodeEntry = this.dbNodeRefFactory.createDBNodeEntry(currentFeature);
            final DBNodeRef geomNodeEntry = this.dbNodeRefFactory.createDBNodeEntry(currentGeom);
            geomByFeature.put(featureNodeEntry, geomNodeEntry);
        }
        buildSpatialIndex("featuresToSearchBy");
        buildSpatialIndex("featuresToSearchIn");

        Rectangle envelope_featuresToSearchIn = geometryCache.getIndexMbr("featuresToSearchIn");
        Collection<Rectangle> tiles = getTiles(envelope_featuresToSearchIn, tileLength, tileOverlap);
        final List<ANode> featuresWithNearest = new ArrayList<>();

        outerloop: for (Rectangle tile : tiles) {
            @NotNull
            List<DBNode> featuresToSearchBy_inTile = geometryCache.search("featuresToSearchBy", tile, dbNodeRefLookup);
            @NotNull
            List<DBNode> featuresToSearchIn_inTile = geometryCache.search("featuresToSearchIn", tile, dbNodeRefLookup);

            if (featuresToSearchBy_inTile.isEmpty() || featuresToSearchIn_inTile.isEmpty()) {
                continue;
            }

            for (final DBNode feature : featuresToSearchIn_inTile) {
                final DBNodeRef geomRef = geomByFeature.get(this.dbNodeRefFactory.createDBNodeEntry(feature));
                final DBNode geom = this.dbNodeRefLookup.resolve(geomRef);
                prepareSpatialPointIndex(POINT_INDEX_NAME, feature, geom, controlPointSearchBehavior);
            }
            buildSpatialIndex(POINT_INDEX_NAME);

            for (final DBNode feature : featuresToSearchBy_inTile) {
                final DBNodeRef geomRef = geomByFeature.get(this.dbNodeRefFactory.createDBNodeEntry(feature));
                final DBNode geom = this.dbNodeRefLookup.resolve(geomRef);

                if (!featuresWithNearest.contains(feature)) {
                    final FElem result = detectNearestPointsByFeature(feature, geom, controlPointSearchBehavior,
                            minDistance, maxDistance);
                    if (result.hasChildren()) {
                        results_root.add(result);
                        featuresWithNearest.add(feature);
                    }
                    final long resultSize = results_root.childIter().size();
                    if (resultSize >= limitErrors)
                        break outerloop;
                }
            }
            geometryCache.removeIndex(POINT_INDEX_NAME);
            System.gc();
        }

        geometryCache.removeIndex(POINT_INDEX_NAME);
        geometryCache.removeIndex("featuresToSearchBy");
        geometryCache.removeIndex("featuresToSearchIn");
        return results_root;
    }

    private Collection<Rectangle> getTiles(final Rectangle allFeaturesEnvelope, double tileLength, double tileOverlap) {
        final List<Rectangle> tiles = new ArrayList<>();

        for (double y = allFeaturesEnvelope.y1(); y <= allFeaturesEnvelope.y2(); y = y + tileLength) {
            for (double x = allFeaturesEnvelope.x1(); x <= allFeaturesEnvelope.x2(); x = x + tileLength) {
                final double tileX1 = x;
                final double tileY1 = y;
                final double tileX2 = x + tileLength;
                final double tileY2 = y + tileLength;
                final Rectangle tile = Geometries.rectangle(tileX1 - tileOverlap, tileY1 - tileOverlap,
                        tileX2 + tileOverlap, tileY2 + tileOverlap);
                tiles.add(tile);
            }
        }
        return tiles;
    }

    /**
     * Prepares spatial indexing of the control points of a feature geometry, for a spatial index with given name. The
     * behavior for retrieving control points can be influenced by the parameter controlPointSearchBehavior.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param indexName
     *            Identifies the index.
     * @param node
     *            represents the node of the feature to be indexed
     * @param geometry
     *            represents the GML geometry to index
     * @param controlPointSearchBehavior
     *            codes to influence to resulting set of control points (multiple values separated by commas):
     *            'IGNORE_ARC_MID_POINT' - to ignore the middle point of an arc (in Arc and ArcString)
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void prepareSpatialPointIndex(final @NotNull String indexName, final ANode node, final ANode geometry,
            final String controlPointSearchBehavior) throws QueryException {

        if (node instanceof DBNode && geometry instanceof DBNode) {

            final List<com.vividsolutions.jts.geom.Point> points = getControlPoints(geometry,
                    controlPointSearchBehavior);

            if (!points.isEmpty()) {
                final DBNodeRef geometryNodeEntry = this.dbNodeRefFactory.createDBNodeEntry((DBNode) node);

                for (com.vividsolutions.jts.geom.Point p : points) {
                    // cache the index entry
                    final com.github.davidmoten.rtree.geometry.Point treeGeom;
                    treeGeom = Geometries.point(p.getX(), p.getY());
                    geometryCache.prepareSpatialIndex(indexName, geometryNodeEntry, treeGeom);
                }
            }
        }
    }

    /**
     * Parses the control points of featuresToSearchBy and returns the result of a nearest point search. The behavior for
     * retrieving control points can be influenced by the parameter controlPointSearchBehavior. For each relevant control
     * point, search the index "PointIndex" for a nearest point. If a point within a distance which is strictly smaller than
     * 'maxDistance' is found, then the corresponding feature which is nearest to the current point will be returned. Points
     * which are strictly smaller than 'minDistance' will not be detected as nearest points. Identical points will not be
     * detected as nearest points.
     *
     * @param featureToSearchBy
     *            represents the node of the feature to search the index
     * @param geometry
     *            represents the GML geometry of the feature to search the index by
     * @param controlPointSearchBehavior
     *            codes to influence the set of control points (multiple values separated by commas): 'IGNORE_ARC_MID_POINT'
     *            - to ignore the middle point of an arc (in Arc and ArcString)
     * @param minDistance
     *            as the minimum distance to search nearest points around the points of featuresToSearchBy.
     * @param maxDistance
     *            as the maximum distance to search nearest points around the points of featuresToSearchBy.
     * @return A DOM element with detailed information of the computed results; the element can be empty (if the geometry
     *         does not contain any relevant points).
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    private FElem detectNearestPointsByFeature(final ANode featureToSearchBy, final ANode geometry,
            final String controlPointSearchBehavior, final double minDistance, final double maxDistance)
            throws QueryException {

        final QNm result_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "result", GMLGEOX_RESULT_NS);
        final FElem result = new FElem(result_QNM);

        EnumSet<ControlPointSearchBehavior> searchBehavior = EnumSet.noneOf(ControlPointSearchBehavior.class);
        if (StringUtils.isNotBlank(controlPointSearchBehavior)) {
            for (String sp : StringUtils.split(controlPointSearchBehavior, ", ")) {
                Optional<ControlPointSearchBehavior> osp = ControlPointSearchBehavior.fromString(sp);
                if (osp.isPresent()) {
                    searchBehavior.add(osp.get());
                } else {
                    throw new QueryException("Search behavior '" + sp
                            + "' is not supported. Only use one or more of the following value(s): "
                            + Arrays.stream(ControlPointSearchBehavior.values()).map(cpsb -> cpsb.getName())
                                    .collect(Collectors.joining(", ")));
                }
            }
        }

        final Points points = deegreeTransformer.getControlPoints(geometry, searchBehavior);
        final DBNodeRef currentFeature = dbNodeRefFactory.createDBNodeEntry((DBNode) featureToSearchBy);

        outerloop: for (Point currentPointDeegree : points) {
            final double x = currentPointDeegree.get0();
            final double y = currentPointDeegree.get1();
            final com.github.davidmoten.rtree.geometry.Point currentPointRTree = Geometries.point(x, y);

            List<com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>> nearestEntries = geometryCache
                    .nearest(POINT_INDEX_NAME, currentPointRTree, maxDistance, Integer.MAX_VALUE);
            if (minDistance > 0.0) {
                List<com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>> nearestEntries_lowerBound = geometryCache
                        .nearest(POINT_INDEX_NAME, currentPointRTree, minDistance, Integer.MAX_VALUE);
                nearestEntries.removeAll(nearestEntries_lowerBound);
            }

            for (com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry> entry : nearestEntries) {
                if (!entry.geometry().equals(currentPointRTree)) {
                    // Only report non-identical nearest points.
                    final QNm featureWithNearest_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "featureWithNearest",
                            GMLGEOX_RESULT_NS);
                    final QNm currentPointWithNearest_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "currentPointWithNearest",
                            GMLGEOX_RESULT_NS);
                    final QNm nearestFeature_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "nearestFeature", GMLGEOX_RESULT_NS);
                    final QNm nearestPoint_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "nearestPoint", GMLGEOX_RESULT_NS);
                    final QNm distance_QNM = new QNm(GMLGEOX_RESULT_PREFIX, "distance", GMLGEOX_RESULT_NS);

                    final FElem featureWithNearest = new FElem(featureWithNearest_QNM);
                    final FElem currentPointWithNearest = new FElem(currentPointWithNearest_QNM);
                    final FElem nearestFeature = new FElem(nearestFeature_QNM);
                    final FElem nearestPoint = new FElem(nearestPoint_QNM);
                    final FElem distance = new FElem(distance_QNM);

                    featureWithNearest.add(this.dbNodeRefLookup.resolve(currentFeature).toString());
                    currentPointWithNearest.add(toPosListString(currentPointRTree));
                    nearestFeature.add(this.dbNodeRefLookup.resolve(entry.value()).toString());
                    nearestPoint.add(toPosListString((com.github.davidmoten.rtree.geometry.Point) entry.geometry()));
                    distance.add(String.valueOf(
                            currentPointRTree.distance((com.github.davidmoten.rtree.geometry.Point) entry.geometry())));

                    result.add(featureWithNearest);
                    result.add(currentPointWithNearest);
                    result.add(nearestFeature);
                    result.add(nearestPoint);
                    result.add(distance);
                    break outerloop; // This function shall only return the nearest Object.
                }
            }
        }
        return result;
    }

    /**
     * Prepares spatial indexing of a feature geometry, for the default and a named spatial index.
     *
     * <p>
     * See {@link JtsTransformer#toJTSGeometry(Geometry)} for a list of supported and unsupported geometry types.
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @param node
     *            represents the node of the feature to be indexed
     * @param geometry
     *            represents the GML geometry to index
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void prepareDefaultAndSpecificSpatialIndex(final String indexName, final ANode node, final ANode geometry)
            throws QueryException {
        if (node instanceof DBNode && geometry instanceof DBNode) {
            final com.vividsolutions.jts.geom.Geometry _geom = getOrCacheGeometry(geometry);
            final Envelope env = _geom.getEnvelopeInternal();
            if (!env.isNull()) {
                // cache the index entry
                final DBNodeRef geometryNodeEntry;
                if (_geom.getUserData() != null) {
                    geometryNodeEntry = (DBNodeRef) _geom.getUserData();
                } else {
                    geometryNodeEntry = this.dbNodeRefFactory.createDBNodeEntry((DBNode) node);
                }
                final com.github.davidmoten.rtree.geometry.Geometry treeGeom;
                if (env.getHeight() == 0.0 && env.getWidth() == 0.0) {
                    treeGeom = Geometries.point(env.getMinX(), env.getMinY());
                } else {
                    treeGeom = Geometries.rectangle(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
                }
                geometryCache.prepareSpatialIndex(indexName, geometryNodeEntry, treeGeom);
                geometryCache.prepareSpatialIndex(DEFAULT_SPATIAL_INDEX, geometryNodeEntry, treeGeom);
                // also cache the envelope
                geometryCache.addEnvelope(geometryNodeEntry, env);
            }
        }
    }

    /**
     * Create the default spatial index using bulk loading.
     *
     * <p>
     * Uses the index entries that have been prepared using method(s) prepareSpatialIndex(...).
     *
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    public void buildSpatialIndex() throws QueryException {
        buildSpatialIndex(DEFAULT_SPATIAL_INDEX);
    }

    /**
     * Create the named spatial index using bulk loading.
     *
     * <p>
     * Uses the index entries that have been prepared using method(s) prepareSpatialIndex(...).
     *
     * @param indexName
     *            Identifies the index. <code>null</code> or the empty string identifies the default index.
     * @throws QueryException
     *             If the index has already been built.
     */
    @Requires(Permission.NONE)
    public void buildSpatialIndex(final String indexName) throws QueryException {
        geometryCache.buildIndexUsingBulkLoading(indexName);
    }

    /**
     * Retrieve the first two coordinates of a given geometry.
     *
     * @param geom
     *            the geometry
     * @return an empty array if the geometry is null or empty, otherwise an array with the x and y from the first
     *         coordinate of the geometry
     */
    @Requires(Permission.NONE)
    @Deterministic
    public String[] georefFromGeom(com.vividsolutions.jts.geom.Geometry geom) {

        if (geom == null || geom.isEmpty()) {
            return new String[]{};
        } else {
            Coordinate firstCoord = geom.getCoordinates()[0];
            return new String[]{"" + firstCoord.x, "" + firstCoord.y};
        }
    }

    /**
     * Retrieve x and y of the given coordinate, as strings without scientific notation.
     *
     * @param coord
     *            the coordinate
     * @return an array with the x and y of the given coordinate, as strings without scientific notation.
     * @throws QueryException
     *             In case an exception occurred.
     */
    @Requires(Permission.NONE)
    @Deterministic
    public String[] georefFromCoord(com.vividsolutions.jts.geom.Coordinate coord) throws QueryException {
        return new String[]{"" + coord.x, "" + coord.y};
    }

    /**
     * Computes the segment of the given (Multi)LineString that are in the interior of the given (Multi)Polygon.
     *
     * @param line
     *            a non-empty JTS LineString or MultiLineString
     * @param polygon
     *            a non-empty JTS Polygon or MultiPolygon
     * @return the segments of the line that are in the interior of the polygon, as a JTS Geometry(Collection)
     * @throws QueryException
     *             if the parameter values are not correct
     */
    @Requires(Permission.NONE)
    @Deterministic
    public com.vividsolutions.jts.geom.Geometry interiorIntersectionsLinePolygon(
            final com.vividsolutions.jts.geom.Geometry line, final com.vividsolutions.jts.geom.Geometry polygon)
            throws QueryException {

        if (line == null || line.isEmpty()) {
            throw new QueryException("First parameter must not be null or the empty geometry.");
        } else if (polygon == null || polygon.isEmpty()) {
            throw new QueryException("Second parameter must not be null or the empty geometry.");
        } else if (!(line instanceof com.vividsolutions.jts.geom.LineString
                || line instanceof com.vividsolutions.jts.geom.MultiLineString)) {
            throw new QueryException("First parameter must be a JTS LineString or MultiLineString.");
        } else if (!(polygon instanceof com.vividsolutions.jts.geom.Polygon
                || polygon instanceof com.vividsolutions.jts.geom.MultiPolygon)) {
            throw new QueryException("Second parameter must be a JTS Polygon or MultiPolygon.");
        } else if (!line.relate(polygon, "T********")) {
            // there is no interior intersection
            return null;
        } else {

            com.vividsolutions.jts.geom.Geometry lineIntersectionWithPolygon;

            try {
                lineIntersectionWithPolygon = line.intersection(polygon);
            } catch (Exception e) {
                throw new QueryException(
                        e.getClass().getName() + " while computing the intersection. Message is: " + e.getMessage());
            }

            // now we compute the difference of the intersection and the polygon boundary
            // JTS difference does not support geometry collections, though
            // so we need to handle cases of geometry collections

            com.vividsolutions.jts.geom.Geometry[] intersections = flattenAllGeometryCollections(
                    lineIntersectionWithPolygon);
            com.vividsolutions.jts.geom.Geometry[] polyBoundaryComponents = flattenAllGeometryCollections(
                    polygon.getBoundary());

            List<com.vividsolutions.jts.geom.Geometry> interiorIntersections = new ArrayList<>();

            for (com.vividsolutions.jts.geom.Geometry intersection : intersections) {

                // diff the intersection against each component of the boundary
                // if the result is not empty, it represents an interior intersection

                com.vividsolutions.jts.geom.Geometry diffResult = intersection;

                for (com.vividsolutions.jts.geom.Geometry polyBoundaryComponent : polyBoundaryComponents) {

                    try {
                        diffResult = diffResult.difference(polyBoundaryComponent);
                    } catch (Exception e) {
                        throw new QueryException(e.getClass().getName()
                                + " while computing the difference. Message is: " + e.getMessage());
                    }

                    if (diffResult.isEmpty()) {
                        break;
                    }
                }

                if (!diffResult.isEmpty()) {
                    interiorIntersections.add(diffResult);
                }
            }

            if (interiorIntersections.isEmpty()) {
                return null;
            } else {
                return unionGeom(interiorIntersections);
            }
        }
    }

    @Requires(Permission.READ)
    @Deterministic
    public String detailedVersion() {
        try {
            final URLClassLoader cl = (URLClassLoader) getClass().getClassLoader();
            final URL url = cl.findResource("META-INF/MANIFEST.MF");
            final Manifest manifest = new Manifest(url.openStream());
            final String version = manifest.getMainAttributes().getValue("Implementation-Version");
            final String buildTime = manifest.getMainAttributes().getValue("Build-Date").substring(2);
            return version + "-b" + buildTime;
        } catch (Exception E) {
            return "unknown";
        }
    }

    @Requires(Permission.ADMIN)
    public GmlGeoX getModuleInstance() {
        return this;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.geometryFactory);
        out.writeUTF(this.dbNodeRefFactory.getDbNamePrefix());
        out.writeUTF(this.geometryValidator.registeredGmlGeometries());
        out.writeObject(this.geometryCache);
        final String setStandardSRS = this.srsLookup.getStandardSRS();
        out.writeUTF(setStandardSRS != null ? setStandardSRS : "");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // Todo read from meta
        final BxNamespaceHolder bxNamespaceHolder = BxNamespaceHolder.init(queryContext);
        this.srsLookup = new SrsLookup();
        this.geometryFactory = (IIGeometryFactory) in.readObject();
        this.deegreeTransformer = new DeegreeTransformer(this.geometryFactory, bxNamespaceHolder, this.srsLookup);
        this.jtsTransformer = new JtsTransformer(this.deegreeTransformer, this.jtsFactory, this.srsLookup);
        this.dbNodeRefFactory = DBNodeRefFactory.create(in.readUTF() + "000");
        this.dbNodeRefLookup = new DBNodeRefLookup(this.queryContext, this.dbNodeRefFactory);
        this.geometryValidator = new GeometryValidator(this.srsLookup, this.jtsTransformer, this.deegreeTransformer,
                bxNamespaceHolder);
        this.geometryValidator.unregisterAllGmlGeometries();
        final String gmlGeometries = in.readUTF();
        Stream.of(gmlGeometries.split(",")).map(e -> e.trim())
                .forEach(e -> this.geometryValidator.registerGmlGeometry(e));
        this.geometryCache = (GeometryCache) in.readObject();
        this.srsLookup.setStandardSRS(in.readUTF());
    }
}
