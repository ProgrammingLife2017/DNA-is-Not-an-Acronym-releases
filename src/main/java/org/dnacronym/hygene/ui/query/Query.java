package org.dnacronym.hygene.ui.query;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.dnacronym.hygene.graph.SearchQuery;
import org.dnacronym.hygene.parser.ParseException;
import org.dnacronym.hygene.ui.graph.GraphStore;

import java.util.Set;


/**
 * Queries are performed here.
 */
public final class Query {
    private final BooleanProperty visibleProperty;
    private final ObservableList<Integer> queriedNodeIds;

    private SearchQuery searchQuery;


    /**
     * Creates instance of {@link Query}.
     *
     * @param graphStore the {@link GraphStore} used to retrieve the most up to date graph
     */
    public Query(final GraphStore graphStore) {
        this.visibleProperty = new SimpleBooleanProperty(true);
        queriedNodeIds = FXCollections.observableArrayList();

        graphStore.getGfaFileProperty().addListener((observable, oldValue, newValue) ->
                setSearchQuery(new SearchQuery(newValue)));
    }


    /**
     * Sets the {@link SearchQuery} used to query the graph.
     *
     * @param searchQuery the {@link SearchQuery} used to query the graph
     */
    public void setSearchQuery(final SearchQuery searchQuery) {
        this.searchQuery = searchQuery;
    }

    /**
     * Performs a query by looking at the sequences of nodes and returning the nodes which contain the passed sequence.
     *
     * @param sequence the sequence to search for inside the sequences of nodes
     * @throws ParseException if unable to execute a regex query
     * @see SearchQuery
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // this is temporary
    public void query(final String sequence) throws ParseException {
        if (searchQuery == null) {
            return;
        }

        final Set<Integer> nodeIds = searchQuery.executeSequenceRegexQuery(sequence);
        queriedNodeIds.setAll(nodeIds);
    }

    /**
     * Returns the {@link BooleanProperty} which decides the visibility of the query pane.
     *
     * @return the {@link BooleanProperty} which decides the visibility of the query pane
     */
    public BooleanProperty getVisibleProperty() {
        return visibleProperty;
    }

    /**
     * Returns the {@link ObservableList} of the most recently queried node ids.
     *
     * @return the {@link ObservableList} of the most recently queried node ids
     */
    public ObservableList<Integer> getQueriedNodes() {
        return queriedNodeIds;
    }
}