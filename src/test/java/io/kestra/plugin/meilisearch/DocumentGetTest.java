package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import meilisearch.plugin.DocumentAdd;
import meilisearch.plugin.DocumentGet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KestraTest
class DocumentGetTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of(
            "id", "movies",
            "index", "test",
            "url", "http://localhost:7700",
            "key", "MASTER_KEY"
        ));

        DocumentGet task = DocumentGet.builder()
            .id("{{id}}")
            .index("{{index}}")
            .url("{{url}}")
            .key("{{key}}")
            .build();

        DocumentGet.Output runOutput = task.run(runContext);

        assertNull(runOutput.getDocument());
    }
}