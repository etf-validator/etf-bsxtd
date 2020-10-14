import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $members := db:open($dbname)//ii:member
let $init := ggeo:init($dbname)

let $geometries := $members/*
return
 <test_curveEndPoints>    
   {
    for $geom in $geometries
    order by $geom/@gml:id
    let $endPoints := ggeo:curveEndPoints($geom)    
    let $result := string-join(($endPoints ! ggeo:toWKT(.)),', ')
    return 
      <test geom='{$geom/@gml:id}'>{$result}</test>    
   }
 </test_curveEndPoints>
