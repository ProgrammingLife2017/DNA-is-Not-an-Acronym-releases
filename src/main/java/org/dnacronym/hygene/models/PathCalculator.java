package org.dnacronym.hygene.models;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dnacronym.hygene.graph.DummyNode;
import org.dnacronym.hygene.graph.Edge;
import org.dnacronym.hygene.graph.NewNode;
import org.dnacronym.hygene.graph.Segment;
import org.dnacronym.hygene.graph.Subgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * The class with calculate the genome paths.
 */
public class PathCalculator {
    private static final Logger LOGGER = LogManager.getLogger(PathCalculator.class);

    private Subgraph subgraph;

    private Set<Edge> visited;


    public PathCalculator(Subgraph subgraph) {
        this.subgraph = subgraph;
        this.visited = new HashSet<>();
    }


    public void computePaths(final Subgraph subgraph) {
        // Determine start
        List<NewNode> sourceConnectedNodes = subgraph.getNodes().stream()
                .filter(n -> subgraph.getNeighbours(n, SequenceDirection.LEFT).isEmpty())
                .collect(Collectors.toList());

        // Compute topological ordering

        Map<NewNode, Set<String>> genomeStore = new HashMap<>();

        final Queue<Edge> toVisit = new LinkedList<>();

        final NewNode origin = new Segment(-1, -1, 0);
        genomeStore.put(origin, new HashSet<>());

        sourceConnectedNodes.forEach(sourceConnectedNode -> {
            toVisit.add(new Edge(origin, sourceConnectedNode));
            genomeStore.get(origin).addAll(sourceConnectedNode.getMetadata().getGenomes());
        });

        List<NewNode> topologicalOrder = new LinkedList<>();

        HashSet<Edge> visitedEdges = new HashSet<>();
        HashSet<NewNode> visitedNodes = new HashSet<>();

        while (!toVisit.isEmpty()) {
            NewNode active = toVisit.remove().getTo();
            visitedNodes.add(active);
            topologicalOrder.add(active);

            visitedEdges.addAll(active.getOutgoingEdges());

            active.getOutgoingEdges().stream()
                    .filter(e -> !visitedNodes.contains(e.getTo()) && e.getTo().getIncomingEdges().stream()
                            .filter(out -> !visitedEdges.contains(out)).count() == 0)
                    .forEach(toVisit::add);

            if (active instanceof Segment) {
                System.out.println("Visiting segment" + active);

                genomeStore.put(active, new HashSet<>(active.getMetadata().getGenomes()));
            } else if (active instanceof DummyNode) {
                System.out.println("Visiting dummy node " + active);

                List<String> genomes = ((DummyNode) active).getDiversionSource().getMetadata().getGenomes();
                genomeStore.put(active, new HashSet<>(genomes));
            } else {
                throw new IllegalStateException("Invalid node type");
            }
        }

        System.out.println(genomeStore);

        // Create edges genome store
        Map<Edge, Set<String>> paths = new HashMap<>();

        // Go over topological order and assign importance
        topologicalOrder.forEach(node -> {
            node.getIncomingEdges().forEach(e -> {
                final Set<String> intersection = new HashSet<>(genomeStore.get(e.getFrom()));
                intersection.retainAll(genomeStore.get(node));

                paths.put(e, intersection);

                genomeStore.get(e.getFrom()).removeAll(intersection);
            });
        });

        System.out.println(paths);
    }
}
