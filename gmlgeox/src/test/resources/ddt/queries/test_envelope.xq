import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $membersAll := db:open($dbname)//ii:member
let $init := ggeo:init($dbname)

let $geometryParsingErrorsMap :=
  map:merge(for $member in $membersAll
    let $geometryNode := $member/*
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

let $logParsingErrors :=
  let $maxNumberOfParsingErrorsToLog := 100
  let $numberOfGeometryParsingErrors := map:size($geometryParsingErrorsMap)
  let $log1 := prof:dump($numberOfGeometryParsingErrors ||' geometry parsing error(s).')   
  return if(map:size($geometryParsingErrorsMap) = 0) then   
    ()
  else  
  let $log2 := if($numberOfGeometryParsingErrors > $maxNumberOfParsingErrorsToLog) then
    prof:dump('Logging only ' || $maxNumberOfParsingErrorsToLog || ' of these errors.')
    else ()
  let $keySequence := subsequence(map:keys($geometryParsingErrorsMap),1,$maxNumberOfParsingErrorsToLog)
  return 
   for $gmlid in $keySequence
   let $errValue := map:get($geometryParsingErrorsMap,$gmlid)
   let $errDescription := $errValue[2]
   return
   prof:dump('Member with @gml:id ' || $gmlid || ' - Geometry parsing error: ' || $errDescription)

(: Ignore members that have geometry parsing errors. Store remaining members as variable $members. :)
let $members := $membersAll[not(map:contains($geometryParsingErrorsMap,./@gml:id))]

let $env1 := ggeo:envelope($members[@gml:id = 'M1']/*)
let $result1 := string-join($env1,' ')

return
 <test_envelope>  
  <test1>{$result1}</test1>  
 </test_envelope>
