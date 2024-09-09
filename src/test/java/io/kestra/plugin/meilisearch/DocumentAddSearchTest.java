package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.utils.IdUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@KestraTest
class DocumentAddSearchTest {
    private static final String SEARCH_INDEX = "testSearch";
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void testDocumentAddAndSearch() throws Exception {
        final String pattern = "John";

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("examples/basicSearchName.json");

        URI uri = storageInterface.put(null, URI.create("/" + IdUtils.create() + ".ion"), inputStream);
        Data<Map> data = Data.<Map>builder().fromURI(Property.of(uri)).build();

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentAdd documentAdd = TestUtils.createDocumentAdd(data, SEARCH_INDEX);

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext searchRunContext = runContextFactory.of(ImmutableMap.of());

        Search search = TestUtils.createSearch(pattern, SEARCH_INDEX);

        Search.Output searchOutput = search.run(searchRunContext);

        assertThat(searchOutput.getTotalHits(), is(2));
    }

    @Test
    void testSearchEmptyHits() throws Exception {
        RunContext searchRunContext = runContextFactory.of(ImmutableMap.of());

        Search search = TestUtils.createSearch("randomPattern", SEARCH_INDEX);

        Search.Output searchOutput = search.run(searchRunContext);

        assertThat(searchOutput.getTotalHits(), is(0));
    }
}