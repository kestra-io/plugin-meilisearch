package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import meilisearch.plugin.DocumentAdd;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@KestraTest
class DocumentAddTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of(
            "document", "{\"nom\":\"John\",\"prenom\":\"Doe\"}",
            "index", "test",
            "url", "http://localhost:7700",
            "key", "MASTER_KEY"
        ));

        DocumentAdd task = DocumentAdd.builder()
            .document("{{document}}")
            .index("{{index}}")
            .url("{{url}}")
            .key("{{key}}")
            .build();

        DocumentAdd.Output runOutput = task.run(runContext);

        assertFalse(runOutput.isSuccess());
    }
}