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
package de.interactive_instruments.etf.bsxm.index;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.internal.EntryDefault;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.basex.query.QueryException;
import org.basex.query.value.node.ANode;
import org.basex.query.value.node.DBNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.etf.bsxm.GmlGeoX;
import de.interactive_instruments.etf.bsxm.node.DBNodeRef;
import de.interactive_instruments.etf.bsxm.node.DBNodeRefLookup;
import de.interactive_instruments.etf.bsxm.node.ExternalizableDBNodeRefMap;

/**
 * The GeometryManager builds and maintains spatial indexes and an in-memory cache for JTS geometries that can be used
 * with the GmlGeoX module. The cache is filled during the indexing of the geometries and updated when geometries are
 * accessed using the {@link GmlGeoX#getOrCacheGeometry(ANode)} function.
 *
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public final class GeometryCache implements Externalizable {

    public static String DEFAULT_SPATIAL_INDEX = "";

    private static final Logger logger = LoggerFactory.getLogger(GeometryCache.class);

    // Max cache entries as number
    private static final String ETF_GEOCACHE_SIZE = "etf.gmlgeox.geocache.size";

    // Record hitcounts and misscounts as boolean
    private static final String ETF_GEOCACHE_REC_STATS = "etf.gmlgeox.geocache.statistics";

    /**
     * Geometry cache, where a key is the ID of a database node that represents a geometry, and the value is the JTS
     * geometry parsed from that node.
     */
    private Cache<DBNodeRef, Geometry> geometryCache = null;
    private HashMap<DBNodeRef, Envelope> envelopeByDBNodeEntry = new HashMap<>();
    private Map<String, RTree<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>> rtreeByIndexName = new HashMap<>();
    private final Map<String, List<com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>>> geomIndexEntriesByIndexName = new HashMap<>();
    private int maxSizeOfGeometryCache;

    public GeometryCache() {
        resetCache(Integer.valueOf(System.getProperty(ETF_GEOCACHE_SIZE, "100000")));
    }

    /**
     * Resets the geometry cache, by replacing the existing cache with a new cache of the given size. That means that all
     * cached geometries will be lost.
     *
     * @param maxSize
     *            new cache size
     */
    public void resetCache(final int maxSize) {
        try {
            if (logger.isDebugEnabled() || Boolean.valueOf(System.getProperty(ETF_GEOCACHE_REC_STATS, "false"))) {
                geometryCache = Caffeine.newBuilder().recordStats().maximumSize(maxSize).build();
            } else {
                geometryCache = Caffeine.newBuilder().maximumSize(maxSize).build();
            }
            this.maxSizeOfGeometryCache = maxSize;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cache for geometries could not be initialized: " + e.getMessage());
        }
    }

    /**
     * Get a geometry from the cache
     *
     * @param dbNode
     *            tbd
     * @return the parsed geometry of the geometry node, or <code>null</code> if no geometry was found
     */
    @Nullable
    public com.vividsolutions.jts.geom.Geometry getGeometry(final DBNodeRef dbNode) {
        return geometryCache.getIfPresent(dbNode);
    }

    /**
     * Returns the number of all read accesses to the cache
     *
     * @return number of read accesses to the cache
     */
    public long getCount() {
        return geometryCache.stats().hitCount();
    }

    /**
     * Returns the number of all failed read accesses to the cache
     *
     * @return number of failed read accesses to the cache
     */
    public long getMissCount() {
        return geometryCache.stats().missCount();
    }

    /**
     * Put a feature geometry in the cache
     *
     * @param nodeRef
     *            the reference of the database node that represents the geometry
     * @param geom
     *            the geometry to cache
     */
    public void cacheGeometry(DBNodeRef nodeRef, com.vividsolutions.jts.geom.Geometry geom) {
        geometryCache.put(nodeRef, geom);
    }

    /**
     * Index a geometry
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @param entry
     *            the entry referencing the BaseX node (typically of a feature)
     * @param geometry
     *            the geometry to index
     */
    public void index(@NotNull final String indexName, final DBNodeRef entry,
            final com.github.davidmoten.rtree.geometry.Geometry geometry) {
        RTree<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry> rtree = rtreeByIndexName.get(indexName);
        if (rtree == null) {
            rtree = RTree.star().create();
        }
        rtree = rtree.add(entry, geometry);
        rtreeByIndexName.put(indexName, rtree);
    }

    /**
     * Report current size of the named spatial index
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @return size of the spatial index; can be 0 if no index with given name was found
     */
    public int indexSize(@NotNull final String indexName) {
        if (rtreeByIndexName.containsKey(indexName)) {
            return rtreeByIndexName.get(indexName).size();
        } else {
            return 0;
        }
    }

    /**
     * return all entries in the named spatial index
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @param lookup
     *            tbd
     * @return iterator over all entries; can be <code>null</code> if no index with given name was found
     */
    public List<DBNode> getAll(@NotNull final String indexName, final DBNodeRefLookup lookup) {
        if (rtreeByIndexName.containsKey(indexName)) {
            return lookup
                    .collect(rtreeByIndexName.get(indexName).entries().map(com.github.davidmoten.rtree.Entry::value));
        } else {
            return null;
        }
    }

    /**
     * Return all entries in the named spatial index whose bounding box intersects with the given bounding box
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @param bbox
     *            the bounding box / rectangle
     * @param lookup
     *            tbd
     * @return iterator over all detected entries; can be <code>null</code> if no index with given name was found
     */
    @NotNull
    public List<DBNode> search(@NotNull final String indexName, final Rectangle bbox, final DBNodeRefLookup lookup) {
        if (rtreeByIndexName.containsKey(indexName)) {
            return lookup.collect(
                    rtreeByIndexName.get(indexName).search(bbox).map(com.github.davidmoten.rtree.Entry::value));
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns the nearest k entries (k=maxCount) to the given point where the entries are strictly less than a given
     * maximum distance from the point.
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @param point
     *            the position at which nearest entries are searched.
     * @param maxDistance
     *            as the maximum distance to search nearest entries.
     * @param maxCount
     *            tbd
     * @return iterator over all detected entries; can be empty - if no index with given name was found, or no entries
     *         within the maximum distance - but not <code>null</code>
     */
    public List<com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>> nearest(
            final String indexName, final Point point, final double maxDistance, final int maxCount) {

        if (rtreeByIndexName.containsKey(indexName)) {

            return rtreeByIndexName.get(indexName).nearest(point, maxDistance, maxCount).toList().toBlocking().single();

        } else {

            return new ArrayList<com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>>();
        }
    }

    /**
     * Create the named spatial index by bulk loading, using the STR method. Before the index can be built, entries must be
     * added by calling {@link #prepareSpatialIndex(String, DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry)}.
     *
     * <p>
     * According to https://github.com/ambling/rtree-benchmark, creating an R*-tree using bulk loading is faster than doing
     * so without bulk loading. Furthermore, according to https://en.wikipedia.org/wiki/R-tree, an STR bulk loaded R*-tree
     * is a "very efficient tree".
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @throws QueryException
     *             If the index has already been built.
     */
    public void buildIndexUsingBulkLoading(@NotNull final String indexName) throws QueryException {
        if (rtreeByIndexName.containsKey(indexName)) {
            throw new QueryException("Spatial index '" + indexName + "' has already been built.");
        } else if (geomIndexEntriesByIndexName.containsKey(indexName)) {
            RTree<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry> rtree = RTree.star()
                    .create(geomIndexEntriesByIndexName.get(indexName));
            rtreeByIndexName.put(indexName, rtree);
            geomIndexEntriesByIndexName.remove(indexName);
        }
        // Else: No entries for that index have been added using
        // prepareSpatialIndex(...) -> ignore
    }

    /**
     * Removes the named spatial index and all its entries.
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     */
    public void removeIndex(@NotNull final String indexName) {
        rtreeByIndexName.remove(indexName);
    }

    /**
     * @param entry
     *            tbd
     * @return <code>true</code> if a mapping to an envelope exists for the given DBNode entry, else <code>false</code>s
     */
    public boolean hasEnvelope(final DBNodeRef entry) {
        return envelopeByDBNodeEntry.containsKey(entry);
    }

    /**
     * @param entry
     *            tbd
     * @return The envelope stored for the given entry, or <code>null</code> if the manager does not contain a mapping for
     *         the given entry.
     */
    public Envelope getEnvelope(final DBNodeRef entry) {
        return envelopeByDBNodeEntry.get(entry);
    }

    /**
     * Adds a mapping from the given entry to the given envelope to this manager.
     *
     * @param entry
     *            tbd
     * @param env
     *            tbd
     */
    public void addEnvelope(final DBNodeRef entry, final Envelope env) {
        envelopeByDBNodeEntry.put(entry, env);
    }

    /**
     * Returns the minimum bounding rectangle of a given rtree by index name.
     *
     * @param indexName
     *            as String of the indexed rtree
     * @return minimum bounding rectangle
     */
    public Rectangle getIndexMbr(@NotNull final String indexName) {
        return rtreeByIndexName.get(indexName).mbr().get();
    }

    /**
     * Prepares spatial indexing by caching an entry for the named spatial index.
     *
     * <p>
     * With an explicit call to {@link #buildIndexUsingBulkLoading(String)}, that index is built.
     *
     * @param indexName
     *            Identifies the index. The empty string identifies the default index.
     * @param nodeEntry
     *            represents the node of the element to be indexed
     * @param geometry
     *            the geometry to use in the index
     */
    public void prepareSpatialIndex(@NotNull final String indexName, DBNodeRef nodeEntry,
            com.github.davidmoten.rtree.geometry.Geometry geometry) {
        final List<com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>> geomIndexEntries;
        if (geomIndexEntriesByIndexName.containsKey(indexName)) {
            geomIndexEntries = geomIndexEntriesByIndexName.get(indexName);
        } else {
            geomIndexEntries = new ArrayList<>();
            geomIndexEntriesByIndexName.put(indexName, geomIndexEntries);
        }
        geomIndexEntries.add(new EntryDefault<>(nodeEntry, geometry));
    }

    /** @return the maximum size of the geometry cache */
    public int getCacheSize() {
        return this.maxSizeOfGeometryCache;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(maxSizeOfGeometryCache);
        final ExternalizableDBNodeRefMap dbNodeRefMap = new ExternalizableDBNodeRefMap();
        // Geometries
        {
            final ConcurrentMap<DBNodeRef, Geometry> geometryCacheMap = geometryCache.asMap();
            out.writeInt(geometryCacheMap.size());
            int posGeo = 0;
            final int[] geometryCacheDBNodeRefPositions = dbNodeRefMap.addAndGetRefPositions(geometryCacheMap.keySet());
            for (final Geometry geometry : geometryCacheMap.values()) {
                out.writeInt(geometryCacheDBNodeRefPositions[posGeo++]);
                out.writeObject(ExternalizableJtsGeometry.create(geometry));
            }
        }
        // Envelopes
        {
            int posEnvelope = 0;
            final int[] geometryCacheDBNodeRefPositions = dbNodeRefMap
                    .addAndGetRefPositions(envelopeByDBNodeEntry.keySet());
            out.writeInt(envelopeByDBNodeEntry.size());
            for (final Envelope geometry : envelopeByDBNodeEntry.values()) {
                out.writeInt(geometryCacheDBNodeRefPositions[posEnvelope++]);
                out.writeObject(new ExternalizableJtsEnvelope(geometry));
            }
        }
        // Rtrees
        {
            out.writeInt(rtreeByIndexName.size());
            for (final Entry<String, RTree<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>> rtreeIndex : rtreeByIndexName
                    .entrySet()) {
                out.writeUTF(rtreeIndex.getKey());
                final List<com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>> rtreeEntrySet = rtreeIndex
                        .getValue().entries().toList().toBlocking().single();
                final int[] rtreeDBNodeRefPositions = dbNodeRefMap
                        .addAndGetRefPositions(rtreeEntrySet.stream().map(com.github.davidmoten.rtree.Entry::value)
                                .collect(Collectors.toCollection(() -> new ArrayList<>(rtreeEntrySet.size()))));
                out.writeInt(rtreeDBNodeRefPositions.length);
                int posRtree = 0;
                for (final com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry> entry : rtreeEntrySet) {
                    out.writeInt(rtreeDBNodeRefPositions[posRtree++]);
                    out.writeObject(ExternalizableRtreeGeometry.create(entry.geometry()));
                }
            }
        }
        out.writeObject(dbNodeRefMap);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        this.maxSizeOfGeometryCache = in.readInt();

        final GeometryFactory jtsGeomFactory = new GeometryFactory();

        // Prepare Geometries
        int[] geometryCacheDBNodeRefPositions = new int[in.readInt()];
        List<Geometry> geometries = new ArrayList<>(geometryCacheDBNodeRefPositions.length);
        for (int i = 0; i < geometryCacheDBNodeRefPositions.length; i++) {
            geometryCacheDBNodeRefPositions[i] = in.readInt();
            geometries.add(((ExternalizableJtsGeometry) in.readObject()).toJtsGeometry(jtsGeomFactory));
        }

        // Prepare Envelopes
        int[] envelopeDBNodeRefPositions = new int[in.readInt()];
        List<Envelope> envelopes = new ArrayList<>(geometryCacheDBNodeRefPositions.length);
        for (int i = 0; i < envelopeDBNodeRefPositions.length; i++) {
            envelopeDBNodeRefPositions[i] = in.readInt();
            envelopes.add(((ExternalizableJtsEnvelope) in.readObject()).toEnvelope());
        }

        // Prepare Rtrees
        final String[] indexnames = new String[in.readInt()];
        final List<int[]> rtreeDBNodeRefPositions = new ArrayList<>(indexnames.length);
        final List<com.github.davidmoten.rtree.geometry.Geometry[]> rtreeGeometries = new ArrayList<>(
                indexnames.length);
        for (int i = 0; i < indexnames.length; i++) {
            indexnames[i] = in.readUTF();
            final int[] positions = new int[in.readInt()];
            final com.github.davidmoten.rtree.geometry.Geometry[] rtreeGeos = new com.github.davidmoten.rtree.geometry.Geometry[positions.length];
            for (int p = 0; p < positions.length; p++) {
                positions[p] = in.readInt();
                rtreeGeos[p] = ((ExternalizableRtreeGeometry) in.readObject()).toRtreeGeometry();
            }
            rtreeDBNodeRefPositions.add(positions);
            rtreeGeometries.add(rtreeGeos);
        }

        // Restore DBNodeRefs
        final ExternalizableDBNodeRefMap dbNodeRefMap = ((ExternalizableDBNodeRefMap) in.readObject());

        // Restore Geometries
        {
            resetCache(this.maxSizeOfGeometryCache);
            final DBNodeRef[] geometryCacheDBNodeRefs = dbNodeRefMap.getByRefPositions(geometryCacheDBNodeRefPositions);
            for (int i = 0; i < geometryCacheDBNodeRefs.length; i++) {
                this.geometryCache.put(geometryCacheDBNodeRefs[i], geometries.get(i));
            }
        }

        // Restore Envelopes
        {
            final DBNodeRef[] envelopeDBNodeRefs = dbNodeRefMap.getByRefPositions(envelopeDBNodeRefPositions);
            this.envelopeByDBNodeEntry = new HashMap<>(envelopeDBNodeRefs.length);
            for (int i = 0; i < envelopeDBNodeRefs.length; i++) {
                this.envelopeByDBNodeEntry.put(envelopeDBNodeRefs[i], envelopes.get(i));
            }
        }

        // Restore Rtrees
        {
            this.rtreeByIndexName = new HashMap<>(indexnames.length);
            for (int i = 0; i < indexnames.length; i++) {

                final DBNodeRef[] rtreeDBNodeRefs = dbNodeRefMap.getByRefPositions(rtreeDBNodeRefPositions.get(i));
                rtreeDBNodeRefPositions.set(i, null);
                final com.github.davidmoten.rtree.geometry.Geometry[] currentRtreeGeometries = rtreeGeometries.get(i);
                final List<com.github.davidmoten.rtree.Entry<DBNodeRef, com.github.davidmoten.rtree.geometry.Geometry>> entries = new ArrayList<>(
                        rtreeDBNodeRefs.length);
                for (int p = 0; p < rtreeDBNodeRefs.length; p++) {
                    entries.add(new EntryDefault<>(rtreeDBNodeRefs[p], currentRtreeGeometries[p]));
                }
                rtreeGeometries.set(i, null);
                this.rtreeByIndexName.put(indexnames[i], RTree.star().create(entries));
            }
        }

    }
}
