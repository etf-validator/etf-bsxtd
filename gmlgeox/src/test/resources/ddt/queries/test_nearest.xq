import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $membersAll := db:open($dbname)//ii:member
let $init := ggeo:init($dbname)

(: First, build default index :)
let $geometryParsingErrorsMap :=
  map:merge(for $member in $membersAll
    let $geometryNode := $member/*/*:position/*
    return
    if ($geometryNode) then
      try {
        prof:void(ggeo:prepareSpatialIndex($member,$geometryNode))
      } catch * {
        map:entry($member/@gml:id,($member,$err:description))
      }
    else ())
let $dummyBuildDefaultSpatialIndex :=
  if((count($membersAll/*) - map:size($geometryParsingErrorsMap)) > 0) then
    prof:void(ggeo:buildSpatialIndex())
  else
    ()

let $maxDistance := xs:double(0.010)
let $minDistance := xs:double(0.003)
let $featuresToSearchBy := $membersAll/*
let $featuresToSearchIn := $featuresToSearchBy
let $limitErrors := 1000

(: Test min-max boundaries :)
let $nearestPointsDetails_minMax := ggeo:detectNearestPoints($featuresToSearchBy, $featuresToSearchBy/*:position/*, $featuresToSearchIn, $featuresToSearchIn/*:position/*, 'IGNORE_ARC_MID_POINT', $minDistance, $maxDistance, xs:int($limitErrors), xs:double(20000), $maxDistance)

(: Test max boundaries :)
let $maxDistance := xs:double(0.003)
let $minDistance := xs:double(0.0)
let $nearestPointsDetails_max := ggeo:detectNearestPoints($featuresToSearchBy, $featuresToSearchBy/*:position/*, $featuresToSearchIn, $featuresToSearchIn/*:position/*, 'IGNORE_ARC_MID_POINT', $minDistance, $maxDistance, xs:int($limitErrors), xs:double(20000), $maxDistance)

(: Test LimitErrors :)
let $limitErrors := 1
let $nearestPointsDetails_limitErrors := ggeo:detectNearestPoints($featuresToSearchBy, $featuresToSearchBy/*:position/*, $featuresToSearchIn, $featuresToSearchIn/*:position/*, 'IGNORE_ARC_MID_POINT', $minDistance, $maxDistance, xs:int($limitErrors), xs:double(20000), $maxDistance)
let $limitErrors := 1000

(: Test Pass :)
let $maxDistance := xs:double(0.001)
let $minDistance := xs:double(0.0)
let $nearestPointsDetails_pass := ggeo:detectNearestPoints($featuresToSearchBy, $featuresToSearchBy/*:position/*, $featuresToSearchIn, $featuresToSearchIn/*:position/*, 'IGNORE_ARC_MID_POINT', $minDistance, $maxDistance, xs:int($limitErrors), xs:double(20000), $maxDistance)

return
 <test_nearest>
  <max>
    {$nearestPointsDetails_max}
  </max>
  <minMax>
    {$nearestPointsDetails_minMax}
  </minMax>
  <pass>
    {$nearestPointsDetails_pass}
  </pass>
  <limitErrors>
    {$nearestPointsDetails_limitErrors}
  </limitErrors>
 </test_nearest>
