import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $members := db:open($dbname)//ii:member
let $init := ggeo:init($dbname)

let $geometries := $members/*

let $c1 := ggeo:getOrCacheGeometry($geometries[@gml:id = 'c1'])
let $c2 := ggeo:getOrCacheGeometry($geometries[@gml:id = 'c2'])
let $c3 := ggeo:getOrCacheGeometry($geometries[@gml:id = 'c3'])
let $c4 := ggeo:getOrCacheGeometry($geometries[@gml:id = 'c4'])
let $c5 := ggeo:getOrCacheGeometry($geometries[@gml:id = 'c5'])
let $s1 := ggeo:getOrCacheGeometry($geometries[@gml:id = 's1'])

let $c1result := ggeo:interiorIntersectionsLinePolygon($c1,$s1)
let $c2result := ggeo:interiorIntersectionsLinePolygon($c2,$s1)
let $c3result := ggeo:interiorIntersectionsLinePolygon($c3,$s1)
let $c4result := ggeo:interiorIntersectionsLinePolygon($c4,$s1)
let $c5result := ggeo:interiorIntersectionsLinePolygon($c5,$s1)

return
 <test_interiorIntersectionsLinePolygon>
  <test
   line="c1"
   polygon="s1">{if(empty($c1result)) then 'empty_sequence' else ggeo:toWKT($c1result)}</test>
   <test
   line="c2"
   polygon="s1">{if(empty($c2result)) then 'empty_sequence' else ggeo:toWKT($c2result)}</test>
   <test
   line="c3"
   polygon="s1">{if(empty($c3result)) then 'empty_sequence' else ggeo:toWKT($c3result)}</test>
   <test
   line="c4"
   polygon="s1">{if(empty($c4result)) then 'empty_sequence' else ggeo:toWKT($c4result)}</test>
   <test
   line="c5"
   polygon="s1">{if(empty($c5result)) then 'empty_sequence' else ggeo:toWKT($c5result)}</test> 
 </test_interiorIntersectionsLinePolygon>
