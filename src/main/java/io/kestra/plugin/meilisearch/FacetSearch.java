package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.FacetSearchRequest;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.FacetSearchable;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
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
    title = "FacetSearch",
    description = """
        Perform a facet [search](https://www.meilisearch.com/docs/reference/api/facet_search) from a Meilisearch DB.
        WARNING: make sure to set the [filterable attributes](https://www.meilisearch.com/docs/learn/filtering_and_sorting/search_with_facet_filters#configure-facet-index-settings) before.
        """
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Sample facet search",
            code = {
                """
                    facetQuery: "fiction",
                    facetName: "genre",
                    filters:
                        -"rating > 3"
                    url: "http://localhost:7700",
                    key: "MASTER_KEY",
                    index: "movies"

                    id: meilisearch-facet-search-flow
                        namespace: company.team

                    variables:
                      index: movies
                      facetQuery: fiction
                      facetName: genre
                      host: http://172.18.0.3:7700/

                    tasks:
                      - id: facet_search_documents
                        type: io.kestra.plugin.meilisearch.FacetSearch
                        index: {{ vars.index }}
                        facetQuery: {{ vars.facetQuery }}
                        facetName: {{ vars.facetName }}
                        filters:
                            - "rating > 3"
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
public class FacetSearch extends AbstractMeilisearchConnection implements RunnableTask<FacetSearch.Output> {
    @PluginProperty(dynamic = true)
    private Property<String> index;

    @PluginProperty(dynamic = true)
    private Property<String> facetName;

    @PluginProperty(dynamic = true)
    private Property<String> facetQuery;

    @PluginProperty(dynamic = true)
    private List<String> filters;

    @Override
    public FacetSearch.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);
        Index searchIndex = client.index(index.as(runContext, String.class));

        FacetSearchRequest fsr = FacetSearchRequest.builder()
            .facetName(facetName.as(runContext, String.class))
            .facetQuery(facetQuery.as(runContext, String.class))
            .filter(runContext.render(filters).toArray(new String[]{}))
            .build();

        var results = searchIndex.facetSearch(fsr);

        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (var output = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            Flux<FacetSearchable> hitFlux = Flux.just(results);
            FileSerde.writeAll(output, hitFlux).blockOptional();

            return FacetSearch.Output.builder()
                .uri(runContext.storage().putFile(tempFile))
                .totalHits((long) results.getFacetHits().size())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "FacetSearch results"
        )
        private final URI uri;
        private final Long totalHits;
    }
}