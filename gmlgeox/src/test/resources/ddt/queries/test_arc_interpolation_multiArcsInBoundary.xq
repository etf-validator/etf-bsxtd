import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';

let $members := db:open($dbname)//ii:member

let $candidate1_geom := $members[@gml:id = 'BF_DENW36AL1Nv000MT']/*
let $gelaendekante1_1_geom := $members[@gml:id = 'GK_DENW36AL1Nv000MV']/*
let $gelaendekante1_2_geom := $members[@gml:id = 'GK_DENW36AL1Nv000MU']/*

let $candidate2_geom := $members[@gml:id = 'BF_DENW36AL1Ku000Et']/*
let $gelaendekante2_1_geom := $members[@gml:id = 'GK_DENW36AL1Ku000Ep']/*

let $candidate3_geom := $members[@gml:id = 'BF_DENW36AL1Pa000Nw']/*
let $gelaendekante3_1_geom := $members[@gml:id = 'GK_DENW36AL1Pa000O9']/*
let $gelaendekante3_2_geom := $members[@gml:id = 'GK_DENW36AL1Pa000OC']/*
let $gelaendekante3_3_geom := $members[@gml:id = 'GK_DENW36AL1Pa000OA']/*
let $gelaendekante3_4_geom := $members[@gml:id = 'GK_DENW36AL1Pa000OB']/*

let $init := ggeo:init($dbname)

let $c1_geom := ggeo:getOrCacheGeometry($candidate1_geom)
let $gk1_1_geom := ggeo:getOrCacheGeometry($gelaendekante1_1_geom)
let $gk1_2_geom := ggeo:getOrCacheGeometry($gelaendekante1_2_geom)

let $c2_geom := ggeo:getOrCacheGeometry($candidate2_geom)
let $gk2_1_geom := ggeo:getOrCacheGeometry($gelaendekante2_1_geom)

let $c3_geom := ggeo:getOrCacheGeometry($candidate3_geom)
let $gk3_1_geom := ggeo:getOrCacheGeometry($gelaendekante3_1_geom)
let $gk3_2_geom := ggeo:getOrCacheGeometry($gelaendekante3_2_geom)
let $gk3_3_geom := ggeo:getOrCacheGeometry($gelaendekante3_3_geom)
let $gk3_4_geom := ggeo:getOrCacheGeometry($gelaendekante3_4_geom)

let $test_gk1_1InBoundary := ggeo:relateGeomGeom($gk1_1_geom,$c1_geom,'FTFFTF***')
let $test_gk1_2InBoundary := ggeo:relateGeomGeom($gk1_2_geom,$c1_geom,'FTFFTF***')

let $test_gk2_1InBoundary := ggeo:relateGeomGeom($gk2_1_geom,$c2_geom,'FTFFTF***')

let $test_gk3_1InBoundary := ggeo:relateGeomGeom($gk3_1_geom,$c3_geom,'FTFFTF***')
let $test_gk3_2InBoundary := ggeo:relateGeomGeom($gk3_2_geom,$c3_geom,'FTFFTF***')
let $test_gk3_3InBoundary := ggeo:relateGeomGeom($gk3_3_geom,$c3_geom,'FTFFTF***')
let $test_gk3_4InBoundary := ggeo:relateGeomGeom($gk3_4_geom,$c3_geom,'FTFFTF***')

return
 <test_arc_interpolation_multiArcsInBoundary>  
  <relationshiptest>
    <tests>
     <test_gk_in_boundary candidate="BF_DENW36AL1Nv000MT" gk="GK_DENW36AL1Nv000MV">{$test_gk1_1InBoundary}</test_gk_in_boundary>
     <test_gk_in_boundary candidate="BF_DENW36AL1Nv000MT" gk="GK_DENW36AL1Nv000MU">{$test_gk1_2InBoundary}</test_gk_in_boundary>
     
     <test_gk_in_boundary candidate="BF_DENW36AL1Ku000Et" gk="GK_DENW36AL1Ku000Ep">{$test_gk2_1InBoundary}</test_gk_in_boundary>
     
     <test_gk_in_boundary candidate="BF_DENW36AL1Pa000Nw" gk="GK_DENW36AL1Pa000O9">{$test_gk3_1InBoundary}</test_gk_in_boundary>
     <test_gk_in_boundary candidate="BF_DENW36AL1Pa000Nw" gk="GK_DENW36AL1Pa000OC">{$test_gk3_2InBoundary}</test_gk_in_boundary>
     <test_gk_in_boundary candidate="BF_DENW36AL1Pa000Nw" gk="GK_DENW36AL1Pa000OA">{$test_gk3_3InBoundary}</test_gk_in_boundary>
     <test_gk_in_boundary candidate="BF_DENW36AL1Pa000Nw" gk="GK_DENW36AL1Pa000OB">{$test_gk3_4InBoundary}</test_gk_in_boundary>
     </tests>
  </relationshiptest>
 </test_arc_interpolation_multiArcsInBoundary>
