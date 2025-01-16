package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.FacetSearchRequest;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.FacetSearchable;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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

    @Schema(title = "Index", description = "Index of the collection you want to search in")
    @NotNull
    private Property<String> index;

    @Schema(title = "Facet name", description = "Name of the facet you wan to perform a search on (ex: facetName: \"genre\" on a film collection)")
    @NotNull
    private Property<String> facetName;

    @Schema(title = "Facet query", description = "Query that will be used on the specified facetName")
    private Property<String> facetQuery;

    @Schema(title = "Filters", description = "Additional filters to apply to your facet search")
    @Builder.Default
    private Property<List<String>> filters = Property.of(new ArrayList<>());

    @Override
    public FacetSearch.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);
        Index searchIndex = client.index(runContext.render(this.index).as(String.class).orElseThrow());

        FacetSearchRequest fsr = FacetSearchRequest.builder()
            .facetName(runContext.render(this.facetName).as(String.class).orElseThrow())
            .facetQuery(runContext.render(this.facetQuery).as(String.class).orElse(null))
            .filter(runContext.render(this.filters).asList(String.class).toArray(new String[]{}))
            .build();

        var result = searchIndex.facetSearch(fsr);

        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (var output = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            Flux<FacetSearchable> hitFlux = Flux.just(result);
            FileSerde.writeAll(output, hitFlux).blockOptional();

            return FacetSearch.Output.builder()
                .uri(runContext.storage().putFile(tempFile))
                .totalHits((long) result.getFacetHits().size())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "URI to output", description = "Results URI to an Amazon .ion file")
        private final URI uri;
        @Schema(title = "Hits number", description = "Number of items hit by the facet search request")
        private final Long totalHits;
    }
}
