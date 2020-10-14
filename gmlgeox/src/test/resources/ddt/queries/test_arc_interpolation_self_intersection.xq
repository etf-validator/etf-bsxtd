import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $candidates := db:open($dbname)//ii:member/*
let $init := ggeo:init($dbname)

let $dummyUnregisterDefaultGmlGeoXGeometryTypes := prof:void(ggeo:unregisterAllGmlGeometries())
let $dummyRegisterRelevantGmlGeometryTypes :=  prof:void(('Point','MultiPoint','Curve','MultiCurve','CompositeCurve','PolyhedralSurface','Surface','MultiSurface','CompositeSurface') ! ggeo:registerGmlGeometry(.))

let $dummySetMaxError := ggeo:setMaxErrorForInterpolation(xs:double(0.00000001))
let $dummySetMaxNumPoints := ggeo:setMaxNumPointsForInterpolation(xs:int(1000000))

let $setCustomCacheSize := ggeo:cacheSize(1000000)
let $cacheSizeBeforeValidation := ggeo:getCacheSize()
let $setCacheSizeForValidation := ggeo:cacheSize(0)

let $featuresWithErrors_map := 
 map:merge(for $candidate in $candidates
  let $validationResult := ggeo:validateAndReport($candidate,'1111')
  return
    if(xs:boolean($validationResult/*:isValid)) then
      ()
    else 
      (: map:entry(string($candidate/@gml:id), $validationResult) :)
      let $result := $validationResult/*:result/text()
      let $messages :=
        for $error in $validationResult/*:errors/*:message
            return ' ID ' || $error/*:argument[@*:token='ID']/text() || 
            ', context element ' || $error/*:argument[@*:token='context'] || 
            " : " || $error/*:argument[@*:token='original'] || 
            " Coordinates (" ||  $error/*:argument[@*:token='coordinates'] || ')'
      let $errDescription := string-join(($result, $messages),' ')
      return
        map:entry(string($candidate/@gml:id), ($candidate,$errDescription))
 )

let $dummyResetCacheSizeToValueBeforeValidation := ggeo:cacheSize($cacheSizeBeforeValidation)
let $dummyResetMaxError := ggeo:setMaxErrorForInterpolation(xs:double(0.00001))
let $dummyResetMaxNumPoints := ggeo:setMaxNumPointsForInterpolation(xs:int(1000))

return
 <test_arc_interpolation_self_intersection>  
  <validationtest>
  {
    for $key in map:keys($featuresWithErrors_map)
    order by $key
    return
      <test id="{$key}">{map:get($featuresWithErrors_map,$key)[2]}</test>
  }
  </validationtest>
  <cacheSizeBeforeValidation>{ggeo:getCacheSize()}</cacheSizeBeforeValidation>
 </test_arc_interpolation_self_intersection>
