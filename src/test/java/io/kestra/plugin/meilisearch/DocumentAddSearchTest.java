package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static io.kestra.plugin.meilisearch.MeilisearchTestUtils.getJsonTestData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@KestraTest
class DocumentAddSearchTest {
    private static final String SEARCH_INDEX = "testSearch";
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testDocumentAddAndSearch() throws Exception {
        final String pattern = "John";

        String documents = getJsonTestData(
            Map.of(
                "id", "testSearch1",
                "name", pattern + " Doe"
            ),
            Map.of(
                "id", "testSearch2",
                "name",  pattern + "Hoe"
            ),
            Map.of(
                "id", "testSearch3",
                "name", "Bryan Smith"
            )
        );

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentAdd documentAdd = DocumentAdd.builder()
            .from(documents)
            .index(SEARCH_INDEX)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext searchRunContext = runContextFactory.of(ImmutableMap.of());

        Search search = Search.builder()
            .query(pattern)
            .index(SEARCH_INDEX)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        Search.Output searchOutput = search.run(searchRunContext);

        assertThat(searchOutput.getTotalHits(), is(2));
    }

    @Test
    void testSearchEmptyHits() throws Exception {
        RunContext searchRunContext = runContextFactory.of(ImmutableMap.of());

        Search search = Search.builder()
            .query("random")
            .index(SEARCH_INDEX)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        Search.Output searchOutput = search.run(searchRunContext);

        assertThat(searchOutput.getTotalHits(), is(0));
    }
}