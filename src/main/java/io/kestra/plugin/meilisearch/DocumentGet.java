package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;



@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get a document from Meilisearch.",
    description = "Get a json [document](https://www.meilisearch.com/docs/reference/api/documents#get-documents-with-get) from Meilisearch using id and index."
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Get Document from Meilisearch",
            full = true,
            code = {
                """
                id: meilisearch_get_flow
                namespace: company.team

                variables:
                  id: a123
                  index: pokemons
                  host: http://172.18.0.3:7700/

                tasks:
                  - id: get_document
                    type: io.kestra.plugin.meilisearch.DocumentGet
                    index: "{{ vars.index }}"
                    documentId: "{{ vars.id }}"
                    url: "{{ vars.host }}"
                    key: "{{ secret('MEILISEARCH_MASTER_KEY') }}"
                """
            }
        )
    }
)
public class DocumentGet extends AbstractMeilisearchConnection implements RunnableTask<DocumentGet.Output> {
    @NotNull
    @Schema(title = "Document ID")
    private Property<String> documentId;

    @NotNull
    @Schema(title = "Index", description = "Index of the collections you want to retrieve your document from")
    private Property<String> index;

    @Override
    public DocumentGet.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);

        Index searchIndex = client.index(runContext.render(this.index).as(String.class).orElseThrow());
        Map<String, Object> output = (Map<String, Object>) searchIndex.getDocument(runContext.render(this.documentId).as(String.class).orElseThrow(), Map.class);

        return Output.builder()
            .document(output)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "JSON Document",
            description = "Returned document as a JSON object"
        )
        private final Map<String, Object> document;
    }
}
