(:~
 :
 : -------------------------------------------------
 : Reusable Resource XQuery Function Library Facade
 : -------------------------------------------------
 :
 : Copyright (C) 2019 interactive instruments GmbH
 :
 : Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 : the European Commission - subsequent versions of the EUPL (the "Licence");
 : You may not use this work except in compliance with the Licence.
 : You may obtain a copy of the Licence at:
 :
 : https://joinup.ec.europa.eu/software/page/eupl
 :
 : Unless required by applicable law or agreed to in writing, software
 : distributed under the Licence is distributed on an "AS IS" basis,
 : WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 : See the Licence for the specific language governing permissions and
 : limitations under the Licence.
 :
 : @see     https://jonherrmann.github.io/etf-bridge/Developer_documentation.html (will be changed in future releases)
 : @author  Jon Herrmann ( herrmann aT interactive-instruments doT de )
 :
 :)
module namespace reusable = 'https://modules.etf-validator.net/reusableresources/1';

import module namespace java = 'java:de.interactive_instruments.etf.bsxm.ReusableResource';

declare function reusable:init($attachmentDir) {
    java:init($attachmentDir)
};

(:~
 : Restore or initialize an externalizable Java Object
 :
 : The Java Class must implement the Java Externalizable interface. This function uses the
 : class name of the Java Object as identifier to store and restore information. See the
 : other restoreOrInit() function which allows you to use your own identifier.
 :
 : The initialization function must be a non-parameter lambda expression. Example:
 : let $init := function() {
 :    let $dummy := prof:dump('Called once for initialization')
 :    (: init code here :)
 :    return map { }
 :  }
 :
 : Throws BaseXException if the Java Class does not implement the Externalizable interface,
 : if the storing or restoring failed.
 :
 : @param   $javaOject Java Object
 : @param   $initFct lambda initialization function that is called to initialized the object
 : @returns true if the object was restored, otherwise a sequence whose first element is false
 :)
declare function reusable:restoreOrInit(
    $javaOject,
    $initFct as function() as map(*)) {

    if(java:existsObj($javaOject)) then
        (true(), prof:time(java:restore($javaOject), "Restoring resource: "))
    else
        (false(), $initFct(), java:store($javaOject))
};

(:~
 : Restore or initialize an externalizable Java Object
 :
 : The Java Class must implement the Java Externalizable interface.
 :
 : The initialization function must be a non-parameter lambda expression. Example:
 : let $init := function() {
 :    let $dummy := prof:dump('Called once for initialization')
 :    (: init code here :)
 :    return map { }
 :  }
 :
 : Throws BaseXException if the Java Class does not implement the Externalizable interface,
 : if the storing or restoring failed.
 :
 : @param   $resourceName the name of the resource
 : @param   $javaOject Java Object
 : @param   $initFct lambda initialization function that is called to initialized the object
 : @returns true if the object was restored, otherwise a sequence whose first element is false
 :)
declare function reusable:restoreOrInit(
    $resourceName as xs:string,
    $javaOject,
    $initFct as function() as map(*)) {

    if(java:existsObjByName($resourceName)) then
        (true(), prof:time(java:restore($javaOject, $resourceName), "Restoring resource '" || $resourceName || "': "))
    else
        (false(), $initFct(), java:store($javaOject, $resourceName))
};

(:~
 : Restore or initialize a XQuery map
 :
 : The initialization function must be a lambda expression that returns a map. Example:
 : let $initMap := function() {
 :   let $map := map { (//*:member/@*:id)[1] : //*[1], 'bar': //*[2] }
 :   return $map
 : }
 :
 : Please note that function only supports Maps with values that reference Nodes in the Database.
 : This is not supported:
 : let $initMap := function() {
 :   let $map := map { (//*:member/@*:id)[1] : 'UNSUPPORTED', 'bar': //*[2] }
 :   return $map
 : }
 :
 : @param   $resourceName the name of the resource
 : @param   $initFct lambda initialization function that is called to initialized the map
 : @returns empty sequence
 :)
declare function reusable:restoreOrInitMap(
    $resourceName as xs:string,
    $initFct as function() as map(*)) as map(*) {

    if(java:existsObjByName($resourceName)) then
        prof:time(java:restoreMap($resourceName), "Restoring resource '" || $resourceName || "': ")
    else
        let $intializedMap := $initFct()
        let $d := java:storeMap($intializedMap, $resourceName)
        return $intializedMap
};
