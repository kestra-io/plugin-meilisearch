package io.kestra.plugin.meilisearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

import static io.kestra.core.utils.Rethrow.throwFunction;

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
                    id: meilisearch-add-flow
                        namespace: company.team

                    variables:
                      host: http://172.18.0.3:7700/

                    tasks:
                      - id: http_download
                        type: io.kestra.plugin.core.http.Download
                        uri: https://pokeapi.co/api/v2/pokemon/jigglypuff

                      - id: to_ion
                        type: io.kestra.plugin.serdes.json.JsonToIon
                        from: "{{ outputs.http_download.uri }}"

                      - id: add
                        type: io.kestra.plugin.meilisearch.DocumentAdd
                        index: "pokemon"
                        url: "{{ vars.host }}"
                        key: "MASTER_KEY"
                        data: "{{ outputs.to_ion.uri }}"
                """
            }
        )
    }
)
public class DocumentAdd extends AbstractMeilisearchConnection implements RunnableTask<DocumentAdd.Output> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Schema(
        title = "The file to upload"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private Data<Map> data;

    @PluginProperty(dynamic = true)
    @NotNull
    private Property<String> index;

    @Override
    public DocumentAdd.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);
        Index documentIndex = client.index(this.index.as(runContext, String.class));

        Integer count = this.data.flux(runContext, Map.class, map -> map)
            .map(throwFunction(row -> {
                documentIndex.addDocuments(MAPPER.writeValueAsString((Map<String,Object>) row));
                return 1;
            }))
            .reduce(Integer::sum)
            .blockOptional()
            .orElse(0);

        runContext.metric(Counter.of("documentAdded", count));

        return Output.builder()
            .outputMessage("Document successfully added to index " + this.index.as(runContext, String.class))
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
}
