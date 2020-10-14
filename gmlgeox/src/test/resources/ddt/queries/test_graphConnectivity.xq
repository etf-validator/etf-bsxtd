import module namespace graph = 'https://modules.etf-validator.net/graphx/1';

declare variable $dbname external := 'GMLGEOX-JUNIT-TEST-DB-000';
let $dummyInitGraphX := graph:init($dbname)
let $nodes := db:open($dbname)//n

let $v1 := $nodes[@id="v1"]
let $v2 := $nodes[@id="v2"]
let $v3 := $nodes[@id="v3"]
let $v4 := $nodes[@id="v4"]
let $v5 := $nodes[@id="v5"]
let $v6 := $nodes[@id="v6"]
let $v7 := $nodes[@id="v7"]
let $v8 := $nodes[@id="v8"]
let $v9 := $nodes[@id="v9"]

let $createSimpleGraph :=	(
 graph:addVertexToSimpleGraph($v1),
	graph:addVertexToSimpleGraph($v2),
	graph:addVertexToSimpleGraph($v3),
	graph:addVertexToSimpleGraph($v4),
	graph:addVertexToSimpleGraph($v5),
	graph:addVertexToSimpleGraph($v6),
	graph:addVertexToSimpleGraph($v7),
	graph:addVertexToSimpleGraph($v8),
	graph:addVertexToSimpleGraph($v9),
	graph:addEdgeToSimpleGraph($v1, $v2),
	graph:addEdgeToSimpleGraph($v1, $v3),
	graph:addEdgeToSimpleGraph($v3, $v4),
	graph:addEdgeToSimpleGraph($v5, $v6),
	graph:addEdgeToSimpleGraph($v6, $v8),
	graph:addEdgeToSimpleGraph($v5, $v7),
	graph:addEdgeToSimpleGraph($v7, $v8))
	
	let $connectedSets := graph:determineConnectedSetsInSimpleGraph()
	let $results :=
	  for $connectedSet in $connectedSets/*:set/*
	  return <connectedSet>{string-join(sort($connectedSet/*:member/*/@id),'; ')}</connectedSet>
 (: Ensure that resources can be reclaimed once the graph is no longer needed. :)
 let $resetSimpleGraph := graph:resetSimpleGraph()
 return 
 <test_graphConnectivity>
 {$results}
 </test_graphConnectivity>
