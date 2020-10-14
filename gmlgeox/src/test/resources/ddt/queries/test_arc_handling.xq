import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.example.org/test/arc';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';

let $doc := fn:doc($docPath)
let $geometries := $doc//ii:geom/*
let $geom1 := $geometries[@gml:id = 'ARC1']
let $geom2 := $geometries[@gml:id = 'ARC2']
let $geom3 := $geometries[@gml:id = 'LS']
let $init := ggeo:init($dbname)
let $ggeogeom1 := ggeo:parseGeometry($geom1)
let $ggeogeom2 := ggeo:parseGeometry($geom2)
let $ggeogeom3 := ggeo:parseGeometry($geom3)
let $ggeogeom1wkt := ggeo:toWKT($ggeogeom1)
let $ggeogeom2wkt := ggeo:toWKT($ggeogeom2)
let $ggeogeom3wkt := ggeo:toWKT($ggeogeom3)
return
 <testresults>
 <loadtest>
 <wkt geom='{$geom1/@gml:id}'>{$ggeogeom1wkt}</wkt>
 <wkt geom='{$geom2/@gml:id}'>{$ggeogeom2wkt}</wkt>
 <wkt geom='{$geom3/@gml:id}'>{$ggeogeom3wkt}</wkt>
 </loadtest>
  <validationtest>
   {
    for $geom in $geometries
    let $vr := ggeo:validateAndReport($geom)
    return
     <test>
      <valid>{
        if (xs:boolean($vr/ggeo:valid)) then
         'true'
        else
         'false'
       }</valid>
      <result>{
        $vr/ggeo:result/text()
       }</result>
      <messages>
       {
        for $msg in $vr/ggeo:message
        return
         <message>
          <type>{
            $msg/data(@type)
           }</type>
          <text>{
            $msg/text()
           }</text>
         </message>
       }
      </messages>
     </test>
   }
  </validationtest>

  <relationshiptest>
    <test
     geom1='{$geom1/@gml:id}'
     geom2='{$geom2/@gml:id}'>
     <intersects>
      {ggeo:intersects($ggeogeom1, $ggeogeom2)}
     </intersects>
     <equals>
      {ggeo:equals($ggeogeom1, $ggeogeom2)}
     </equals>
    </test>
    <test
     geom1='{$geom3/@gml:id}'
     geom2='{$geom1/@gml:id}'>
     <intersects>
      {ggeo:intersects($ggeogeom3, $ggeogeom1)}
     </intersects>
    </test>
  </relationshiptest>
 </testresults>
