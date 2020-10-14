import module namespace ggeo = 'https://modules.etf-validator.net/gmlgeox/2';

declare namespace gml = 'http://www.opengis.net/gml/3.2';
declare namespace ii = 'http://www.interactive-instruments.de/test';
declare namespace etf = 'http://www.interactive-instruments.de/etf/2.0';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $init := ggeo:init($dbname)
let $geometries := db:open($dbname)//ii:member/*

(: Default xquery file. Only used for testing validation functionalities :)

return
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
        for $message in $vr/ggeo:errors/*:message
        return $message
      }
     </messages>
    </test>
  }
 </validationtest>
