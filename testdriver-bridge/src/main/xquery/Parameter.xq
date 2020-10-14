(:~
 :
 : -------------------------------------------------
 : Parameter XQuery Function Library Facade
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
module namespace parameter = 'https://modules.etf-validator.net/parameter/1';

declare variable $parameter:error := xs:QName("parameter:ParameterError");

declare function parameter:asPositiveDouble($parameterName as xs:string, $paramValue) as xs:double {
    try {
      let $v := xs:double($paramValue)
      return if ($v gt 0) then $v else error($parameter:error, concat("Der Parameter '",$parameterName, "' muss größer Null sein. Der Wert ist '",data($paramValue),"'.&#xa;"))
    } catch err:FORG0001 {
      error($parameter:error, concat("Der Parameter '",$parameterName, "' muss eine Gleitkommazahl sein. Der Wert ist '",data($paramValue),"'.&#xa;"))
    }
};

declare function parameter:asPositiveInt($parameterName as xs:string, $paramValue) as xs:int {
    try {
      let $v := xs:int($paramValue)
      return if ($v gt 0) then $v else error($parameter:error, concat("Der Parameter '",$parameterName, "' muss größer Null sein. Der Wert ist '",data($paramValue),"'.&#xa;"))
    } catch err:FORG0001 {
      error($parameter:error, concat("Der Parameter '",$parameterName, "' muss eine Ganzzahl sein. Der Wert ist '",data($paramValue),"'.&#xa;"))
    }
};
