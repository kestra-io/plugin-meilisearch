package io.kestra.plugin.meilisearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import static io.kestra.core.utils.Rethrow.throwFunction;

@Slf4j
@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Add a document to Meilisearch.",
    description = "Add one or multiple [documents](https://www.meilisearch.com/docs/reference/api/documents#add-or-replace-documents) to a Meilisearch DB."
)
@Plugin(
    examples = {
        @Example(
            title = "Add Document to Meilisearch",
            full = true,
            code = {
                """
                    id: meilisearch_add_flow
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
                        key: "{{ secret('MEILISEARCH_MASTER_KEY') }}"
                        from: "{{ outputs.to_ion.uri }}"
                    """
            }
        )
    },
    metrics = {
        @Metric(
            name = "documentAdded",
            description = "The number of documents added to Meilisearch",
            type = Counter.TYPE
        )
    }
)
public class DocumentAdd extends AbstractMeilisearchConnection implements RunnableTask<VoidOutput>, Data.From {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @NotNull
    private Object from;

    @NotNull
    @Schema(title = "Index", description = "Index of the collection you want to add documents to")
    private Property<String> index;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        Client client = this.createClient(runContext);
        var renderedIndex = runContext.render(this.index).as(String.class).orElseThrow();
        Index documentIndex = client.index(renderedIndex);

        Integer count = Data.from(from).read(runContext)
            .map(throwFunction(row -> {
                documentIndex.addDocuments(MAPPER.writeValueAsString(row));
                return 1;
            }))
            .reduce(Integer::sum)
            .blockOptional()
            .orElse(0);

        runContext.metric(Counter.of("documentAdded", count));
        logger.info("Successfully added documents to index {}", renderedIndex);

        return null;
    }
}
