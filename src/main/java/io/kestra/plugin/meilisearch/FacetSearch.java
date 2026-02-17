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
    title = "Search facets in Meilisearch",
    description = """
        Runs a facet search on a Meilisearch index and writes the facet hits to an .ion file in Kestra storage. Facet attributes must be configured as filterable in the index settings; filters default to none.
        """
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Sample facet search",
            full = true,
            code = {
                """
                id: meilisearch_facet_search_flow
                namespace: company.team

                variables:
                  index: movies
                  facetQuery: fiction
                  facetName: genre
                  host: http://172.18.0.3:7700/

                tasks:
                  - id: facet_search_documents
                    type: io.kestra.plugin.meilisearch.FacetSearch
                    index: "{{ vars.index }}"
                    facetQuery: "{{ vars.facetQuery }}"
                    facetName: "{{ vars.facetName }}"
                    filters:
                      - "rating > 3"
                    url: "{{ vars.host }}"
                    key: "{{ secret('MEILISEARCH_MASTER_KEY') }}"

                  - id: to_json
                    type: io.kestra.plugin.serdes.json.IonToJson
                    from: "{{ outputs.facet_search_documents.uri }}"
                """
            }
        )
    }
)
public class FacetSearch extends AbstractMeilisearchConnection implements RunnableTask<FacetSearch.Output> {

    @Schema(title = "Index", description = "Name of the Meilisearch index to search.")
    @NotNull
    private Property<String> index;

    @Schema(title = "Facet name", description = "Facet attribute configured as filterable (e.g., facetName \"genre\" on a film index).")
    @NotNull
    private Property<String> facetName;

    @Schema(title = "Facet query", description = "Query string applied to the specified facet; optional.")
    private Property<String> facetQuery;

    @Schema(title = "Filters", description = "List of Meilisearch filters applied to the facet search; defaults to an empty list.")
    @Builder.Default
    private Property<List<String>> filters = Property.ofValue(new ArrayList<>());

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
        @Schema(title = "Output URI", description = "URI in Kestra storage to the .ion file with facet search results.")
        private final URI uri;
        @Schema(title = "Total hits", description = "Number of facet hits returned by the request.")
        private final Long totalHits;
    }
}
