package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get Document",
    description = "Get Document from Meilisearch"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Get Document from Meilisearch",
            code = {
                """
                    id: \\"document id we want to retrieve\\",
                    url: \\"url of the meilisearch server\\",
                    key: \\"masterKey of the meilisearch server\\",
                    index: \\"index provided\\"
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
        Logger logger = runContext.logger();

        MeilisearchFactory factory = this.meilisearchFactory(runContext);

        try {
            Client client = factory.getMeilisearchClient();
            Index searchIndex = client.index(runContext.render(index));
            Object output = searchIndex.getDocument(runContext.render(id), Object.class);

            return Output.builder()
                .document(output)
                .build();

        } catch (MeilisearchException e) {
            logger.debug(e.getMessage());
            throw e;
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Document retrieved",
            description = "Return document from id"
        )
        private final Object document;
    }
}
