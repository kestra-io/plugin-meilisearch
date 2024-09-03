package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;
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
    title = "Add Document",
    description = "Add Document to Meilisearch"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Add Document to Meilisearch",
            code = {
                """
                    document: \\"document to be added in JSON\\",
                    url: \\"url of the meilisearch server\\",
                    key: \\"masterKey of the meilisearch server\\",
                    index: \\"index where we want to add the document\\"
                """
            }
        )
    }
)

/**
 * Using Docker to open Meilisearch
 * Command on Windows : docker run -it --rm -p 7700:7700 -e MEILI_MASTER_KEY='MASTER_KEY' -v "$(pwd)/meili_data:/meili_data" getmeili/meilisearch:v1.9
 */
public class DocumentAdd extends AbstractMeilisearchConnection implements RunnableTask<DocumentAdd.Output> {
    @Schema(
        title = "The file to upload"
    )
    @PluginProperty(dynamic = true)
    private String from;

    @PluginProperty(dynamic = true)
    private String index;

    @Override
    public DocumentAdd.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        MeilisearchFactory factory = this.meilisearchFactory(runContext);

        try {
            Client client = factory.getMeilisearchClient();
            Index searchIndex = client.index(runContext.render(index));
            searchIndex.addDocuments(runContext.render(from));

            return Output.builder()
                .outputMessage("Document successfully added to index " + runContext.render(index))
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
            title = "Add document is successful or not",
            description = "Describe if the add of the document was successful or not and the reason"
        )
        private final String outputMessage;
    }
}
