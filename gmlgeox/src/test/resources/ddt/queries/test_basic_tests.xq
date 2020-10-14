import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $db := db:open($dbname)
let $init := ggeo:init($dbname)

let $points := $db//gml:Point
let $point1 := $points[@gml:id = 'p1']
let $point2 := $points[@gml:id = 'p2']
let $point3 := $points[@gml:id = 'p3']
let $point4 := $points[@gml:id = 'p4']
let $point5 := $points[@gml:id = 'p5']
let $curves := $db//gml:Curve
let $curve1 := $curves[@gml:id = 'c1']
let $curve2 := $curves[@gml:id = 'c2']
let $curve3 := $curves[@gml:id = 'c3']
let $curve4 := $curves[@gml:id = 'c4']
let $curve5 := $curves[@gml:id = 'c5']
let $curve6 := $curves[@gml:id = 'c6']
let $curve7 := $curves[@gml:id = 'c7']
let $curve8 := $curves[@gml:id = 'c8']
let $curve9 := $curves[@gml:id = 'c9']
let $curve10 := $curves[@gml:id = 'c10']
let $curve11 := $curves[@gml:id = 'c11']
let $curve12 := $curves[@gml:id = 'c12']
let $curve13 := $curves[@gml:id = 'c13']
let $curve14 := $curves[@gml:id = 'c14']
let $curve15 := $curves[@gml:id = 'c15']
let $surfaces := $db//gml:Surface
let $surface1 := $surfaces[@gml:id = 's1']
let $surface2 := $surfaces[@gml:id = 's2']
let $surface3 := $surfaces[@gml:id = 's3']
let $surface4 := $surfaces[@gml:id = 's4']
let $surface5 := $surfaces[@gml:id = 's5']
let $surface6 := $surfaces[@gml:id = 's6']
let $surface7 := $surfaces[@gml:id = 's7']
let $surface8 := $surfaces[@gml:id = 's8']
let $surface9 := $surfaces[@gml:id = 's9']
let $surface10 := $surfaces[@gml:id = 's10']
let $surface11 := $surfaces[@gml:id = 's11']
let $surface12 := $surfaces[@gml:id = 's12']
let $surface13 := $surfaces[@gml:id = 's13']
let $surface14 := $surfaces[@gml:id = 's14']
let $surface16 := $surfaces[@gml:id = 's16']

(: Point/Point :)
(: (true false false false true false true false false true true false false false false false) :)
let $test1 :=
(ggeo:contains($point1, $point2), (: true :)
ggeo:contains($point1, $point3), (: false :)
ggeo:crosses($point1, $point2), (: false :)
ggeo:crosses($point1, $point3), (: false :)
ggeo:equals($point1, $point2), (: true :)
ggeo:equals($point1, $point3), (: false :)
ggeo:intersects($point1, $point2), (: true :)
ggeo:intersects($point1, $point3), (: false :)
ggeo:isDisjoint($point1, $point2), (: false :)
ggeo:isDisjoint($point1, $point3), (: true :)
ggeo:isWithin($point1, $point2), (: true :)
ggeo:isWithin($point1, $point3), (: false :)
ggeo:overlaps($point1, $point2), (: false :)
ggeo:overlaps($point1, $point3), (: false :)
ggeo:touches($point1, $point2), (: false :)
ggeo:touches($point1, $point3)) (: false :)

(: Point/Curve :)
(: (false false true false false false false false false false false true true true true true true false false false false false false true false false false false false false false false false false false false false true true) :)
let $test2 := (
ggeo:contains($point1, $curve1), (: false :)
ggeo:contains($curve1, $point1), (: false :)
ggeo:contains($curve1, $point4), (: true :)
ggeo:contains($curve1, $point3), (: false :)
ggeo:crosses($point1, $curve1), (: false :)
ggeo:crosses($curve1, $point1), (: false :)
ggeo:crosses($point4, $curve1), (: false :)
ggeo:crosses($curve1, $point4), (: false :)
ggeo:equals($point1, $curve1), (: false :)
ggeo:intersects($point3, $curve1), (: false :)
ggeo:intersects($curve1, $point3), (: false :)
ggeo:intersects($point4, $curve1), (: true :)
ggeo:intersects($curve1, $point4), (: true :)
ggeo:intersects($point1, $curve1), (: true :)
ggeo:intersects($curve1, $point1), (: true :)
ggeo:isDisjoint($point3, $curve1), (: true :)
ggeo:isDisjoint($curve1, $point3), (: true :)
ggeo:isDisjoint($point4, $curve1), (: false :)
ggeo:isDisjoint($curve1, $point4), (: false :)
ggeo:isDisjoint($point1, $curve1), (: false :)
ggeo:isDisjoint($curve1, $point1), (: false :)
ggeo:isWithin($point3, $curve1), (: false :)
ggeo:isWithin($curve1, $point3), (: false :)
ggeo:isWithin($point4, $curve1), (: true :)
ggeo:isWithin($curve1, $point4), (: false :)
ggeo:isWithin($point1, $curve1), (: false :)
ggeo:isWithin($curve1, $point1), (: false :)
ggeo:overlaps($point3, $curve1), (: false :)
ggeo:overlaps($curve1, $point3), (: false :)
ggeo:overlaps($point4, $curve1), (: false :)
ggeo:overlaps($curve1, $point4), (: false :)
ggeo:overlaps($point1, $curve1), (: false :)
ggeo:overlaps($curve1, $point1), (: false :)
ggeo:touches($point3, $curve1), (: false :)
ggeo:touches($curve1, $point3), (: false :)
ggeo:touches($point4, $curve1), (: false :)
ggeo:touches($curve1, $point4), (: false :)
ggeo:touches($point1, $curve1), (: true :)
ggeo:touches($curve1, $point1)) (: true :)

(: Point/Surface :)
(: (false false false true false false false false false false false true true true true true true false false false false false false true false false false false false false false false false false false false false true true) :)
let $test3 := (
ggeo:contains($point1, $surface1), (: false :)
ggeo:contains($surface1, $point1), (: false :)
ggeo:contains($surface1, $point4), (: false :)
ggeo:contains($surface1, $point5), (: true :)
ggeo:crosses($point1, $surface1), (: false :)
ggeo:crosses($surface1, $point1), (: false :)
ggeo:crosses($point5, $surface1), (: false :)
ggeo:crosses($surface1, $point5), (: false :)
ggeo:equals($point1, $surface1), (: false :)
ggeo:intersects($point4, $surface1), (: false :)
ggeo:intersects($surface1, $point4), (: false :)
ggeo:intersects($point5, $surface1), (: true :)
ggeo:intersects($surface1, $point5), (: true :)
ggeo:intersects($point1, $surface1), (: true :)
ggeo:intersects($surface1, $point1), (: true :)
ggeo:isDisjoint($point4, $surface1), (: true :)
ggeo:isDisjoint($surface1, $point4), (: true :)
ggeo:isDisjoint($point5, $surface1), (: false :)
ggeo:isDisjoint($surface1, $point5), (: false :)
ggeo:isDisjoint($point1, $surface1), (: false :)
ggeo:isDisjoint($surface1, $point1), (: false :)
ggeo:isWithin($point3, $surface1), (: false :)
ggeo:isWithin($surface1, $point3), (: false :)
ggeo:isWithin($point5, $surface1), (: true :)
ggeo:isWithin($surface1, $point5), (: false :)
ggeo:isWithin($point4, $surface1), (: false :)
ggeo:isWithin($surface1, $point4), (: false :)
ggeo:overlaps($point4, $surface1), (: false :)
ggeo:overlaps($surface1, $point4), (: false :)
ggeo:overlaps($point5, $surface1), (: false :)
ggeo:overlaps($surface1, $point5), (: false :)
ggeo:overlaps($point1, $surface1), (: false :)
ggeo:overlaps($surface1, $point1), (: false :)
ggeo:touches($point4, $surface1), (: false :)
ggeo:touches($surface1, $point4), (: false :)
ggeo:touches($point5, $surface1), (: false :)
ggeo:touches($surface1, $point5), (: false :)
ggeo:touches($point1, $surface1), (: true :)
ggeo:touches($surface1, $point1)) (: true :)

(: Curve/Curve :)
(: (true true true false false false false false true false true false false true true true true true false false false false false false true true false false false false true false false false false false true) :)
let $test4 := (
ggeo:contains($curve1, $curve1), (: true :)
ggeo:contains($curve2, $curve3), (: true :)
ggeo:contains($curve2, $curve7), (: true :)
ggeo:contains($curve2, $curve6), (: false :)
ggeo:crosses($curve1, $curve2), (: false :)
ggeo:crosses($curve2, $curve4), (: false :)
ggeo:crosses($curve2, $curve7), (: false :)
ggeo:crosses($curve2, $curve5), (: false :)
ggeo:crosses($curve4, $curve6), (: true :)
ggeo:equals($curve1, $curve2), (: false :)
ggeo:equals($curve2, $curve3), (: true :)
ggeo:equals($curve2, $curve5), (: false :)
ggeo:intersects($curve1, $curve2), (: false :)
ggeo:intersects($curve2, $curve7), (: true :)
ggeo:intersects($curve4, $curve6), (: true :)
ggeo:intersects($curve2, $curve5), (: true :)
ggeo:intersects($curve2, $curve4), (: true :)
ggeo:isDisjoint($curve1, $curve2), (: true :)
ggeo:isDisjoint($curve2, $curve7), (: false :)
ggeo:isDisjoint($curve4, $curve6), (: false :)
ggeo:isDisjoint($curve2, $curve5), (: false :)
ggeo:isDisjoint($curve2, $curve4), (: false :)
ggeo:isWithin($curve2, $curve1), (: false :)
ggeo:isWithin($curve4, $curve6), (: false :)
ggeo:isWithin($curve7, $curve2), (: true :)
ggeo:isWithin($curve2, $curve3), (: true :)
ggeo:isWithin($curve2, $curve5), (: false :)
ggeo:overlaps($curve2, $curve1), (: false :)
ggeo:overlaps($curve2, $curve4), (: false :)
ggeo:overlaps($curve2, $curve7), (: false :)
ggeo:overlaps($curve2, $curve5), (: true :)
ggeo:overlaps($curve6, $curve4), (: false :)
ggeo:touches($curve2, $curve1), (: false :)
ggeo:touches($curve2, $curve7), (: false :)
ggeo:touches($curve2, $curve5), (: false :)
ggeo:touches($curve4, $curve6), (: false :)
ggeo:touches($curve2, $curve4)) (: true :)

(: Curve/Surface :)
(: (false false true false false false true true false false false true true true true true false false false false false false false false true false false false false false false false false false false false true true true true) :)
let $test5 := (
ggeo:contains($curve1, $surface2), (: false :)
ggeo:contains($surface2, $curve1), (: false :)
ggeo:contains($surface2, $curve12), (: true :)
ggeo:contains($surface2, $curve11), (: false :)
ggeo:crosses($curve8, $surface2), (: false :)
ggeo:crosses($surface2, $curve8), (: false :)
ggeo:crosses($curve9, $surface2), (: true :)
ggeo:crosses($curve13, $surface2), (: true :)
ggeo:crosses($curve10, $surface2), (: false :)
ggeo:equals($curve1, $surface1), (: false :)
ggeo:intersects($curve1, $surface2), (: false :)
ggeo:intersects($surface2, $curve8), (: true :)
ggeo:intersects($curve10, $surface2), (: true :)
ggeo:intersects($surface2, $curve9), (: true :)
ggeo:intersects($curve12, $surface2), (: true :)
ggeo:isDisjoint($curve1, $surface2), (: true :)
ggeo:isDisjoint($surface2, $curve8), (: false :)
ggeo:isDisjoint($curve10, $surface2), (: false :)
ggeo:isDisjoint($surface2, $curve9), (: false :)
ggeo:isDisjoint($curve12, $surface2), (: false :)
ggeo:isWithin($curve1, $surface2), (: false :)
ggeo:isWithin($surface2, $curve1), (: false :)
ggeo:isWithin($curve9, $surface2), (: false :)
ggeo:isWithin($surface2, $curve9), (: false :)
ggeo:isWithin($curve12, $surface2), (: true :)
ggeo:isWithin($surface2, $curve12), (: false :)
ggeo:isWithin($curve11, $surface2), (: false :)
ggeo:isWithin($surface2, $curve11), (: false :)
ggeo:overlaps($curve1, $surface2), (: false :)
ggeo:overlaps($surface2, $curve1), (: false :)
ggeo:overlaps($curve9, $surface2), (: false :)
ggeo:overlaps($surface2, $curve9), (: false :)
ggeo:overlaps($curve12, $surface2), (: false :)
ggeo:overlaps($surface2, $curve12), (: false :)
ggeo:touches($curve1, $surface2), (: false :)
ggeo:touches($surface2, $curve1), (: false :)
ggeo:touches($curve8, $surface2), (: true :)
ggeo:touches($surface2, $curve8), (: true :)
ggeo:touches($curve10, $surface2), (: true :)
ggeo:touches($surface2, $curve10)) (: true :)

(: Surface/Surface :)
(: (false false false true false true false false false false false false false false true false true true true false false false false true false false false true false true false) :)
let $test6 := (
ggeo:contains($surface1, $surface2), (: false :)
ggeo:contains($surface2, $surface1), (: false :)
ggeo:contains($surface3, $surface4), (: false :)
ggeo:contains($surface4, $surface3), (: true :)
ggeo:contains($surface7, $surface4), (: false :)
ggeo:contains($surface4, $surface7), (: true :)
ggeo:contains($surface5, $surface4), (: false :)
ggeo:contains($surface4, $surface5), (: false :)
ggeo:crosses($surface1, $surface2), (: false :)
ggeo:crosses($surface2, $surface1), (: false :)
ggeo:crosses($surface7, $surface4), (: false :)
ggeo:crosses($surface4, $surface7), (: false :)
ggeo:crosses($surface5, $surface4), (: false :)
ggeo:equals($surface4, $surface5), (: false :)
ggeo:equals($surface5, $surface6), (: true :)
ggeo:intersects($surface1, $surface2), (: false :)
ggeo:intersects($surface7, $surface3), (: true :)
ggeo:intersects($surface5, $surface4), (: true :)
ggeo:isDisjoint($surface1, $surface2), (: true :)
ggeo:isDisjoint($surface7, $surface3), (: false :)
ggeo:isDisjoint($surface5, $surface4), (: false :)
ggeo:isWithin($surface1, $surface2), (: false :)
ggeo:isWithin($surface4, $surface3), (: false :)
ggeo:isWithin($surface3, $surface4), (: true :)
ggeo:overlaps($surface1, $surface2), (: false :)
ggeo:overlaps($surface3, $surface7), (: false :)
ggeo:overlaps($surface5, $surface6), (: false :)
ggeo:overlaps($surface4, $surface5), (: true :)
ggeo:touches($surface1, $surface2), (: false :)
ggeo:touches($surface3, $surface7), (: true :)
ggeo:touches($surface5, $surface4) (: false :)
)

(: Point / multiple individual points :)
(: (false true) :)

let $multiplePoints := ($point1, $point2, $point3)
let $test7 := (
ggeo:intersects($point1, $multiplePoints, true()), (: false :)
ggeo:intersects($point1, $multiplePoints, false()) (: true :)
)

(: Point / multiple individual points :)
(: (false true) :)

let $multiplePoints2 := ($point3, $point2, $point1)
let $test8 := (
ggeo:intersects($point1, $multiplePoints2, true()), (: false :)
ggeo:intersects($point1, $multiplePoints2, false()) (: true :)
)

(: Validation - all tests :)
(: ("VVVV", "VVVV", "VVVV") :)
let $test9 := (
ggeo:validate($surface4), (: VVVV :)
ggeo:validate($curve1), (: VVVV :)
ggeo:validate($point1) (: VVVV :)
)

(: Validation - ring orientation :)
(: ("FSSS", "FSSS") :)
let $test10 := (
ggeo:validate($surface8, '1000'), (: FSSS - exterior ring is oriented clockwise :)
ggeo:validate($surface9, '1000') (: FSSS - interior ring is oriented counter-clockwise :)
)

(: Validation - repetition :)
(: ("VVFV", "VVFV", "VVVV") :)
let $test11 := (
ggeo:validate($surface10), (: VVFV - doppelte position :)
ggeo:validate($curve14), (: VVFV - doppelte position :)
ggeo:validate($curve15) (: VVVV :)
)

(: Validation - connectedness :)
(: ("VFVV") :)
let $test12 := (
ggeo:validate($surface11) (: VFVV - fourth patch is not connected :)
)

(: Validation - connectedness :)
(: ("VFVV") :)
let $test13 := (
ggeo:validate($surface12) (: VFVV - the two patches only touch in a single point and are therefore not connected :)
)

(: Validation - connectedness :)
(: ("VVVV") :)
let $test14 := (
ggeo:validate($surface13) (: VVVV - same surface as s11 but only using the first three patches, which are connected :)
)

(: Validation - connectedness :)
(: ("VFVV") :)
let $test15 := (
ggeo:validate($surface14) (: VFVV - patch 1 is connected to patch 2, patch 3 is connected to patch 4, but patches 1/2 are not connected to patches 3/4 :)
)

(: Map use :)
(: ("MULTIPOINT ((0 0), (1 1))") :)
let $multiplePoints3 := ($point3, $point2, $point1)
let $geometryMap := map:merge(
for $x in $multiplePoints3
return
 map {$x/@gml:id: ggeo:parseGeometry($x)}
)
let $tnunion := ggeo:union(for-each($multiplePoints3/@gml:id, $geometryMap))
let $test16 := string($tnunion)

(: Basic test :)
(: (true true true true true) :)

let $geom := $db/*/*/*
let $dummy := for $g in $geom
return
 ggeo:index($g, $g)
let $test17 := (
count(ggeo:search(4, 2.4, 8, 8.5)),
count(ggeo:search(0, 0, 1, 1)),
contains(ggeo:search(0, 0, 1, 1)[@gml:id = 'p1']/gml:pos[1], '1 1'),
ggeo:isWithin($geom[@gml:id = 'c1'], $geom, false()),
number(ggeo:envelope($geom[1])[1])
)

let $test18 := (
    ggeo:validate($surface16) (: FVVV - exterior and interior rings intersect and split polygon into two parts :)
)

return
 <validationtest>
  <test1>{$test1}</test1>
  <test2>{$test2}</test2>
  <test3>{$test3}</test3>
  <test4>{$test4}</test4>
  <test5>{$test5}</test5>
  <test6>{$test6}</test6>
  <test7>{$test7}</test7>
  <test8>{$test8}</test8>
  <test9>{$test9}</test9>
  <test10>{$test10}</test10>
  <test11>{$test11}</test11>
  <test12>{$test12}</test12>
  <test13>{$test13}</test13>
  <test14>{$test14}</test14>
  <test15>{$test15}</test15>
  <test16>{$test16}</test16>
  <test17>{$test17}</test17>
  <test18>{$test18}</test18>
 </validationtest>
