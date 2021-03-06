package org.dnacronym.hygene.ui.query;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dnacronym.hygene.graph.SearchQuery;
import org.dnacronym.hygene.parser.MetadataParseException;
import org.dnacronym.hygene.ui.graph.GraphStore;

import java.util.Set;


/**
 * Queries are performed here.
 */
public final class Query {
    private static final Logger LOGGER = LogManager.getLogger(Query.class);

    private final BooleanProperty queryingProperty;
    private final BooleanProperty visibleProperty;
    private final ObservableList<Integer> queriedNodeIds;

    private SearchQuery searchQuery;


    /**
     * Creates instance of {@link Query}.
     *
     * @param graphStore the {@link GraphStore} used to retrieve the most up to date graph
     */
    @Inject
    public Query(final GraphStore graphStore) {
        visibleProperty = new SimpleBooleanProperty();
        queryingProperty = new SimpleBooleanProperty();
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
     * <p>
     * Also clears the current list of queried node ids to avoid confusion.
     *
     * @param sequence the sequence to search for inside the sequences of nodes
     * @see SearchQuery
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // this is temporary
    public void query(final String sequence) {
        if (searchQuery == null) {
            return;
        }
        queryingProperty.set(true);
        queriedNodeIds.clear();
        LOGGER.info("Started querying: '" + sequence + "'.");

        final Thread thread = new Thread(() -> {
            try {
                final Set<Integer> nodeIds = searchQuery.executeSequenceRegexQuery(sequence);
                Platform.runLater(() -> {
                    queriedNodeIds.setAll(nodeIds);
                    queryingProperty.set(false);
                });
            } catch (final MetadataParseException e) {
                LOGGER.error("Unable to execute a query.", e);
            }

            LOGGER.info("Finished querying: '" + sequence + "'.");
        });

        thread.setDaemon(true);
        thread.start();
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
     * Returns the {@link ReadOnlyBooleanProperty} which is true when a query is in progress.
     *
     * @return the {@link ReadOnlyBooleanProperty} which is true when a query is in progress
     */
    public ReadOnlyBooleanProperty getQueryingProperty() {
        return queryingProperty;
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
