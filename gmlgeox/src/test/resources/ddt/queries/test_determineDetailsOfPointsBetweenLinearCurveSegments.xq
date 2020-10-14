import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $members := db:open($dbname)//ii:member
let $init := ggeo:init($dbname)

let $angleInGon := 2.0
let $angleInDegree := $angleInGon div 400 * 360
let $distance := 0.03

return
 <test_determineDetailsOfPointsBetweenLinearCurveSegments>
 {
   for $member in $members
   (: where $member/@gml:id/data() = 'Member_Curve3_one_bend_point' :)
   let $result := ggeo:determineDetailsOfPointsBetweenLinearCurveSegments($member/*)
   return 
     <test gml:id="{$member/@gml:id}">{
     for $relevantPoint in $result/*/*:Point
     return
       if(xs:decimal($relevantPoint/*:angleInDegree) le $angleInDegree or xs:decimal($relevantPoint/*:distance) le $distance) then
         <continuousPoint>{$relevantPoint}</continuousPoint>
       else 
         <bendPoint>{$relevantPoint}</bendPoint>
     }</test>
 }
</test_determineDetailsOfPointsBetweenLinearCurveSegments>
 
