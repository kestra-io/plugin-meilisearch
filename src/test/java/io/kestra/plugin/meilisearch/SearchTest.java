package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KestraTest
class SearchTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of(
            "query", "carlo",
            "index", "test",
            "url", "http://localhost:7700",
            "key", "MASTER_KEY"
        ));

        Search task = Search.builder()
            .id("{{query}}")
            .index("{{index}}")
            .url("{{url}}")
            .key("{{key}}")
            .build();

        Search.Output runOutput = task.run(runContext);

        assertTrue(runOutput.getResult().isEmpty());
    }
}