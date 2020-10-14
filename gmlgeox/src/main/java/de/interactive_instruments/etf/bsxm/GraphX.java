/**
 * Copyright 2017-2020 European Union, interactive instruments GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf.bsxm;

import java.util.List;
import java.util.Set;

import org.basex.query.QueryModule;
import org.basex.query.value.item.QNm;
import org.basex.query.value.node.DBNode;
import org.basex.query.value.node.FElem;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import de.interactive_instruments.etf.bsxm.node.DBNodeRef;
import de.interactive_instruments.etf.bsxm.node.DBNodeRefFactory;
import de.interactive_instruments.etf.bsxm.node.DBNodeRefLookup;

/**
 * This module supports the creation of graphs and performing algorithms on them.
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
final public class GraphX extends QueryModule {

    public static final byte[] GRAPHX_NS = "https://modules.etf-validator.net/graphx/1".getBytes();
    public static final byte[] GRAPHX_PREFIX = "graph".getBytes();

    private SimpleGraph<DBNodeRef, DefaultEdge> simpleGraph = new SimpleGraph<>(DefaultEdge.class);

    private DBNodeRefLookup dbNodeRefLookup;
    private DBNodeRefFactory dbNodeRefFactory;

    public GraphX() {}

    /**
     * Initialize the GrapX module with the database name
     *
     * Must be run before using other functions. Database names must be suffixed with a three digits index, i.e. DB-000.
     *
     * Throws IllegalArgumentException if database name is not suffixed with a three digits index.
     *
     * @param databaseName
     *            full database name ( i.e. DB-000 )
     */
    @Requires(Permission.NONE)
    public void init(final String databaseName) {
        this.dbNodeRefFactory = DBNodeRefFactory.create(databaseName);
        this.dbNodeRefLookup = new DBNodeRefLookup(this.queryContext, this.dbNodeRefFactory);
    }

    /**
     * Resets the simple graph held by this module. Use this method to ensure that resources can be reclaimed once the graph
     * is no longer needed.
     */
    @Requires(Permission.NONE)
    public void resetSimpleGraph() {
        simpleGraph = new SimpleGraph<>(DefaultEdge.class);
    }

    /**
     * Adds the given database node as a vertex to the simple graph held by this module.
     *
     * NOTE: Before a new graph is created, ensure that a previously established graph is reset using
     * {@link #resetSimpleGraph()}.
     *
     * @param vertex
     *            the database node to add to the graph as a new vertex
     */
    @Requires(Permission.NONE)
    public void addVertexToSimpleGraph(final DBNode vertex) {
        final DBNodeRef nodeEntry = this.dbNodeRefFactory.createDBNodeEntry(vertex);
        simpleGraph.addVertex(nodeEntry);
    }

    /**
     * Adds an undirected edge between two vertices of the simple graph held by this module. The two vertices must already
     * be contained in this graph. If they are not found in the graph, an IllegalArgumentException will be thrown.
     *
     * @param vertex1
     *            represents one end of the edge
     * @param vertex2
     *            represents the other end of the edge
     */
    @Requires(Permission.NONE)
    public void addEdgeToSimpleGraph(final DBNode vertex1, final DBNode vertex2) {
        final DBNodeRef nodeEntry1 = this.dbNodeRefFactory.createDBNodeEntry(vertex1);
        final DBNodeRef nodeEntry2 = this.dbNodeRefFactory.createDBNodeEntry(vertex2);
        simpleGraph.addEdge(nodeEntry1, nodeEntry2);
    }

    /**
     * @return A DOM element, with the connected sets (represented as a sequence of nodes of the vertices in the set) that
     *         were found in the simple graph; the element can be empty (if the simple graph held by this module is empty).
     *         The element has the following (exemplary) structure:
     *
     *         <pre>
     * {@code
     * <graph:ConnectedSets xmlns:graph=
    "https://modules.etf-validator.net/graphx/1">
     *   <graph:set>
     *     <graph:ConnectedSet>
     *       <graph:member>
     *         <!-- A database node that is a vertex in the simple graph and part of the connected set. -->
     *         <xyz:ElementA ..>
     *           ..
     *         </xyz:ElementA>
     *       </graph:member>
     *       <graph:member>
     *         <!-- A database node that is a vertex in the simple graph and part of the connected set. -->
     *         <xyz:ElementB ..>
     *           ..
     *         </xyz:ElementB>
     *       </graph:member>
     *       ..
     *     </graph:ConnectedSet>
     *   </graph:set>
     *   ..
     * </graph:ConnectedSets>
     * }
     * </pre>
     */
    @Requires(Permission.NONE)
    public FElem determineConnectedSetsInSimpleGraph() {

        final QNm CONNECTED_SETS_QNM = new QNm(GRAPHX_PREFIX, "ConnectedSets", GRAPHX_NS);
        final QNm SET_QNM = new QNm(GRAPHX_PREFIX, "set", GRAPHX_NS);
        final QNm CONNECTED_SET_QNM = new QNm(GRAPHX_PREFIX, "ConnectedSet", GRAPHX_NS);
        final QNm MEMBER_QNM = new QNm(GRAPHX_PREFIX, "member", GRAPHX_NS);

        final ConnectivityInspector<DBNodeRef, DefaultEdge> ci = new ConnectivityInspector<>(simpleGraph);
        final List<Set<DBNodeRef>> connectedSets = ci.connectedSets();

        final FElem root = new FElem(CONNECTED_SETS_QNM);

        for (final Set<DBNodeRef> s : connectedSets) {

            final FElem set = new FElem(SET_QNM);
            final FElem conSet = new FElem(CONNECTED_SET_QNM);

            for (DBNodeRef nodeRef : s) {
                final FElem member = new FElem(MEMBER_QNM);
                member.add(this.dbNodeRefLookup.resolve(nodeRef));
                conSet.add(member);
            }

            set.add(conSet);
            root.add(set);
        }

        return root;
    }

}
