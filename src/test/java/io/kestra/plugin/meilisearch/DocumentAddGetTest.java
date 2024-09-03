package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.json.JSONArray;
import java.util.Map;

import static io.kestra.plugin.meilisearch.MeilisearchTestUtils.getJsonTestData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@KestraTest
class DocumentAddGetTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testAddGetJsonDocument() throws Exception {
        final String id = "12";
        final String index = "testAdd";

        String documents = getJsonTestData(
            Map.of(
                "id", id,
                "title", "Notebook",
                "genres", new JSONArray("[\"Romance\",\"Drama\"]")
                )
        );

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentAdd documentAdd = DocumentAdd.builder()
            .from(documents)
            .index(index)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext getRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentGet documentGet = DocumentGet.builder()
            .id(id)
            .index(index)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        DocumentGet.Output getOutput = documentGet.run(getRunContext);

        Map<String, Object> doc = (Map<String, Object>) getOutput.getDocument();
        assertThat(doc.get("id"), is(id));
    }
}