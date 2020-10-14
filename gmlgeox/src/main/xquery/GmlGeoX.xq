(:~
 :
 : ---------------------------------------
 : GmlGeoX XQuery Function Library Facade
 : ---------------------------------------
 :
 : Copyright (C) 2018-2020 interactive instruments GmbH
 :
 : Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 : the European Commission - subsequent versions of the EUPL (the "Licence");
 : You may not use this work except in compliance with the Licence.
 : You may obtain a copy of the Licence at:
 :
 : https://joinup.ec.europa.eu/software/page/eupl
 :
 : Unless required by applicable law or agreed to in writing, software
 : distributed under the Licence is distributed on an "AS IS" basis,
 : WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 : See the Licence for the specific language governing permissions and
 : limitations under the Licence.
 :
 : @author  Johannes Echterhoff (echterhoff aT interactive-instruments doT de)
 : @author  Clemens Portele ( portele aT interactive-instruments doT de )
 : @author  Jon Herrmann ( herrmann aT interactive-instruments doT de )
 : @author  Christoph Spalek (spalek aT interactive-instruments doT de)
 :
 :)

module namespace geox = 'https://modules.etf-validator.net/gmlgeox/2';
import module namespace java = 'java:de.interactive_instruments.etf.bsxm.GmlGeoX';

(:~
 : Initializes the query module.
 :
 : @param $databaseName
 :)
declare function geox:init($databaseName as xs:string) as empty-sequence() {
    java:init($databaseName)
};

(:~
 : Loads SRS configuration files from the given directory, to be used when looking up SRS names when creating geometry objects.
 :
 : @param $databaseName
 : @param $configurationDirectoryPathName path to a directory that contains SRS configuration files
 :)
declare function geox:init($databaseName as xs:string, $configurationDirectoryPathName as xs:string) as empty-sequence() {
    java:init($databaseName, $configurationDirectoryPathName)
};

(:~
 : Retrieve the Well-Known-Text representation of a given JTS geometry.
 :
 : @param $geom a JTS geometry
 : @return the WKT representation of the given geometry, or '<null>' if the geometry is null.
 :)
declare function geox:toWKT($geom ) as xs:string {
    java:toWKT($geom)
};

(:
 : Parse the Well-Known-Text to a JTS geometry.
 :
 : @param geomWkt geometry as well-known-text
 : @return the JTS geometry parsed from the wkt
 :)
declare function geox:fromWKT($wkt as xs:string) {
    java:fromWKT($wkt)
};

(:~
 : Flattens the given geometry if it is a geometry collection. Members that are not geometry collections are added to the result. Thus, MultiPoint, -LineString, and -Polygon will also be flattened. Contained geometry collections are recursively scanned for relevant members.
 :
 : @param $geom a JTS geometry, can be a JTS GeometryCollection
 : @return a sequence of JTS geometry objects that are not collection types; can be empty
 :)
declare function geox:flattenAllGeometryCollections($geom )  {
    java:flattenAllGeometryCollections($geom)
};

(:~
 : Only dissolves GeometryCollection objects, not MultiPoint, -LineString, or -Polygon.
 :
 : @param $geom the JTS geometry to flatten
 : @return the resulting set of geometry objects (none of direct type GeometryCollection); can be empty
 :)
declare function geox:flattenGeometryCollections($geom )  {
    java:flattenGeometryCollections($geom)
};

(:~
 : Identifies points of the given line, where the segment that ends in a point and the following segment that starts with that point form a change in direction whose angular value is within a given interval.
 :
 : @param $geom a JTS LineString which shall be checked for directional changes whose value is within the given interval
 : @param $minAngle Minimum directional change to be considered, in degrees. 0<=minAngle<=maxAngle<=180
 : @param $maxAngle Maximum directional change to be considered, in degrees. 0<=minAngle<=maxAngle<=180
 : @return A sequence of JTS Point objects where the line has a directional change within the given change interval. Can be empty in case that the given $geom is null or only has one segment.
 :)
declare function geox:directionChanges($geom , $minAngle , $maxAngle )  {
    java:directionChanges($geom, $minAngle, $maxAngle)
};

(:~
 : Identifies points of the given line, where the segment that ends in a point and the following segment that starts with that point form a change in direction whose angular value is greater than the given limit.
 :
 : @param $geom A JTS LineString which shall be checked for directional changes that are greater than the given limit.
 : @param $limitAngle Angular value of directional change that defines the limit, in degrees. 0 <= limitAngle <= 180
 : @return Sequence of JTS Point objects where the line has a directional change that is greater than the given limit. Can be empty in case that the given $geom is null or only has one segment.
 :)
declare function geox:directionChangesGreaterThanLimit($geom , $limitAngle )  {
    java:directionChangesGreaterThanLimit($geom, $limitAngle)
};

(:~
 : Validates the given (GML geometry) node, using all available tests.
 :
 : See the documentation of the geox:validate(element(), string) method for a description of the supported geometry types and tests.
 :
 : @param $node the GML geometry to validate
 : @return A mask with the test results, encoded as characters - one at each position (1-based index) of the available tests. 'V' indicates that the test passed, i.e. that the geometry is valid according to that test. 'F' indicates that the test failed. 'S' indicates that the test was skipped.
 :)
declare function geox:validate($node ) as xs:string {
    java:validate($node)
};

(:~
 : Validates the given (GML geometry) node.
 :
 : By default validation is only performed for the following GML geometry elements: Point, Polygon, Surface, Curve, LinearRing, MultiPolygon, MultiGeometry, MultiSurface, MultiCurve, Ring, and LineString. The set of GML elements to validate can be modified via the following functions: geox:registerGmlGeometry(String), geox:unregisterGmlGeometry(String), and geox:unregisterAllGmlGeometries().
 :
 : The validation tasks to perform can be specified via the given mask. The mask is a simple string, where the character '1' at the position of a specific test (assuming a 1-based index) specifies that the test shall be performed. If the mask does not contain a character at the position of a specific test (because the mask is empty or the length is smaller than the position), then the test will be executed.
 :
 : The following tests are available:
 :
 : <table border="1">
 : <tr>
 : <th>Position</th>
 : <th>Test Name</th>
 : <th>Description</th>
 : </tr>
 : <tr>
 : <td>1</td>
 : <td>General Validation</td>
 : <td>This test validates the given geometry using the validation functionality of both deegree and JTS. More specifically:
 : <p>
 : <p>
 : <span style="text-decoration: underline;"><strong>deegree based validation:</strong></span>
 : </p>
 : <ul>
 : <li>primitive geometry (point, curve, ring, surface):
 : <ul>
 : <li>point: no specific validation</li>
 : <li>curve:
 : <ul>
 : <li>duplication of successive control points (only for linear curve segments)</li>
 : <li>segment discontinuity</li>
 : <li>self intersection (based on JTS isSimple())</li>
 : </ul>
 : </li>
 : <li>ring:
 : <ul>
 : <li>Same as curve.</li>
 : <li>In addition, test if ring is closed</li>
 : </ul>
 : </li>
 : <li>surface:
 : <ul>
 : <li>only checks PolygonPatch, individually:</li>
 : <li>applies ring validation to interior and exterior rings</li>
 : <li>checks ring orientation (ignored for GML 3.1):
 : <ul>
 : <li>must be counter-clockwise for exterior ring</li>
 : <li>must be clockwise for interior ring</li>
 : </ul>
 : </li>
 : <li>interior ring intersects exterior</li>
 : <li>interior ring outside of exterior ring</li>
 : <li>interior rings intersection</li>
 : <li>interior rings are nested</li>
 : </ul>
 : </li>
 : </ul>
 : </li>
 : <li>composite geometry: member geometries are validated individually</li>
 : <li>multi geometry: member geometries are validated individually</li>
 : </ul>
 : <p>
 : NOTE: There's some overlap with JTS validation. The following invalid situations are reported by the JTS validation:
 : </p>
 : <ul>
 : <li>curve self intersection</li>
 : <li>interior ring intersects exterior</li>
 : <li>interior ring outside of exterior ring</li>
 : <li>interior rings intersection</li>
 : <li>interior rings are nested</li>
 : <li>interior rings touch</li>
 : <li>interior ring touches exterior</li>
 : </ul>
 : <p>
 : <span style="text-decoration: underline;"><strong>JTS based validation</strong></span>:
 : </p>
 : <ul>
 : <li>Point:
 : <ul>
 : <li>invalid coordinates</li>
 : </ul>
 : </li>
 : <li>LineString:
 : <ul>
 : <li>invalid coordinates</li>
 : <li>too few points</li>
 : </ul>
 : </li>
 : <li>LinearRing:
 : <ul>
 : <li>invalid coordinates</li>
 : <li>closed ring</li>
 : <li>too few points</li>
 : <li>no self intersecting rings</li>
 : </ul>
 : </li>
 : <li>Polygon
 : <ul>
 : <li>invalid coordinates</li>
 : <li>closed ring</li>
 : <li>too few points</li>
 : <li>consistent area</li>
 : <li>no self intersecting rings</li>
 : <li>holes in shell</li>
 : <li>holes not nested</li>
 : <li>connected interiors</li>
 : </ul>
 : </li>
 : <li>MultiPoint:
 : <ul>
 : <li>invalid coordinates</li>
 : </ul>
 : </li>
 : <li>MultiLineString:
 : <ul>
 : <li>Each contained LineString is validated on its own.</li>
 : </ul>
 : </li>
 : <li>MultiPolygon:
 : <ul>
 : <li>Per polygon:
 : <ul>
 : <li>invalid coordinates</li>
 : <li>closed ring</li>
 : <li>holes in shell</li>
 : <li>holes not nested</li>
 : </ul>
 : </li>
 : <li>too few points</li>
 : <li>consistent area</li>
 : <li>no self intersecting rings</li>
 : <li>shells not nested</li>
 : <li>connected interiors</li>
 : </ul>
 : </li>
 : <li>GeometryCollection:
 : <ul>
 : <li>Each member of the collection is validated on its own.</li>
 : </ul>
 : </li>
 : </ul>
 : <p>
 : General description of checks performed by JTS:
 : </p>
 : <ul>
 : <li>invalid coordinates: x and y are neither NaN or infinite)</li>
 : <li>closed ring: tests if ring is closed; empty rings are closed by definition</li>
 : <li>too few points: tests if length of coordinate array - after repeated points have been removed - is big enough (e.g. &gt;= 4 for a ring, &gt;= 2 for a line string)</li>
 : <li>no self intersecting rings: Check that there is no ring which self-intersects (except of course at its endpoints); required by OGC topology rules</li>
 : <li>consistent area: Checks that the arrangement of edges in a polygonal geometry graph forms a consistent area. Includes check for duplicate rings.</li>
 : <li>holes in shell: Tests that each hole is inside the polygon shell (i.e. hole rings do not cross the shell ring).</li>
 : <li>holes not nested: Tests that no hole is nested inside another hole.</li>
 : <li>connected interiors: Check that the holes do not split the interior of the polygon into at least two pieces.</li>
 : <li>shells not nested: Tests that no element polygon is wholly in the interior of another element polygon (of a MultiPolygon).</li>
 : </ul>
 : </td>
 : </tr>
 : <tr>
 : <td>2</td>
 : <td>Polygon Patch Connectivity</td>
 : <td>Checks that multiple polygon patches within a single surface are connected.</td>
 : </tr>
 : <tr>
 : <td>3</td>
 : <td>Repetition of Position in CurveSegments</td>
 : <td>Checks that consecutive positions within a CurveSegment are not equal.</td>
 : </tr>
 : <tr>
 : <td>4</td>
 : <td>isSimple</td>
 : <td>
 : <p>
 : Tests whether a geometry is simple, based on JTS Geometry.isSimple(). In general, the OGC Simple Features specification of simplicity follows the rule: A Geometry is simple if and only if the only self-intersections are at boundary points.
 : </p>
 : <p>
 : Simplicity is defined for each JTS geometry type as follows:
 : </p>
 : <ul>
 : <li>Polygonal geometries are simple if their rings are simple (i.e., their rings do not self-intersect).
 : <ul>
 : <li>Note: This does not check if different rings of the geometry intersect, meaning that isSimple cannot be used to fully test for (invalid) self-intersections in polygons. The JTS validity check fully tests for self-intersections in polygons, and is part of the general validation in GmlGeoX.</li>
 : </ul>
 : </li>
 : <li>Linear geometries are simple iff they do not self-intersect at points other than boundary points.</li>
 : <li>Zero-dimensional (point) geometries are simple if and only if they have no repeated points.</li>
 : <li>Empty geometries are always simple, by definition.</li>
 : </ul>
 : </td>
 : </tr>
 : </table>
 :
 : Examples:
 :
 : <ul>
 : <li>The mask '0100' indicates that only the 'Polygon Patch Connectivity' test shall be performed.
 : <li>The mask '1110' indicates that all tests except the isSimple test shall be performed .
 : </ul>
 :
 : @param $node the GML geometry to validate
 : @param $testMask test mask
 : @return A mask with the test results, encoded as characters - one at each position (1-based index) of the available tests. 'V' indicates that the test passed, i.e. that the geometry is valid according to that test. 'F' indicates that the test failed. 'S' indicates that the test was skipped. Example: the string 'SVFF' shows that the first test was skipped, while the second test passed and the third and fourth failed.
 :)
declare function geox:validate($node , $testMask as xs:string) as xs:string {
    java:validate($node, $testMask)
};

(:~
 : Validates the given (GML geometry) node, using all available tests.
 : 
 : See the documentation of the geox:validate(element(), string) method for a description of the supported geometry types and tests.
 :
 : @param $node The GML geometry element to validate.
 : @return An XML element with the validation result (for details about its structure, see the description of the result in function geox:validateAndReport(element(), string))
 :)
declare function geox:validateAndReport($node )  {
    java:validateAndReport($node)
};

(:~
 : Validates the given (GML geometry) node. The validation tasks to perform are specified via the given mask.
 :
 : See the documentation of the geox:validate(element(), string) method for a description of the supported geometry types, tests, and the test mask.
 :
 : @param $node The GML geometry element to validate.
 : @param $testMask Defines which tests shall be executed.
 : @return a DOM element, with the validation result and validation message (providing further details about any errors). The validation result is encoded as a sequence of characters - one at each position (1-based index) of the available tests. 'V' indicates that the test passed, i.e. that the geometry is valid according to that test. 'F' indicates that the test failed. 'S' indicates that the test was skipped. Example: the string 'SVFF' shows that the first test was skipped, while the second test passed and the third and fourth failed.
 :
 :         <ggeo:ValidationResult xmlns:ggeo=
 : "de.interactive_instruments.etf.bsxm.GmlGeoX">
 :           <ggeo:valid>false</ggeo:valid>
 :           <ggeo:result>VFV</ggeo:result>
 :           <ggeo:errors>
 :             <etf:message
 :               xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
 :               ref="TR.gmlgeox.validation.geometry.jts.5">
 :               <etf:argument token="original">Invalid polygon. Two rings of the polygonal geometry intersect.</etf:argument>
 :               <etf:argument token="ID">DETHL56P0000F1TJ</etf:argument>
 :               <etf:argument token="context">Surface</etf:argument>
 :               <etf:argument token="coordinates">666424.2393405803,5614560.422015165</etf:argument>
 :             </etf:message>
 :           </ggeo:errors>
 :         </ggeo:ValidationResult>
 :
 :         Where:
 :         <ul>
 :         <li>ggeo:valid - contains the boolean value indicating if the object passed all tests (defined by the testMask).
 :         <li>ggeo:result - contains a string that is a mask with the test results, encoded as characters - one at each position (1-based index) of the available tests. 'V' indicates that the test passed, i.e. that the geometry is valid according to that test. 'F' indicates that the test failed. 'S' indicates that the test was skipped.
 :         <li>ggeo:message (one for each message produced during validation) contains:
 :         <ul>
 :         <li>an XML attribute 'type' that indicates the severity level of the message ('FATAL', 'ERROR', 'WARNING', or 'NOTICE')
 :         <li>the actual validation message as text content
 :         </ul>
 :         </ul>
 :)
declare function geox:validateAndReport($node , $testMask as xs:string)  {
    java:validateAndReport($node, $testMask)
};

(:~
 : Tests if the first geometry contains the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a GML geometry element
 : @param $geom2 represents the second geometry, encoded as a GML geometry element
 : @return true() if the first geometry contains the second one, else false()
 :)
declare function geox:contains($geom1 , $geom2 ) as xs:boolean {
    java:contains($geom1, $geom2)
};

(:~
 : Tests if one geometry contains a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $arg1 represents the first geometry, encoded as a GML geometry element
 : @param $arg2 represents a list of geometries, encoded as GML geometry elements
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:contains($arg1 , $arg2 , $matchAll as xs:boolean) as xs:boolean {
    java:contains($arg1, $arg2, $matchAll)
};

(:~
 : Tests if the first geometry contains the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents the second geometry, encoded as a JTS geometry object
 : @return true() if the first geometry contains the second one, else false().
 :)
declare function geox:containsGeomGeom($geom1 , $geom2 ) as xs:boolean {
    java:containsGeomGeom($geom1, $geom2)
};

(:~
 : Tests if one geometry contains a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:containsGeomGeom($geom1 , $geom2 , $matchAll as xs:boolean) as xs:boolean {
    java:containsGeomGeom($geom1, $geom2, $matchAll)
};

(:~
 : Tests if the first geometry crosses the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a GML geometry element
 : @param $geom2 represents the second geometry, encoded as a GML geometry element
 : @return true() if the first geometry crosses the second one, else false() .
 :)
declare function geox:crosses($geom1 , $geom2 ) as xs:boolean {
    java:crosses($geom1, $geom2)
};

(:~
 : Tests if one geometry crosses a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $arg1 represents the first geometry, encoded as a GML geometry element
 : @param $arg2 represents a list of geometries, encoded as GML geometry elements
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:crosses($arg1 , $arg2 , $matchAll as xs:boolean) as xs:boolean {
    java:crosses($arg1, $arg2, $matchAll)
};

(:~
 : Tests if the first geometry crosses the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents the second geometry, encoded as a JTS geometry object
 : @return true() if the first geometry crosses the second one, else false() .
 :)
declare function geox:crossesGeomGeom($geom1 , $geom2 ) as xs:boolean {
    java:crossesGeomGeom($geom1, $geom2)
};

(:~
 : Tests if one geometry crosses a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:crossesGeomGeom($geom1 , $geom2 , $matchAll as xs:boolean) as xs:boolean {
    java:crossesGeomGeom($geom1, $geom2, $matchAll)
};

(:~
 : Tests if the first geometry equals the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a GML geometry element
 : @param $geom2 represents the second geometry, encoded as a GML geometry element
 : @return true() if the first geometry equals the second one, else false().
 :)
declare function geox:equals($geom1 , $geom2 ) as xs:boolean {
    java:equals($geom1, $geom2)
};

(:~
 : Tests if one geometry equals a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $arg1 represents the first geometry, encoded as a GML geometry element
 : @param $arg2 represents a list of geometries, encoded as GML geometry elements
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:equals($arg1 , $arg2 , $matchAll as xs:boolean) as xs:boolean {
    java:equals($arg1, $arg2, $matchAll)
};

(:~
 : Tests if the first geometry equals the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents the second geometry, encoded as a JTS geometry object
 : @return true() if the first geometry equals the second one, else false().
 :)
declare function geox:equalsGeomGeom($geom1 , $geom2 ) as xs:boolean {
    java:equalsGeomGeom($geom1, $geom2)
};

(:~
 : Tests if one geometry equals a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:equalsGeomGeom($geom1 , $geom2 , $matchAll as xs:boolean) as xs:boolean {
    java:equalsGeomGeom($geom1, $geom2, $matchAll)
};

(:~
 : Tests if the first geometry intersects the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a GML geometry element
 : @param $geom2 represents the second geometry, encoded as a GML geometry element
 : @return true() if the first geometry intersects the second one, else false().
 :)
declare function geox:intersects($geom1 , $geom2 ) as xs:boolean {
    java:intersects($geom1, $geom2)
};

(:~
 : Tests if one geometry intersects a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $arg1 represents the first geometry, encoded as a GML geometry element
 : @param $arg2 represents a list of geometries, encoded as GML geometry elements
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:intersects($arg1 , $arg2 , $matchAll as xs:boolean) as xs:boolean {
    java:intersects($arg1, $arg2, $matchAll)
};

(:~
 : Tests if the first geometry intersects the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents the second geometry, encoded as a JTS geometry object
 : @return true() if the first geometry intersects the second one, else false().
 :)
declare function geox:intersectsGeomGeom($geom1 , $geom2 ) as xs:boolean {
    java:intersectsGeomGeom($geom1, $geom2)
};

(:~
 : Tests if one geometry intersects a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:intersectsGeomGeom($geom1 , $geom2 , $matchAll as xs:boolean) as xs:boolean {
    java:intersectsGeomGeom($geom1, $geom2, $matchAll)
};

(:~
 : Determine the name of the SRS that applies to the given geometry element. The SRS is looked up as follows (in order):
 :
 : <ol>
 : <li>If the element itself has an 'srsName' attribute, then the value of that attribute is returned.
 : <li>Otherwise, if a standard SRS is defined (see geox:setStandardSRS(String)), it is used.
 : <li>Otherwise, if an ancestor of the given element has the 'srsName' attribute, then the value of that attribute from the first ancestor (i.e., the nearest) is used.
 : <li>Otherwise, if an ancestor of the given element has a child element with local name 'boundedBy', which itself has a child with local name 'Envelope', and that child has an 'srsName' attribute, then the value of that attribute from the first ancestor (i.e., the nearest) that fulfills the criteria is used.
 : </ol>
 :
 : NOTE: The lookup is independent of a specific GML namespace.
 :
 : @param $geometryNode a gml geometry node
 : @return the value of the applicable 'srsName' attribute, if found, otherwise the empty sequence

 :)
declare function geox:determineSrsName($geometryNode ) as xs:string? {
    java:determineSrsName($geometryNode)
};

(:~
 : Determine the name of the SRS that applies to the given geometry component element (e.g. a curve segment). The SRS is looked up as follows (in order):
 :
 : <ol>
 : <li>If a standard SRS is defined (see geox:setStandardSRS(String)), it is used.
 : <li>Otherwise, if an ancestor of the given element has the 'srsName' attribute, then the value of that attribute from the first ancestor (i.e., the nearest) is used.
 : <li>Otherwise, if an ancestor of the given element has a child element with local name 'boundedBy', which itself has a child with local name 'Envelope', and that child has an 'srsName' attribute, then the value of that attribute from the first ancestor (i.e., the nearest) that fulfills the criteria is used.
 : </ol>
 :
 : NOTE: The lookup is independent of a specific GML namespace.
 :
 : @param $geometryComponentNode a gml geometry component node (e.g. Arc or Circle)
 : @return the value of the applicable 'srsName' attribute, if found, otherwise the empty sequence
 :)
declare function geox:determineSrsNameForGeometryComponent($geometryComponentNode ) as xs:string? {
    java:determineSrsNameForGeometryComponent($geometryComponentNode)
};

(:~
 : Parse a geometry.
 :
 : @param $v either a geometry node or a JTS geometry
 : @return a JTS geometry
 :)
declare function geox:parseGeometry($v ) {
    java:parseGeometry($v)
};

(:~
 : Tests if the first and the second geometry are disjoint.
 :
 : @param $geom1 represents the first geometry, encoded as a GML geometry element
 : @param $geom2 represents the second geometry, encoded as a GML geometry element
 : @return true() if the first and the second geometry are disjoint, else false().
 :)
declare function geox:isDisjoint($geom1 , $geom2 ) as xs:boolean {
    java:isDisjoint($geom1, $geom2)
};

(:~
 : Tests if one geometry is disjoint with a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $arg1 represents the first geometry, encoded as a GML geometry element
 : @param $arg2 represents a list of geometries, encoded as GML geometry elements
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:isDisjoint($arg1 , $arg2 , $matchAll as xs:boolean) as xs:boolean {
    java:isDisjoint($arg1, $arg2, $matchAll)
};

(:~
 : Tests if the first and the second geometry are disjoint.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents the second geometry, encoded as a JTS geometry object
 : @return true() if the first and the second geometry are disjoint, else false().
 :)
declare function geox:isDisjointGeomGeom($geom1 , $geom2 ) as xs:boolean {
    java:isDisjointGeomGeom($geom1, $geom2)
};

(:~
 : Tests if one geometry is disjoint with a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:isDisjointGeomGeom($geom1 , $geom2 , $matchAll as xs:boolean) as xs:boolean {
    java:isDisjointGeomGeom($geom1, $geom2, $matchAll)
};

(:~
 : Tests if the first geometry is within the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a GML geometry element
 : @param $geom2 represents the second geometry, encoded as a GML geometry element
 : @return true() if the first geometry is within the second geometry, else false().
 :)
declare function geox:isWithin($geom1 , $geom2 ) as xs:boolean {
    java:isWithin($geom1, $geom2)
};

(:~
 : Tests if one geometry is within a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $arg1 represents the first geometry, encoded as a GML geometry element
 : @param $arg2 represents a list of geometries, encoded as GML geometry elements
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:isWithin($arg1 , $arg2 , $matchAll as xs:boolean) as xs:boolean {
    java:isWithin($arg1, $arg2, $matchAll)
};

(:~
 : Tests if the first geometry is within the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents the second geometry, encoded as a JTS geometry object
 : @return true() if the first geometry is within the second geometry, else false().
 :)
declare function geox:isWithinGeomGeom($geom1 , $geom2 ) as xs:boolean {
    java:isWithinGeomGeom($geom1, $geom2)
};

(:~
 : Tests if one geometry is within a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty. 
 :)
declare function geox:isWithinGeomGeom($geom1 , $geom2 , $matchAll as xs:boolean) as xs:boolean {
    java:isWithinGeomGeom($geom1, $geom2, $matchAll)
};

(:~
 : Tests if the first geometry overlaps the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a GML geometry element
 : @param $geom2 represents the second geometry, encoded as a GML geometry element
 : @return true() if the first geometry overlaps the second one, else false().
 :)
declare function geox:overlaps($geom1 , $geom2 ) as xs:boolean {
    java:overlaps($geom1, $geom2)
};

(:~
 : Tests if one geometry overlaps a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $arg1 represents the first geometry, encoded as a GML geometry element
 : @param $arg2 represents a list of geometries, encoded as GML geometry elements
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:overlaps($arg1 , $arg2 , $matchAll as xs:boolean) as xs:boolean {
    java:overlaps($arg1, $arg2, $matchAll)
};

(:~
 : Tests if the first geometry overlaps the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents the second geometry, encoded as a JTS geometry object
 : @return true() if the first geometry intersects the second one, else false().
 :)
declare function geox:overlapsGeomGeom($geom1 , $geom2 ) as xs:boolean {
    java:overlapsGeomGeom($geom1, $geom2)
};

(:~
 : Tests if one geometry overlaps a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:overlapsGeomGeom($geom1 , $geom2 , $matchAll as xs:boolean) as xs:boolean {
    java:overlapsGeomGeom($geom1, $geom2, $matchAll)
};

(:~
 : Tests if the first geometry touches the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a GML geometry element
 : @param $geom2 represents the second geometry, encoded as a GML geometry element
 : @return true() if the first geometry touches the second one, else false().
 :)
declare function geox:touches($geom1 , $geom2 ) as xs:boolean {
    java:touches($geom1, $geom2)
};

(:~
 : Tests if one geometry touches a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $arg1 represents the first geometry, encoded as a GML geometry element
 : @param $arg2 represents a list of geometries, encoded as GML geometry elements
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:touches($arg1 , $arg2 , $matchAll as xs:boolean) as xs:boolean {
    java:touches($arg1, $arg2, $matchAll)
};

(:~
 : Tests if the first geometry touches the second geometry.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents the second geometry, encoded as a JTS geometry object
 : @return true() if the first geometry intersects the second one, else false()
 :)
declare function geox:touchesGeomGeom($geom1 , $geom2 ) as xs:boolean {
    java:touchesGeomGeom($geom1, $geom2)
};

(:~
 : Tests if one geometry touches a list of geometries. Whether a match is required for all or just one of these is controlled via parameter.
 :
 : @param $geom1 represents the first geometry, encoded as a JTS geometry object
 : @param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 : @param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 : @return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:touchesGeomGeom($geom1 , $geom2 , $matchAll as xs:boolean) as xs:boolean {
    java:touchesGeomGeom($geom1, $geom2, $matchAll)
};

(:~
 : Adds the name of a GML geometry element to the set of elements for which validation is performed.
 :
 : @param $gmlGeometry name (simple, i.e. without namespace (prefix)) of a GML geometry element to validate.
 :)
declare function geox:registerGmlGeometry($gmlGeometry as xs:string) as empty-sequence() {
    java:registerGmlGeometry($gmlGeometry)
};

(:~
 : Set the standard SRS to use for a geometry if no srsName attribute is directly defined for it. Setting a standard SRS can improve performance, but should only be done if all geometry elements without srsName attribute have the same SRS.
 :
 : @param $srsName name of the SRS to assign to a geometry if it does not have an srsName attribute itself.
 :)
declare function geox:setStandardSRS($srsName as xs:string) as empty-sequence() {
    java:setStandardSRS($srsName)
};

(:~
 : Set the maximum number of points to be created when interpolating an arc. Default is 1000. The lower the maximum error (set via geox:setMaxErrorForInterpolation(double)), the higher the number of points needs to be. Arc interpolation will never create more than the configured maximum number of points. However, the interpolation will also never create more points than needed to achieve the maximum error. In order to achieve interpolations with a very low maximum error, the maximum number of points needs to be increased accordingly.
 :
 : @param $maxNumPoints maximum number of points to be created when interpolating an arc
 :)
declare function geox:setMaxNumPointsForInterpolation($maxNumPoints as xs:int) as empty-sequence() {
    java:setMaxNumPointsForInterpolation($maxNumPoints)
};

(:~
 : Set the maximum error (e.g. 0.00000001 - default setting is 0.00001), i.e. the maximum difference between an arc and the interpolated line string - that shall be achieved when creating new arc interpolations. The lower the error (maximum difference), the more interpolation points will be needed. However, note that a maximum for the number of such points exists. It can be set via geox:setMaxNumPointsForInterpolation(int) (default value is stated in the documentation of that method).
 :
 : @param $maxError the maximum difference between an arc and the interpolated line
 :)
declare function geox:setMaxErrorForInterpolation($maxError as xs:double) as empty-sequence() {
    java:setMaxErrorForInterpolation($maxError)
};

(:~
 : Removes the name of a GML geometry element from the set of elements for which validation is performed.
 :
 : @param $nodeName name (simple, i.e. without namespace (prefix)) of a GML geometry element to remove from validation.
 :)
declare function geox:unregisterGmlGeometry($nodeName as xs:string) as empty-sequence() {
    java:unregisterGmlGeometry($nodeName)
};

(:~
 : Removes all names of GML geometry elements that are currently registered for validation.
 :)
declare function geox:unregisterAllGmlGeometries() as empty-sequence() {
    java:unregisterAllGmlGeometries()
};

(:~
 : @return the currently registered GML geometry element names (comma separated)
 :)
declare function geox:registeredGmlGeometries() as xs:string {
    java:registeredGmlGeometries()
};

(:~
 : Create the union of the given geometry nodes.
 :
 : @param $val a single or collection of geometry nodes.
 : @return the union of the geometries - can be a JTS geometry collection
 :)
declare function geox:union($val) {
    java:union($val)
};

(:~
 : Check if a JTS geometry is empty.
 :
 : @param $geom the JTS geometry to check
 : @return true() if the geometry is null or empty, else false()
 :)
declare function geox:isEmptyGeom($geom ) as xs:boolean {
    java:isEmptyGeom($geom)
};

(:~
 : Checks that the second control point of each arc in the given $arcStringNode is positioned in the middle third of that arc.
 :
 : @param $arcStringNode a gml:Arc or gml:ArcString element
 : @return The coordinate of the second control point of the first invalid arc, or the empty sequence if all arcs are valid.
 :)
declare function geox:checkSecondControlPointInMiddleThirdOfArc($arcStringNode ) {
    java:checkSecondControlPointInMiddleThirdOfArc($arcStringNode)
};

(:~
 : Checks that the three control points of a gml:Circle are at least a given amount of degrees apart from each other.
 :
 : @param $circleNode a gml:Circle element, defined by three control points
 : @param $minSeparationInDegree the minimum angle between each control point, in degree (0<=x<=120)
 : @return The JTS coordinate of a control point which does not have the minimum angle to one of the other control points, or the empty sequence if the angles between all points are greater than or equal to the minimum separation angle
 :)
declare function geox:checkMinimumSeparationOfCircleControlPoints($circleNode , $minSeparationInDegree )  {
    java:checkMinimumSeparationOfCircleControlPoints($circleNode, $minSeparationInDegree)
};

(:~
 : Checks if a given geometry is closed. Only LineStrings and MultiLineStrings are checked.
 :
 : @param $geom the geometry to check
 : @return true(), if the geometry is closed, else false() 
 :)
declare function geox:isClosedGeom($geom ) as xs:boolean {
    java:isClosedGeom($geom)
};

(:~
 : Checks if the geometry represented by the given node is closed. Only LineStrings and MultiLineStrings are checked.
 :
 : @param $geom the geometry to check
 : @return true(), if the geometry is closed, else false()
 :)
declare function geox:isClosed($geom ) as xs:boolean {
    java:isClosed($geom)
};

(:~
 : Checks if a given geometry is closed. Points and MultiPoints are closed by definition (they do not have a boundary). Polygons and MultiPolygons are never closed in 2D, and since operations in 3D are not supported, this method will always return false() if a polygon is encountered - unless the parameter onlyCheckCurveGeometries is set to true(). LinearRings are closed by definition. The remaining geometry types that will be checked are LineString and MultiLineString. If a (Multi)LineString is not closed, this method will return false().
 :
 : @param $geom the geometry to test
 : @param $onlyCheckCurveGeometries true() if only curve geometries (i.e., for JTS: LineString, LinearRing, and MultiLineString) shall be tested, else false() (in this case, the occurrence of polygons will result in the return value false()).
 : @return true() if the given geometry is closed, else false()
 :)
declare function geox:isClosedGeom($geom , $onlyCheckCurveGeometries as xs:boolean) as xs:boolean {
    java:isClosedGeom($geom, $onlyCheckCurveGeometries)
};

(:~
 : Checks if the geometry represented by the given node is closed. Points and MultiPoints are closed by definition (they do not have a boundary). Polygons and MultiPolygons are never closed in 2D, and since operations in 3D are not supported, this method will always return false() if a polygon is encountered - unless the parameter onlyCheckCurveGeometries is set to true(). LinearRings are closed by definition. The remaining geometry types that will be checked are LineString and MultiLineString. If a (Multi)LineString is not closed, this method will return false().
 :
 : @param $geomNode the geometry node to test
 : @param $onlyCheckCurveGeometries true() if only curve geometries (i.e., for JTS: LineString, LinearRing, and MultiLineString) shall be tested, else false() (in this case, the occurrence of polygons will result in the return value false()).
 : @return true() if the geometry represented by the given node is closed, else false().
 :)
declare function geox:isClosed($geomNode , $onlyCheckCurveGeometries as xs:boolean) as xs:boolean {
    java:isClosed($geomNode, $onlyCheckCurveGeometries)
};

(:~
 : Identifies the holes contained in the geometry represented by the given geometry node and returns them as a JTS geometry. If holes were found a union is built, to ensure that the result is a valid JTS Polygon or JTS MultiPolygon. If no holes were found an empty JTS GeometryCollection is returned.
 :
 : @param $geometryNode potentially existing holes will be extracted from the geometry represented by this node (the geometry can be a Polygon, MultiPolygon, or any other JTS geometry)
 : @return A geometry (JTS Polygon or MultiPolygon) with the holes contained in the given geometry. Can also be an empty JTS GeometryCollection
 :)
declare function geox:holes($geometryNode ) {
    java:holes($geometryNode)
};

(:~
 : Identifies the holes contained in the given geometry and returns them as a JTS geometry. If holes were found a union is built, to ensure that the result is a valid JTS Polygon or JTS MultiPolygon. If no holes were found an empty JTS GeometryCollection is returned.
 :
 : @param $geom potentially existing holes will be extracted from this geometry (can be a Polygon, MultiPolygon, or any other JTS geometry)
 : @return A geometry (JTS Polygon or MultiPolygon) with the holes contained in the given geometry. Can also be an empty JTS GeometryCollection
 :)
declare function geox:holesGeom($geom ) {
    java:holesGeom($geom)
};

(:~
 : Identifies the holes contained in the given geometry and returns them as polygons within a JTS geometry collection.
 :
 : @param $geom potentially existing holes will be extracted from this geometry (can be a Polygon, MultiPolygon, or any other JTS geometry)
 : @return A JTS geometry collection with the holes (as polygons) contained in the given geometry. Can also be an empty JTS geometry
 :)
declare function geox:holesAsGeometryCollection($geom ) {
    java:holesAsGeometryCollection($geom)
};

(:~
 : Check if a given geometry node is valid.
 :
 : @param $geometryNode the geometry element
 : @return true() if the given node represents a valid geometry, else false()
 :)
declare function geox:isValid($geometryNode ) as xs:boolean {
    java:isValid($geometryNode)
};

(:~
 :Tests if the first geometry relates to the second geometry as defined by the given intersection pattern.
 :
 :@param $arg1 represents the first geometry, encoded as a GML geometry element
 :@param $arg2 represents the second geometry, encoded as a GML geometry element
 :@param $intersectionPattern the pattern against which to check the intersection matrix for the two geometries (IxI,IxB,IxE,BxI,BxB,BxE,ExI,ExB,ExE)
 :@return true() if the DE-9IM intersection matrix for the two geometries matches the $intersectionPattern, else false().
 :)
declare function geox:relate($arg1 , $arg2 , $intersectionPattern as xs:string) as xs:boolean {
    java:relate($arg1, $arg2, $intersectionPattern)
};

(:~
 :Tests if one geometry relates to a list of geometries as defined by the given intersection pattern. Whether a match is required for all or just one of these is controlled via parameter.
 :
 :@param $value1 represents the first geometry, encoded as a GML geometry element
 :@param $value2 represents a list of geometries, encoded as GML geometry elements
 :@param $intersectionPattern the pattern against which to check the intersection matrix for the geometries (IxI,IxB,IxE,BxI,BxB,BxE,ExI,ExB,ExE)
 :@param $matchAll true() if $arg1 must fulfill the spatial relationship defined by the $intersectionPattern for all geometries in $arg2, else false()
 :@return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:relate($value1 , $value2 , $intersectionPattern as xs:string, $matchAll as xs:boolean) as xs:boolean {
    java:relate($value1, $value2, $intersectionPattern, $matchAll)
};

(:~
 :Tests if the first geometry relates to the second geometry as defined by the given intersection pattern.
 :
 :@param $geom1 represents the first geometry, encoded as a JTS geometry object
 :@param $geom2 represents the second geometry, encoded as a JTS geometry object
 :@param $intersectionPattern the pattern against which to check the intersection matrix for the two geometries (IxI,IxB,IxE,BxI,BxB,BxE,ExI,ExB,ExE)
 :@return true() if the DE-9IM intersection matrix for the two geometries matches the $intersectionPattern, else false().
 :)
declare function geox:relateGeomGeom($geom1 , $geom2 , $intersectionPattern as xs:string) as xs:boolean {
    java:relateGeomGeom($geom1, $geom2, $intersectionPattern)
};

(:~
 :Tests if one geometry relates to a list of geometries as defined by the given intersection pattern. Whether a match is required for all or just one of these is controlled via parameter.
 :
 :@param $geom1 represents the first geometry, encoded as a JTS geometry object
 :@param $geom2 represents a list of geometries, encoded as a JTS geometry object (typically a JTS geometry collection)
 :@param $intersectionPattern the pattern against which to check the intersection matrix for the geometries (IxI,IxB,IxE,BxI,BxB,BxE,ExI,ExB,ExE)
 :@param $matchAll true() if $arg1 must fulfill the spatial relationship for all geometries in $arg2, else false()
 :@return true() if the conditions are met, else false(). false() will also be returned if $arg2 is empty.
 :)
declare function geox:relateGeomGeom($geom1 , $geom2 , $intersectionPattern as xs:string, $matchAll as xs:boolean) as xs:boolean {
    java:relateGeomGeom($geom1, $geom2, $intersectionPattern, $matchAll)
};

(:~
 : Computes the intersection between the first and the second geometry node.
 :
 : @param $geometry1 represents the first geometry
 : @param $geometry2 represents the second geometry
 : @return the point-set common to the two geometries
 :)
declare function geox:intersection($geometry1 , $geometry2 ) {
    java:intersection($geometry1, $geometry2)
};

(:~
 : Computes the intersection between the first and the second geometry.
 :
 : @param $geometry1 the first geometry
 : @param $geometry2 the second geometry
 : @return the point-set common to the two geometries
 :)
declare function geox:intersectionGeomGeom($geometry1 , $geometry2 ) {
    java:intersectionGeomGeom($geometry1, $geometry2)
};

(:~
 : Computes the difference between the first and the second geometry node.
 :
 : @param $geometry1 represents the first geometry
 : @param $geometry2 represents the second geometry
 : @return the closure of the point-set of the points contained in $geometry1 that are not contained in $geometry2, as a JTS Geometry object.
 :)
declare function geox:difference($geometry1 , $geometry2 ) {
    java:difference($geometry1, $geometry2)
};

(:~
 : Returns the area of the geometry encoded in the given geometry node. Only areal geometries have a non-zero area.
 :
 : @param $geometryNode a GML element
 : @return the area of the geometry
 :)
declare function geox:area($geometryNode ) {
    java:area($geometryNode)
};

(:~
 : Returns the area of the given geometry. Only areal geometries have a non-zero area.
 :
 : @param $geometry a JTS Geometry object
 : @return the area of the geometry
 :)
declare function geox:areaGeom($geometry ) {
    java:areaGeom($geometry)
};

(:~
 : Returns the centroid of the geometry represented by the given geometry node. The centroid is equal to the centroid of the set ofcomponent geometries of highest dimension (since the lower-dimension geometries contribute zero"weight" to the centroid). The centroid of an empty geometry is POINT EMPTY.
 :
 : @param $geometryNode a GML element
 : @return the centroid of the geometry
 :)
declare function geox:centroid($geometryNode ) {
    java:centroid($geometryNode)
};

(:~
 : Returns the centroid of the given geometry. The centroid is equal to the centroid of the set ofcomponent geometries of highest dimension (since the lower-dimension geometries contribute zero"weight" to the centroid). The centroid of an empty geometry is POINT EMPTY.
 :
 : @param $geometry a JTS Geometry object
 : @return the centroid of the geometry
 :)
declare function geox:centroidGeom($geometry ) {
    java:centroidGeom($geometry)
};

(:~
 : Returns the boundary, or an empty geometry of appropriate dimension if the given geometry is empty or has no boundary (e.g. a curve whose end points are equal). (In the case of zero-dimensional geometries, an empty JTS GeometryCollection is returned.) For a discussion of this function, see the OpenGIS SimpleFeatures Specification. As stated in SFS Section 2.1.13.1, "the boundary of a Geometry is a set of Geometries of the next lower dimension."
 :
 : @param $geometryNode an GML element
 : @return the closure of the combinatorial boundary of the geometry, as a JTS Geometry object
 :)
declare function geox:boundary($geometryNode ) {
    java:boundary($geometryNode)
};

(:~
 : Returns the boundary, or an empty geometry of appropriate dimension if the given geometry is empty or has no boundary (e.g. a curve whose end points are equal). (In the case of zero-dimensional geometries, an empty JTS GeometryCollection is returned.) For a discussion of this function, see the OpenGIS SimpleFeatures Specification. As stated in SFS Section 2.1.13.1, "the boundary of a Geometry is a set of Geometries of the next lower dimension."
 :
 : @param $geometry a JTS Geometry object
 : @return the closure of the combinatorial boundary of the geometry, as a JTS Geometry object
 :)
declare function geox:boundaryGeom($geometry ) {
    java:boundaryGeom($geometry)
};

(:~
 : Computes the difference between the first and the second geometry.
 :
 : @param $geometry1 the first geometry
 : @param $geometry2 the second geometry
 : @return the closure of the point-set of the points contained in $geometry1 that are not contained in $geometry2, as a JTS Geometry object.
 :)
declare function geox:differenceGeomGeom($geometry1 , $geometry2 ) {
    java:differenceGeomGeom($geometry1, $geometry2)
};

(:~
 : Computes the envelope of a geometry.
 :
 : @param $geometryNode represents the geometry
 : @return The bounding box, an array { x1, y1, x2, y2 }
 :)
declare function geox:envelope($geometryNode )  {
    java:envelope($geometryNode)
};

(:~
 : Computes the envelope of a geometry.
 :
 : @param $geometry the JTS geometry
 : @return The bounding box, as an array { x1, y1, x2, y2 }
 :)
declare function geox:envelopeGeom($geometry )  {
    java:envelopeGeom($geometry)
};

(:~
 : Retrieves the end points of the curve represented by the geometry node.
 :
 : NOTE: This is different to computing the boundary of a curve in case that the curve end points are equal (in that case, the curve does not have a boundary).
 :
 : @param $geomNode the geometry element
 : @return A sequence with the two end points of the curve geometry (node); can be empty if the given geometry nodes does not represent a single curve.
 :)
declare function geox:curveEndPoints($geomNode )  {
    java:curveEndPoints($geomNode)
};

(:~
 : Searches the default spatial r-tree index for items whose minimum bounding box intersects with the rectangle defined by the given coordinates.
 :
 : @param $minx represents the minimum value on the first coordinate axis; a number
 : @param $miny represents the minimum value on the second coordinate axis; a number
 : @param $maxx represents the maximum value on the first coordinate axis; a number
 : @param $maxy represents the maximum value on the second coordinate axis; a number
 : @return the node set of all items in the envelope
 :)
declare function geox:search($minx , $miny , $maxx , $maxy )  {
    java:search($minx, $miny, $maxx, $maxy)
};

(:~
 : Searches the named spatial r-tree index for items whose minimum bounding box intersects with the rectangle defined by the given coordinates.
 :
 : @param $indexName Identifies the index. The empty sequence or the empty string identifies the default index.
 : @param $minx represents the minimum value on the first coordinate axis; a number
 : @param $miny represents the minimum value on the second coordinate axis; a number
 : @param $maxx represents the maximum value on the first coordinate axis; a number
 : @param $maxy represents the maximum value on the second coordinate axis; a number
 : @return the node set of all items in the envelope
 :)
declare function geox:search($indexName as xs:string?, $minx , $miny , $maxx , $maxy )  {
    java:search($indexName, $minx, $miny, $maxx, $maxy)
};

(:~
 : Searches the default spatial r-tree index for items whose minimum bounding box intersects with the the minimum bounding box of the given geometry node.
 :
 : @param $geometryNode the geometry element
 : @return the node set of all items in the envelope
 :)
declare function geox:search($geometryNode )  {
    java:search($geometryNode)
};

(:~
 : Searches the named spatial r-tree index for items whose minimum bounding box intersects with the the minimum bounding box of the given geometry node.
 :
 : @param $indexName Identifies the index. The empty sequence or the empty string identifies the default index.
 : @param $geometryNode the geometry element
 : @return the node set of all items in the envelope
 :)
declare function geox:search($indexName as xs:string?, $geometryNode )  {
    java:search($indexName, $geometryNode)
};

(:~
 : Searches the default spatial r-tree index for items whose minimum bounding box intersects with the the minimum bounding box of the given geometry.
 :
 : @param $geom the geometry
 : @return the node set of all items in the envelope
 :)
declare function geox:searchGeom($geom )  {
    java:searchGeom($geom)
};

(:~
 : Searches the named spatial r-tree index for items whose minimum bounding box intersects with the the minimum bounding box of the given geometry.
 :
 : @param $indexName Identifies the index. The empty sequence or the empty string identifies the default index.
 : @param $geom the geometry
 : @return the node set of all items in the envelope
 :)
declare function geox:searchGeom($indexName as xs:string?, $geom )  {
    java:searchGeom($indexName, $geom)
};

(:~
 : Returns all items in the default spatial r-tree index.
 :
 : @return the node set of all items in the index
 :)
declare function geox:search()  {
    java:search()
};

(:~
 : Returns all items in the named spatial r-tree index.
 :
 : @param $indexName Identifies the index. The empty sequence or the empty string identifies the default index.
 : @return the node set of all items in the index 
 :)
declare function geox:searchInIndex($indexName as xs:string?)  {
    java:searchInIndex($indexName)
};

(:~
 : Set cache size for geometries. The cache will be reset.
 :
 : @param $size the size of the geometry cache; default is 100000
 :)
declare function geox:cacheSize($size ) as empty-sequence() {
    java:cacheSize($size)
};

(:~
 : Get the current size of the geometry cache.
 :
 : @return the size of the geometry cache
 :)
declare function geox:getCacheSize() as xs:int {
    java:getCacheSize()
};

(:~
 : Indexes a feature geometry, using the default index.
 :
 : @param $node represents the indexed item node (can be the gml:id of a GML feature element, or the element itself)
 : @param $geometry represents the GML geometry to index 
 :)
declare function geox:index($node , $geometry ) as empty-sequence() {
    java:index($node, $geometry)
};

(:~
 : Removes the named spatial index. WARNING: Be sure you know what you are doing.
 :
 : @param $indexName Identifies the index. The empty string identifies the default index.
 :)
declare function geox:removeIndex($indexName as xs:string) as empty-sequence() {
    java:removeIndex($indexName)
};

(:~
 : Indexes a feature geometry, using the named spatial index.
 :
 : @param $indexName Identifies the index. The empty string identifies the default index.
 : @param $node represents the indexed item node (can be the gml:id of a GML feature element, or the element itself)
 : @param $geometry represents the GML geometry to index
 :)
declare function geox:index($indexName as xs:string, $node , $geometry ) as empty-sequence() {
    java:index($indexName, $node, $geometry)
};

(:~
 : Checks if the coordinates of the given $point are equal (comparing x, y, and z) to the coordinates of one of the points that define the given $geometry.
 :
 : @param $point The JTS Point whose coordinates are checked against the coordinates of the points of $geometry
 : @param $geometry The JTS Geometry whose points are checked to see if one of them has coordinates equal to that of $point
 : @return true() if the coordinates of the given $point are equal to the coordinates of one of the points that define $geometry, else false()
 :)
declare function geox:pointCoordInGeometryCoords($point , $geometry ) as xs:boolean {
    java:pointCoordInGeometryCoords($point, $geometry)
};

(:~
 : Checks if for each curve of the given $geomNode a minimum (defined by parameter $minMatchesPerCurve) number of identical curves (same control points - ignoring curve orientation) from the $otherGeomsNodes exists.
 :
 : @param $geomNode GML geometry node
 : @param $otherGeomsNodes one or more database nodes representing GML geometries
 : @param $minMatchesPerCurve the minimum number of matching identical curves that must be found for each curve from the $geomNode
 : @return The empty sequence, if all curves are matched correctly, otherwise the JTS geometry of the first curve from $geomNode which is not covered by the required number of identical curves from $otherGeomsNodes
 :)
declare function geox:curveUnmatchedByIdenticalCurvesMin($geomNode , $otherGeomsNodes , $minMatchesPerCurve as xs:int) {
    java:curveUnmatchedByIdenticalCurvesMin($geomNode, $otherGeomsNodes, $minMatchesPerCurve)
};

(:~
 : Checks if for each curve of the given $geomNode a maximum (defined by parameter $maxMatchesPerCurve) number of identical curves (same control points - ignoring curve orientation) from the $otherGeomsNodes exists.
 :
 : @param $geomNode GML geometry node
 : @param $otherGeomsNodes one or more database nodes representing GML geometries
 : @param $maxMatchesPerCurve the maximum number of matching identical curves that are allowed to be found for each curve from the $geomNode
 : @return The empty sequence, if all curves are matched correctly, otherwise the JTS geometry of the first curve from $geomNode which is covered by more than the allowed number of identical curves from $otherGeomsNodes
 :)
declare function geox:curveUnmatchedByIdenticalCurvesMax($geomNode , $otherGeomsNodes , $maxMatchesPerCurve as xs:int) {
    java:curveUnmatchedByIdenticalCurvesMax($geomNode, $otherGeomsNodes, $maxMatchesPerCurve)
};

(:~
 : Checks if for each curve of the given $geomNode an identical curve (same control points - ignoring curve orientation) from the $otherGeomNodes exists.
 :
 : @param $geomNode GML geometry node
 : @param $otherGeomNodes one or more database nodes representing GML geometries
 : @return The empty sequence, if full coverage was determined, otherwise the JTS geometry of the first curve from $geomNode which is not covered by an identical curve from $otherGeomNodes
 :)
declare function geox:determineIncompleteCoverageByIdenticalCurveComponents($geomNode , $otherGeomNodes ) {
    java:determineIncompleteCoverageByIdenticalCurveComponents($geomNode, $otherGeomNodes)
};

(:~
 : Checks two geometries for interior intersection of curve components. If both geometries are point based, the result will be the empty sequence (since then there are no curves to check). Components of the first geometry are compared with the components of the second geometry (using a spatial index to prevent unnecessary checks): If two components are not equal (a situation that is allowed) then they are checked for an interior intersection, meaning that the interiors of the two components intersect (T********) or - only when curves are compared - that the boundary of one component intersects the interior of the other component (*T******* or ***T*****). If such a situation is detected, the intersection of the two components will be returned and testing will stop (meaning that the result will only provide information for one invalid intersection, not all intersections).
 :
 : @param $geomNode1 the node that represents the first geometry
 : @param $geomNode2 the node that represents the second geometry
 : @return The intersection of two components from the two geometries, where an invalid intersection was detected, or the empty sequence if no such case exists.
 :)
declare function geox:determineInteriorIntersectionOfCurveComponents($geomNode1 , $geomNode2 ) {
    java:determineInteriorIntersectionOfCurveComponents($geomNode1, $geomNode2)
};

(:~
 : Retrieve the geometry represented by a given node as a JTS geometry. First try the cache and if it is not in the cache construct it from the XML. 
 :
 : @param $geomNode XML element that represents the geometry
 : @return the JTS geometry of the node; can be an empty geometry if the node does not represent a geometry
 :)
declare function geox:getOrCacheGeometry($geomNode ) {
    java:getOrCacheGeometry($geomNode)
};

(:~
 : Prepares spatial indexing of a feature geometry, for the default spatial index.
 :
 : @param $node represents the node of the feature to be indexed
 : @param $geometry represents the GML geometry to index
 :)
declare function geox:prepareSpatialIndex($node , $geometry ) as empty-sequence() {
    java:prepareSpatialIndex($node, $geometry)
};

(:~
 : Prepares spatial indexing of a feature geometry, for the named spatial index.
 :
 : @param $indexName Identifies the index. The empty string identifies the default index.
 : @param $node represents the node of the feature to be indexed
 : @param $geometry represents the GML geometry to index
 :)
declare function geox:prepareSpatialIndex($indexName as xs:string, $node , $geometry ) as empty-sequence() {
    java:prepareSpatialIndex($indexName, $node, $geometry)
};

(:~
 : Prepares spatial indexing of the control points of $featuresToSearchIn, for the named spatial index "PointIndex".
 : Parses the control points of $featuresToSearchBy. The behavior for computing control points can be influenced by the
 : parameter controlPointSearchBehavior. For each control point, search the index "PointIndex" for a nearest point. If a
 : point within the a distance which is strictly smaller than 'maxDistance' is found, detailed information will be
 : returned as a result DOM element.
 : Points which are strictly smaller than 'minDistance' will not be detected as nearest points.
 : Identical points will also not be detected as nearest points.
 :
 : @param $featuresToSearchBy as a sequence of GML feature nodes.
 : @param $featuresToSearchByGeom as a sequence of the GML geometry nodes of $featuresToSearchBy.
 : @param $featuresToSearchIn as a sequence of GML feature nodes.
 : @param $featuresToSearchInGeom as a sequence of the GML geometry nodes of $featuresToSearchIn.
 : @param $controlPointSearchBehavior code(s) to influence the set of control points (multiple values separated by commas): 'IGNORE_ARC_MID_POINT' - to ignore the middle point of an arc (in Arc andArcString)
 : @param $minDistance as the minimum distance to search nearest points around the points of $featuresToSearchBy.
 : @param $maxDistance as the maximum distance to search nearest points around the points of $featuresToSearchBy.
 : @param $limitErrors as the maximum number of features with errors to report.
 : @param $tileLength as the coordinate length of each square tile, by which the input will be processed
 : @param $tileOverlap as the coordinate length, by which the tiles will overlap. In order to prevent errors at boundaries.
 :
 : @return A DOM element with detailed information of the computed results; the element can be empty (if the geometry
 :         does not contain any relevant points). The element has the following (exemplary) structure:
 : <geoxr:geoxResults xmlns:geoxr="https://modules.etf-validator.net/gmlgeox/result">
 :   <geoxr:result>
 :     <!-- The node of the gml feature which has a nearest point. -->
 :     <geoxr:featureWithNearest>
          <GmlFeature>...</GmlFeature>
       </geoxr:featureWithNearest>
 :     <!-- The point of featureWithNearest which has a nearest point. -->
 :     <geoxr:currentPointWithNearest>408331.407 5473380.787</geoxr:currentPointWithNearest>
 :     <!-- The node of the gml feature which is nearest to featureWithNearest. -->
 :     <geoxr:nearestFeature>
          <GmlFeature>...</GmlFeature>
       </geoxr:nearestFeature>
 :     <!-- The point of nearestFeature which is nearest to currentPointWithNearest. -->
 :     <geoxr:nearestPoint>408331.407 5473380.789</geoxr:nearestPoint>
 :     <!-- The distance between the currentPointWithNearest and the nearestPoint. -->
 :     <geoxr:distance>0.0020000003278255463</geoxr:distance>
 :   </geoxr:result>
 :   <geoxr:result>
 :   ..
 :   <geoxr:result>
 :   ..
 : </geoxr:geoxResults>
 :)
declare function geox:detectNearestPoints($featuresToSearchBy, $featuresToSearchByGeom, $featuresToSearchIn, $featuresToSearchInGeom, $controlPointSearchBehavior, $minDistance, $maxDistance, $limitErrors, $tileLength, $tileOverlap) {
    let $nearestPointsDetails := java:detectNearestPoints($featuresToSearchBy, $featuresToSearchByGeom, $featuresToSearchIn, $featuresToSearchInGeom, $controlPointSearchBehavior, $minDistance, $maxDistance, $limitErrors, $tileLength, $tileOverlap)

    (: Update the returned DOM-Element by replacing the "db:open-pre()" expression whith the evaluated xquery. :)
    let $replaced_results :=
    for $result in $nearestPointsDetails/*
      let $featureWithNearest := xquery:eval($result/*:featureWithNearest)
      let $nearestFeature := xquery:eval($result/*:nearestFeature)

      let $updated_featureWithNearest :=
      copy $c := $result
      modify replace node $c/*:featureWithNearest with <geoxr:featureWithNearest xmlns:geoxr="https://modules.etf-validator.net/gmlgeox/result">{$featureWithNearest}</geoxr:featureWithNearest>
      return $c
      let $updated_result :=
      copy $c := $updated_featureWithNearest
      modify replace node $c/*:nearestFeature with <geoxr:nearestFeature xmlns:geoxr="https://modules.etf-validator.net/gmlgeox/result">{$nearestFeature}</geoxr:nearestFeature>
      return $c
    return $updated_result

    let $nearestPointsDetails_replaced :=
    <geoxr:geoxResults xmlns:geoxr="https://modules.etf-validator.net/gmlgeox/result">
      {$replaced_results}
    </geoxr:geoxResults>

    return $nearestPointsDetails_replaced
};

(:~
 : Prepares spatial indexing of the control points of a feature geometry, for a spatial index with given name. 
 : The behavior for retrieving control points can be influenced by the parameter $controlPointSearchBehavior.
 :
 : @param $indexName Identifies the index.
 : @param $node represents the node of the feature to be indexed
 : @param $geometry represents the GML geometry to index
 : @param $controlPointSearchBehavior codes to influence to resulting set of control points (multiple values separated by commas): 'IGNORE_ARC_MID_POINT' - to ignore the middle point of an arc (in Arc and ArcString)
 :)
declare function geox:prepareSpatialPointIndex($indexName, $node, $geometry, $controlPointSearchBehavior) as empty-sequence() {
    java:prepareSpatialPointIndex($indexName, $node, $geometry, $controlPointSearchBehavior)
};

(:~
 : Prepares spatial indexing of a feature geometry, for the default and a named spatial index.
 :
 : @param $indexName Identifies the index. The empty string identifies the default index.
 : @param $node represents the node of the feature to be indexed
 : @param $geometry represents the GML geometry to index
 :)
declare function geox:prepareDefaultAndSpecificSpatialIndex($indexName as xs:string, $node , $geometry ) as empty-sequence() {
    java:prepareDefaultAndSpecificSpatialIndex($indexName, $node, $geometry)
};

(:~
 : Create the default spatial index using bulk loading.
 :
 : Uses the index entries that have been prepared using function(s) geox:prepareSpatialIndex(...).
 :)
declare function geox:buildSpatialIndex() as empty-sequence() {
    java:buildSpatialIndex()
};

(:~
 : Create the named spatial index using bulk loading.
 :
 : Uses the index entries that have been prepared using function(s) geox:prepareSpatialIndex(...).
 :
 : @param $indexName Identifies the index. The empty string identifies the default index.
 :)
declare function geox:buildSpatialIndex($indexName as xs:string) as empty-sequence() {
    java:buildSpatialIndex($indexName)
};

(:~
  : Identify all geometries contained in the given geometry, that have the given dimension. Note that Point and MultiPoint have dimension 0, LineString and MultiLineString have dimension 1, and Polygon and MultiPolygon have dimension 2.
 :
 : @param $geom the geometry - typically a collection - to investigate; must not be empty 
 : @param $dimension the dimension of geometries to return (value must be 0, 1, or 2)
 : @return the geometries with the specified dimension; can be empty
 :)
declare function geox:geometriesWithDimension($geom , $dimension as xs:int )  {
    java:geometriesWithDimension($geom, $dimension)
};

(:~
 : Retrieves the first two coordinates of a given geometry.
 :
 : @param $geom a JTS Geometry object
 : @return an empty sequence if the geometry is null or empty, otherwise a sequence with the x and y from the first coordinate of the geometry
 :)
declare function geox:georefFromGeom($geom ) as xs:string* {
    java:georefFromGeom($geom)
};

(:~
 : Retrieve x and y of the given coordinate, as strings without scientific notation.
 :
 : @param $coord a JTS Coordinate object
 : @return an array with the x and y of the given coordinate, as strings without scientific notation.
 :)
declare function geox:georefFromCoord($coord ) as xs:string* {
    java:georefFromCoord($coord)
};

(:~
 : @return Information about the version of the query module
 :)
declare function geox:detailedVersion() as xs:string {
    java:detailedVersion()
};

(:~
 : Returns the Java instance of the GmlGeoX query module.
 :
 : @return the query module instance
 :)
declare function geox:getModuleInstance() {
    java:getModuleInstance()
};

(:~
 : Computes the segment of the given (Multi)LineString that are in the interior of the given(Multi)Polygon.
 : 
 : @param $line a non-empty JTS LineString or MultiLineString
 : @param $polygon a non-empty JTS Polygon or MultiPolygon
 : @return the segments of the line that are in the interior of the polygon
 :)
declare function geox:interiorIntersectionsLinePolygon($line, $polygon) {
    java:interiorIntersectionsLinePolygon($line, $polygon)
};

(:
 : Compute the DE-9IM intersection matrix that describes how geom1 relates to geom2.
 :
 : @param geom1 the first (JTS) geometry
 : @param geom2 the second (JTS) geometry
 : @return the DE-9IM intersection matrix that describes how geom1 relates to geom2
 :)
declare function geox:intersectionMatrix($geom1, $geom2) {
    java:intersectionMatrix($geom1,$geom2)
};

(:
 : Identify points P in a 1D, 2D, or 3D geometry that are connected to their
 : neighbouring points N via linear curve segments, and compute the angle of
 : these lines as well as the distance of a point P to the imaginary line
 : between its two neighbouring points.
 :
 : @param geomNode the node that represents the geometry
 : @return An element with identified points
 :)
declare function geox:determineDetailsOfPointsBetweenLinearCurveSegments($geomNode) {
    java:determineDetailsOfPointsBetweenLinearCurveSegments($geomNode)
};

(:
 : Returns the length of the given geometry. For a linear geometry, its length is 
 : returned. For an areal geometry, the length of its perimeter is returned. For 
 : (multi-) point geometries, 0.0 is returned.
 :
 : @param jtsGeom the JTS geometry object
 : @return the length of the geometry
 :)
declare function geox:lengthGeom($jtsGeom) {
    java:lengthGeom($jtsGeom)
};

(:
 : Retrieves the JTS representations of all curve components contained in the geometry node.
 :
 : @param geomNode the geometry element
 : @return A sequence with the JTS representations of the curve components of the geometry (node); can be empty if the given geometry node does not contain any curve component.
 :)
declare function geox:curveComponents($geomNode) {
    java:curveComponents($geomNode)
};

