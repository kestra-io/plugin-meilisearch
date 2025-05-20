package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.model.Settings;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.FileSerde;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.tenant.TenantService;
import io.kestra.core.utils.IdUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@KestraTest
class DocumentAddFacetSearchTest {
    private static final String FACET_SEARCH_INDEX = "testFacetSearch";
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void testDocumentAddAndFacetSearch() throws Exception {
        //Search all movies which have "fiction" in "genres" and with "rating" greater than 3
        final String facetName = "genres";
        final String facetQuery = "fiction";
        final List<String> filters = List.of("rating > 3");

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("examples/facetSearchMovies");

        URI uri = storageInterface.put(TenantService.MAIN_TENANT, null, URI.create("/" + IdUtils.create() + ".ion"), inputStream);
        Data<Map> data = Data.<Map>builder().fromURI(Property.of(uri)).build();

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentAdd documentAdd = TestUtils.createDocumentAdd(data, FACET_SEARCH_INDEX);
        documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext facetSearchRunContext = runContextFactory.of(ImmutableMap.of());
        FacetSearch facetSearch = TestUtils.createFacetSearch(facetName, facetQuery, filters, FACET_SEARCH_INDEX);
        FacetSearch.Output facetSearchOutput = facetSearch.run(facetSearchRunContext);

        assertThat(facetSearchOutput.getTotalHits(), is(1L));

        BufferedReader searchInputStream = new BufferedReader(new InputStreamReader(storageInterface.get(null, null, facetSearchOutput.getUri())));
        List<Map<String, Object>> result = new ArrayList<>();
        FileSerde.reader(searchInputStream, r -> result.add((Map<String, Object>) r));

        Map<String, Object> faceResultMap = result.getFirst();
        assertThat(faceResultMap.get("facetQuery"), is("fiction"));
        List<Map<String, Object>> facetHits = ((List) faceResultMap.get("facetHits"));
        assertThat(facetHits.getFirst().get("count"), is(3.0));
    }

    @Test
    void testFacetSearchEmptyHits() throws Exception {
        final String facetName = "genres";
        final String facetQuery = "fiction";
        final List<String> filters = List.of("rating > 1000"); //Set rating high for empty results

        RunContext facetSearchRunContext = runContextFactory.of(ImmutableMap.of());

        FacetSearch facetSearch = TestUtils.createFacetSearch(facetName, facetQuery, filters, FACET_SEARCH_INDEX);

        FacetSearch.Output searchOutput = facetSearch.run(facetSearchRunContext);

        assertThat(searchOutput.getTotalHits(), is(0L));
    }

    @BeforeAll
    static void setupFilterableFields() {
        Settings settings = new Settings();
        settings.setFilterableAttributes(new String[] {"genres", "rating"});

        var meilisearchClient = new Client(new Config("http://localhost:7700", "MASTER_KEY"));
        meilisearchClient.index(FACET_SEARCH_INDEX).updateSettings(settings);
    }
}