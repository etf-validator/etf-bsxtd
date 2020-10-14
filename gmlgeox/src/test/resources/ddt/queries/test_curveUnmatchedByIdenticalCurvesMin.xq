import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';

let $init := ggeo:init($dbname)
let $members := db:open($dbname)//ii:member

let $geometries := $members/*
let $otherGeoms := $geometries[starts-with(@gml:id,'s')]
  
let $testC1 := ggeo:curveUnmatchedByIdenticalCurvesMin($geometries[@gml:id = 'c1'], $otherGeoms, xs:int(1))
let $result_testC1 := if(empty($testC1)) then 'OK' else ggeo:toWKT($testC1)

let $testC2 := ggeo:curveUnmatchedByIdenticalCurvesMin($geometries[@gml:id = 'c2'], $otherGeoms, xs:int(1))
let $result_testC2 := if(empty($testC2)) then 'OK' else ggeo:toWKT($testC2)

let $testC3 := ggeo:curveUnmatchedByIdenticalCurvesMin($geometries[@gml:id = 'c3'], $otherGeoms, xs:int(1))
let $result_testC3 := if(empty($testC3)) then 'OK' else ggeo:toWKT($testC3)

let $testC4 := ggeo:curveUnmatchedByIdenticalCurvesMin($geometries[@gml:id = 'c4'], $otherGeoms, xs:int(1))
let $result_testC4 := if(empty($testC4)) then 'OK' else ggeo:toWKT($testC4)

let $testC5 := ggeo:curveUnmatchedByIdenticalCurvesMin($geometries[@gml:id = 'c5'], $otherGeoms, xs:int(1))
let $result_testC5 := if(empty($testC5)) then 'OK' else ggeo:toWKT($testC5)

return
 <test_curveUnmatchedByIdenticalCurvesMin>
  <test geom="c1">{$result_testC1}</test>
  <test geom="c2">{$result_testC2}</test>
  <test geom="c3">{$result_testC3}</test>
  <test geom="c4">{$result_testC4}</test>
  <test geom="c5">{$result_testC5}</test>
 </test_curveUnmatchedByIdenticalCurvesMin>
