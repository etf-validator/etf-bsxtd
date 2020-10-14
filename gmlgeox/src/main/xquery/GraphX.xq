(:~
 :
 : ---------------------------------------
 : GmlGeoX XQuery Function Library Facade
 : ---------------------------------------
 :
 : Copyright (C) 2018-2020 interactive instruments GmbH
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
 : @author  Johannes Echterhoff (echterhoff aT interactive-instruments doT de)
 :)
module namespace graph = 'https://modules.etf-validator.net/graphx/1';

import module namespace java = 'java:de.interactive_instruments.etf.bsxm.GraphX';

(:~
 : Initialize the GrapX module with the database name
 :
 : Must be run before using other functions.
 : Database names must be suffixed with a three digits index, i.e. DB-000.
 :
 : Throws IllegalArgumentException if database name is not suffixed with a three digits index.
 :
 : @param  $databaseName full database name ( i.e. DB-000 )
 :)
declare function graph:init($databaseName as xs:string) as empty-sequence() {
    java:init($databaseName)
};

(:~
 : Resets the simple graph held by this module. Use this method to ensure that resources can be reclaimed once the graph is no longer needed.
 :)
declare function graph:resetSimpleGraph() as empty-sequence() {
    java:resetSimpleGraph()
};

(:~
 : Adds the given database node as a vertex to the simple graph held by this module.
     *
 : NOTE: Before a new graph is created, ensure that a previously established graph is reset using {@link #resetSimpleGraph()}.
     *
 : @param $vertex the database node to add to the graph as a new vertex
 :)
declare function graph:addVertexToSimpleGraph($vertex) as empty-sequence() {
    java:addVertexToSimpleGraph($vertex)
};

(:~
 : Adds an undirected edge between two vertices of the simple graph held by this module. The two vertices must already be contained in this graph. If they are not found in the graph, an IllegalArgumentException will be thrown.
     *
 : @param $vertex1 represents one end of the edge
 : @param $vertex2 represents the other end of the edge
 :)
declare function graph:addEdgeToSimpleGraph($vertex1, $vertex2) as empty-sequence() {
    java:addEdgeToSimpleGraph($vertex1, $vertex2)
};

(:~
 : @return A DOM element, with the connected sets (represented as a sequence of nodes of the vertices in the set) that were found in the simple graph; the element can be empty (if the simple graph held by this module is empty). The element has the following (exemplary) structure:
 :
 : <graph:ConnectedSets xmlns:graph=
    "https://modules.etf-validator.net/graphx/1">
 :  <graph:set>
 :    <graph:ConnectedSet>
 :      <graph:member>
 :        <!-- A database node that is a vertex in the simple graph and part of the connected set. -->
 :        <xyz:ElementA ..>
 :          ..
 :        </xyz:ElementA>
 :      </graph:member>
 :      <graph:member>
 :        <!-- A database node that is a vertex in the simple graph and part of the connected set. -->
 :        <xyz:ElementB ..>
 :          ..
 :        </xyz:ElementB>
 :      </graph:member>
 :      ..
 :    </graph:ConnectedSet>
 :  </graph:set>
 :  ..
 : </graph:ConnectedSets>
 :)
declare function graph:determineConnectedSetsInSimpleGraph() {
    java:determineConnectedSetsInSimpleGraph()
};
