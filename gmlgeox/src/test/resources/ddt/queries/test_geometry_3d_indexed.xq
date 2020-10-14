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
    return
     try {
      let $vr := ggeo:validateAndReport($geom, '110')
      return
       if (xs:boolean($vr/ggeo:isValid)) then
        ()
       else
        for $message in $vr/ggeo:errors/*:message/*:argument[@*:token='original']
        return $message/text()
     } catch * {
      $err:description
     }
   }
  </validationtest>
  <geoIndexTest>
   {
    let $geometryParsingErrors :=
    map:merge(for $member in $members
    let $geom := ($member//*[self::gml:Point or self::gml:LineString or self::gml:Curve or self::gml:Polygon or self::gml:PolyhedralSurface or self::gml:Surface or self::gml:MultiPoint or self::gml:MultiCurve or self::gml:MultiLineString or self::gml:MultiSurface or self::gml:MultiPolygon or self::gml:MultiGeometry])[1]
    return
     if ($geom) then
      try {
       prof:void(ggeo:index($member, $geom))
      } catch * {
       map:entry($member/@gml:id, $err:description)
      }
     else
      ())
    
    return
     if (map:size($geometryParsingErrors) = 0) then
      'PASSED'
     else
      map:for-each($geometryParsingErrors, function ($a, $b) {
       'gmlid: ' || $a || ' - ' || $b
      })
   
   }
  </geoIndexTest>
 </test_3d>
