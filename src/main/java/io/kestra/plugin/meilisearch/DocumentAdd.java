package io.kestra.plugin.meilisearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Add Document",
    description = "Add one or multiple [documents](https://www.meilisearch.com/docs/reference/api/documents#add-or-replace-documents) to a Meilisearch DB"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Add Document to Meilisearch",
            code = {
                """
                    document: {{inputs.file}},
                    url: "http://localhost:7700",
                    key: "MASTER_KEY",
                    index: "movies"
                """
            }
        )
    }
)
public class DocumentAdd extends AbstractMeilisearchConnection implements RunnableTask<DocumentAdd.Output> {
    @Schema(
        title = "The file to upload"
    )
    @PluginProperty(dynamic = true)
    private Object from;

    @PluginProperty(dynamic = true)
    private String index;

    @Override
    public DocumentAdd.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);
        Index searchIndex = client.index(runContext.render(index));

        String documents = this.source(runContext);
        searchIndex.addDocuments(documents);

        return Output.builder()
            .outputMessage("Document successfully added to index " + runContext.render(index))
            .build();
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

    @SuppressWarnings("unchecked")
    private String source(RunContext runContext) throws IllegalVariableEvaluationException, JsonProcessingException, URISyntaxException, IOException {
        if (this.from instanceof String valueStr) {
            URI from = new URI(runContext.render(runContext.render(valueStr)));
            try(BufferedReader br = new BufferedReader(new InputStreamReader(runContext.storage().getFile(from)), FileSerde.BUFFER_SIZE)) {
                return br.lines().collect(Collectors.joining());
            }
        } else if (this.from instanceof Map valueMap) {
            return new ObjectMapper().writeValueAsString(runContext.render(valueMap));
        }
        throw new IllegalVariableEvaluationException("Invalid value type '" + this.from.getClass() + "'");
    }
}
