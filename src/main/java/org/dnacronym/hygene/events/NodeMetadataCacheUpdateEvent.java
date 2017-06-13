package org.dnacronym.hygene.events;

import org.dnacronym.hygene.graph.Subgraph;


public class NodeMetadataCacheUpdateEvent {
    private final Subgraph subgraph;


    public NodeMetadataCacheUpdateEvent(final Subgraph subgraph) {
        this.subgraph = subgraph;
    }


    public Subgraph getSubgraph() {
        return subgraph;
    }
}
