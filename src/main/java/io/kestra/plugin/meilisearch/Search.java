package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.SearchResult;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;

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
    description = "Perform a basic search query on a Meilisearch database with specific query and return the results in an .ion file"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            code = {
                """
                    id: meilisearch-search-flow
                        namespace: company.team

                    variables:
                      index: movies
                      query: "Lord of the Rings"
                      host: http://172.18.0.3:7700/

                    tasks:
                      - id: search_documents
                        type: io.kestra.plugin.meilisearch.Search
                        index: {{ vars.index }}
                        query: {{ vars.query }}
                        url: "{{ vars.host }}"
                        key: "MASTER_KEY"

                      - id: to_json
                        type: io.kestra.plugin.serdes.json.IonToJson
                        from: "{{ outputs.search_documents.uri }}"
                """
            }
        )
    }
)
public class Search extends AbstractMeilisearchConnection implements RunnableTask<Search.Output> {

    @PluginProperty(dynamic = true)
    private Property<String> query;

    @PluginProperty(dynamic = true)
    private Property<String> index;

    @Override
    public Search.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);
        Index searchIndex = client.index(index.as(runContext, String.class));
        SearchResult results = searchIndex.search(query.as(runContext, String.class));
        ArrayList<HashMap<String, Object>> hits = results.getHits();

        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();

        try (var output = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            Flux<Map> hitFlux = Flux.fromIterable(hits);
            Long count = FileSerde.writeAll(output, hitFlux).blockOptional().orElse(0L);

            return Output.builder()
                .uri(runContext.storage().putFile(tempFile))
                .totalHits(count)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Add document is successful or not",
            description = "Describe if the add of the document was successful or not and the reason"
        )
        private final URI uri;
        private final Long totalHits;
    }
}
