package org.dnacronym.hygene.models;

import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dnacronym.hygene.core.HygeneEventBus;
import org.dnacronym.hygene.core.ThrottledExecutor;
import org.dnacronym.hygene.events.CenterPointQueryChangeEvent;
import org.dnacronym.hygene.events.LayoutDoneEvent;
import org.dnacronym.hygene.events.NodeMetadataCacheUpdateEvent;
import org.dnacronym.hygene.graph.Segment;
import org.dnacronym.hygene.graph.Subgraph;
import org.dnacronym.hygene.parser.GfaFile;
import org.dnacronym.hygene.parser.ParseException;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Represents the cache of {@link Node}s with metadata loaded.
 */
public final class NodeMetadataCache {
    private static final Logger LOGGER = LogManager.getLogger(NodeMetadataCache.class);

    /**
     * Defines the maximum radius for which nodes will be cached.
     */
    private static final int RADIUS_THRESHOLD = 150;
    /**
     * The minimum number of milliseconds that must be between each retrieval operation.
     */
    private static final int RETRIEVE_METADATA_TIMEOUT = 10;

    private final ThrottledExecutor retrievalExecutor;
    private final GfaFile gfaFile;

    private int currentRadius;


    /**
     * Constructs and initializes {@link NodeMetadataCache}.
     */
    public NodeMetadataCache(final GfaFile gfaFile) {
        this.retrievalExecutor = new ThrottledExecutor(RETRIEVE_METADATA_TIMEOUT);
        this.gfaFile = gfaFile;

        HygeneEventBus.getInstance().register(this);
    }


    @Subscribe
    public void centerPointQueryChanged(final CenterPointQueryChangeEvent event) {
        this.currentRadius = event.getRadius();
    }

    @Subscribe
    public void layoutDone(final LayoutDoneEvent event) {
        if (currentRadius >= RADIUS_THRESHOLD) {
            retrievalExecutor.stop();
            return;
        }

        retrievalExecutor.run(() -> {
            retrieveMetadata(gfaFile, event.getSubgraph());
            if (Thread.interrupted()) {
                return;
            }
            HygeneEventBus.getInstance().post(new NodeMetadataCacheUpdateEvent());
        });
    }


    /**
     * Retrieves metadata for cached nodes that have no metadata yet.
     */
    private void retrieveMetadata(final GfaFile gfaFile, final Subgraph subgraph) {
        try {
            final Map<Integer, Integer> sortedSegmentsWithoutMetadata
                    = getSortedSegmentsWithoutMetadata(subgraph.getSegments());
            final Map<Integer, NodeMetadata> metadata
                    = gfaFile.parseNodeMetadata(sortedSegmentsWithoutMetadata);

            metadata.entrySet().forEach(entry -> {
                final Segment segment = subgraph.getSegment(entry.getKey());
                if (segment != null) {
                    segment.setMetadata(entry.getValue());
                }
            });
        } catch (final ParseException e) {
            LOGGER.error("Node metadata could not be retrieved.", e);
            return;
        }
    }

    //
    private Map<Integer, Integer> getSortedSegmentsWithoutMetadata(final Collection<Segment> segments) {
        return segments.stream()
                .filter(node -> !node.hasMetadata())
                .sorted(Comparator.comparingInt(Segment::getLineNumber))
                .collect(Collectors.toMap(
                        Segment::getId,
                        Segment::getLineNumber,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }
}
