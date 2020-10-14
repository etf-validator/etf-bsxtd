import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $members := db:open($dbname)//ii:member
let $init := ggeo:init($dbname)

let $geom1 := ggeo:getOrCacheGeometry($members[@gml:id = 'Member_1']/*)
let $geom2 := ggeo:getOrCacheGeometry($members[@gml:id = 'Member_2']/*)
let $result := ggeo:equalsGeomGeom($geom1,$geom2)

return
 <test_arc_interpolation_compositeCurve>  
  <relationshiptest>
    <tests>
     <test>{$result}</test>     
     </tests>
  </relationshiptest>
 </test_arc_interpolation_compositeCurve>
