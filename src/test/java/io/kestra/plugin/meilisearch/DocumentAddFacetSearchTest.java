package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.model.Settings;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.kestra.plugin.meilisearch.MeilisearchTestUtils.getJsonTestData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@KestraTest
class DocumentAddFacetSearchTest {
    private static final String FACET_SEARCH_INDEX = "testFacetSearch";
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testDocumentAddAndFacetSearch() throws Exception {
        final String facetName = "genres";
        final String facetQuery = "fiction";
        final String[] filters = new String[]{"rating > 3"};

        String documents = getJsonTestData(
            Map.of(
                "id", "1",
                "title", "Film 1",
                "genres", new JSONArray("[\"Fiction\", \"Drama\"]"),
                "rating", 10
            ),
            Map.of(
                "id", "2",
                "title", "Film 2",
                "genres", new JSONArray("[\"Fiction\"]"),
                "rating", 7
            ),
            Map.of(
                "id", "3",
                "title", "Film 3",
                "genres", new JSONArray("[\"Drama\"]"),
                "rating", 5
            ),
            Map.of(
                "id", "4",
                "title", "Film 4",
                "genres", new JSONArray("[\"Fiction\"]"),
                "rating", 2
            ),
            Map.of(
                "id", "5",
                "title", "Film 5",
                "genres", new JSONArray("[\"Fiction\"]"),
                "rating", 12
            )
        );

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentAdd documentAdd = DocumentAdd.builder()
            .from(documents)
            .index(FACET_SEARCH_INDEX)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext facetSearchRunContext = runContextFactory.of(ImmutableMap.of());

        FacetSearch facetSearch = FacetSearch.builder()
            .facetName(facetName)
            .facetQuery(facetQuery)
            .filters(filters)
            .index(FACET_SEARCH_INDEX)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        FacetSearch.Output facetSearchOutput = facetSearch.run(facetSearchRunContext);

        assertThat(facetSearchOutput.getTotalHits(), is(3.0));
    }

    @Test
    void testFacetSearchEmptyHits() throws Exception {
        final String facetName = "genres";
        final String facetQuery = "fiction";
        final String[] filters = new String[]{"rating > 1000"}; //Set rating high for empty results

        RunContext facetSearchRunContext = runContextFactory.of(ImmutableMap.of());

        FacetSearch facetSearch = FacetSearch.builder()
            .facetName(facetName)
            .facetQuery(facetQuery)
            .filters(filters)
            .index(FACET_SEARCH_INDEX)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        FacetSearch.Output searchOutput = facetSearch.run(facetSearchRunContext);

        assertThat(searchOutput.getTotalHits(), is(0.0));
    }

    @BeforeAll
    static void setupFilterableFields() {
        Settings settings = new Settings();
        settings.setFilterableAttributes(new String[] {"genres", "rating"});

        var meilisearchClient = new Client(new Config("http://localhost:7700", "MASTER_KEY"));
        meilisearchClient.index(FACET_SEARCH_INDEX).updateSettings(settings);
    }
}