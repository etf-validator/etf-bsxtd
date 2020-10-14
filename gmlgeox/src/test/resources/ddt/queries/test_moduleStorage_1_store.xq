import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';
import module namespace modulestore = 'de.interactive_instruments.etf.bsxm.GmlGeoXStoreTester';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $init := ggeo:init($dbname)
let $members := db:open($dbname)//ii:member

(: NOTE: There are no geometry parsing errors in this Unit test. Thus, the query does not handle such errors. Production code should take into account geometry parsing errors. :)
let $dummyPrepareSpatialIndex :=
  map:merge(for $member in $members
    let $geometryNode := $member/*
    return 
    if ($geometryNode) then 
      prof:void(ggeo:prepareSpatialIndex($member,$geometryNode))
    else ())
    
let $dummyBuildDefaultSpatialIndex := prof:void(ggeo:buildSpatialIndex())

(: Test retrieval of pre-computed envelope for geometry node. :)
let $env1 := ggeo:envelope($members[@gml:id = 'Mem.1']/*)
let $result1 := string-join($env1,' ')

(: Test search via spatial index. :)
let $spatialIndexSearchResults := ggeo:search($members[@gml:id = 'Mem.2']/*)
let $result2 := string-join(
 (for $res in $spatialIndexSearchResults
  order by $res/@gml:id
  return
    $res/@gml:id
 ),' ')
 
let $gmlgeoxModule := ggeo:getModuleInstance()
let $dummyStoreGmlGeoXModule := modulestore:storeQueryModule($gmlgeoxModule,'GmlGeoX')
let $logStorage := prof:dump('Stored GmlGeoX module.')

return
 <test_moduleStorage>  
  <test1_envelope>{$result1}</test1_envelope>  
  <test2_search>{$result2}</test2_search>
 </test_moduleStorage>
