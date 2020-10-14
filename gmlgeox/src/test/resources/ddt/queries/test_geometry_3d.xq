import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $members := db:open($dbname)//ii:member
let $init := ggeo:init($dbname)

let $geometries := $members/*
return
 <test_3d>
  <validationtest>
   {
    for $geom in $geometries
    let $vr := ggeo:validateAndReport($geom)
    return
     <test
      id='{$geom/@gml:id}'>
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
               for $message in $vr/ggeo:errors/*:message/*:argument[@token='original']
               return $message/text()
       }
      </messages>
     </test>
   }
  </validationtest>
  <relationshiptest>
   {
    let $geom1 := $geometries[@gml:id = 'Point_1']
    let $geom2 := $geometries[@gml:id = 'Curve_1']
    let $ggeogeom1 := ggeo:getOrCacheGeometry($geometries[@gml:id = 'Point_1'])
    let $ggeogeom2 := ggeo:getOrCacheGeometry($geometries[@gml:id = 'Curve_1'])
    return
    <tests>
     <test_nodenode
      geom1='{$geom1/@gml:id}'
      geom2='{$geom2/@gml:id}'>
      <intersects>
       {ggeo:intersects($geom1, $geom2)}
      </intersects>
     </test_nodenode>
     <test_geomgeom
      geom1='{$geom1/@gml:id}'
      geom2='{$geom2/@gml:id}'>
      <intersects>
       {ggeo:intersectsGeomGeom($ggeogeom1, $ggeogeom2)}
      </intersects>
     </test_geomgeom>
     </tests>
   }
  </relationshiptest>
  <relationshiptest>
   {
    let $geom1 := $geometries[@gml:id = 'Point_2']
    let $geom2 := $geometries//*[@gml:id = 'Curve_S1']
    let $ggeogeom1 := ggeo:getOrCacheGeometry($geometries[@gml:id = 'Point_2'])
    let $ggeogeom2 := ggeo:getOrCacheGeometry($geometries//*[@gml:id = 'Curve_S1'])
    return
    <tests>
     <test_nodenode
      geom1='{$geom1/@gml:id}'
      geom2='{$geom2/@gml:id}'>
      <intersects>
       {ggeo:intersects($geom1, $geom2)}
      </intersects>
     </test_nodenode>
     <test_geomgeom
      geom1='{$geom1/@gml:id}'
      geom2='{$geom2/@gml:id}'>
      <intersects>
       {ggeo:intersectsGeomGeom($ggeogeom1, $ggeogeom2)}
      </intersects>
     </test_geomgeom>
     </tests>     
   }
  </relationshiptest>
 </test_3d>
