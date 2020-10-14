import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $init := ggeo:init($dbname)

let $members := db:open($dbname)//ii:member
let $geometries := $members/*
let $otherGeoms := $geometries[starts-with(@gml:id,'s')]
  
let $testC1 := ggeo:curveUnmatchedByIdenticalCurvesMax($geometries[@gml:id = 'c1'], $otherGeoms, xs:int(1))
let $result_testC1 := if(empty($testC1)) then 'OK' else ggeo:toWKT($testC1)

let $testC2 := ggeo:curveUnmatchedByIdenticalCurvesMax($geometries[@gml:id = 'c2'], $otherGeoms, xs:int(1))
let $result_testC2 := if(empty($testC2)) then 'OK' else ggeo:toWKT($testC2)

let $testC3 := ggeo:curveUnmatchedByIdenticalCurvesMax($geometries[@gml:id = 'c3'], $otherGeoms, xs:int(1))
let $result_testC3 := if(empty($testC3)) then 'OK' else ggeo:toWKT($testC3)

return
 <test_curveUnmatchedByIdenticalCurvesMax>
  <test geom="c1">{$result_testC1}</test>
  <test geom="c2">{$result_testC2}</test>
  <test geom="c3">{$result_testC3}</test>
 </test_curveUnmatchedByIdenticalCurvesMax>
