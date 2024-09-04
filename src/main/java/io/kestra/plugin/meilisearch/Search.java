package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.SearchResult;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.io.*;
import java.net.URI;
import java.util.*;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Search Document",
    description = "Search Document from Meilisearch"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Search Document from Meilisearch",
            code = {
                """
                    query: \\"query string to retrieve the doc\\",
                    url: \\"url of the meilisearch server\\",
                    key: \\"masterKey of the meilisearch server\\",
                    index: \\"index\\",
                """
            }
        )
    }
)
public class Search extends AbstractMeilisearchConnection implements RunnableTask<Search.Output> {
    @Schema(
        title = "Basic Search",
        description = "Basic search providing URL and credentials and specific index"
    )
    @PluginProperty(dynamic = true)
    private String query;

    @PluginProperty(dynamic = true)
    private String index;

    @Override
    public Search.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);
        Index searchIndex = client.index(runContext.render(index));
        SearchResult results = searchIndex.search(runContext.render(query));
        ArrayList<HashMap<String, Object>> hits = results.getHits();

        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            oos.writeObject(Optional.of(hits).orElse(new ArrayList<>()));
        }

        return Output.builder()
            .uri(runContext.storage().putFile(tempFile))
            .totalHits(hits.size())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Add document is successful or not",
            description = "Describe if the add of the document was successful or not and the reason"
        )
        private final URI uri;
        private final int totalHits;
    }
}
