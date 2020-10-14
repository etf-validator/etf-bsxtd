import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $init := ggeo:init($dbname)
let $geometries := db:open($dbname)//ii:member/*

return
 <validationtest>
  <isClosed_forCurvesOnly>
  {
   for $geom in $geometries
    return
     <test gmlid="{$geom/@gml:id}">
      <isClosed>{
        if (xs:boolean(ggeo:isClosed($geom))) then
         'true'
        else
         'false'
       }</isClosed> 
     </test>
  }
  </isClosed_forCurvesOnly>
  <isClosed_forAllGeometryTypes>
  {
   for $geom in $geometries
    return
     <test gmlid="{$geom/@gml:id}">
      <isClosed>{
        if (xs:boolean(ggeo:isClosed($geom,false()))) then
         'true'
        else
         'false'
       }</isClosed> 
     </test>
  }
  </isClosed_forAllGeometryTypes>
 </validationtest>
