package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get Document",
    description = "Get a [document](https://www.meilisearch.com/docs/reference/api/documents#get-documents-with-get) from Meilisearch using id and index"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Get Document from Meilisearch",
            code = {
                """
                    id: "11",
                    url: "http://localhost:7700",
                    key: "MASTER_KEY",
                    index: "movies"
                """
            }
        )
    }
)

/**
 * Using Docker to open Meilisearch
 * Command on Windows : docker run -it --rm -p 7700:7700 -e MEILI_MASTER_KEY='MASTER_KEY' -v "$(pwd)/meili_data:/meili_data" getmeili/meilisearch:v1.9
 */
public class DocumentGet extends AbstractMeilisearchConnection implements RunnableTask<DocumentGet.Output> {
    @Schema(
        title = "Get document",
        description = "Get document from meilisearch providing URL and credentials and specific index"
    )
    @PluginProperty(dynamic = true)
    private String id;

    @PluginProperty(dynamic = true)
    private String index;

    @Override
    public DocumentGet.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);
        Index searchIndex = client.index(runContext.render(index));
        Map output = searchIndex.getDocument(runContext.render(id), Map.class);

        return Output.builder()
            .document(output)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Document retrieved",
            description = "Return document from id"
        )
        private final Map document;
    }
}
