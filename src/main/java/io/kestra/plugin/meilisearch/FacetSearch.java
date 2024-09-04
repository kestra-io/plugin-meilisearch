package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.FacetSearchRequest;
import com.meilisearch.sdk.Index;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
    description = "Perform a facet [search](https://www.meilisearch.com/docs/reference/api/facet_search) from a Meilisearch DB"
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
                """
            }
        )
    }
)
public class FacetSearch extends AbstractMeilisearchConnection implements RunnableTask<FacetSearch.Output> {
    @PluginProperty(dynamic = true)
    private String index;

    @PluginProperty(dynamic = true)
    private String facetName;

    @PluginProperty(dynamic = true)
    private String facetQuery;

    @PluginProperty(dynamic = true)
    private List<String> filters;

    @Override
    public FacetSearch.Output run(RunContext runContext) throws Exception {
        Client client = this.createClient(runContext);
        Index searchIndex = client.index(runContext.render(index));

        FacetSearchRequest fsr = FacetSearchRequest.builder()
            .facetName(runContext.render(facetName))
            .facetQuery(runContext.render(facetQuery))
            .filter(runContext.render(filters).toArray(new String[]{}))
            .build();

        ArrayList<HashMap<String, Object>> hits = searchIndex.facetSearch(fsr).getFacetHits();

        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            oos.writeObject(Optional.of(hits).orElse(new ArrayList<>()));
        }

        double hitSize = 0.0;
        if(!hits.isEmpty()) {
            hitSize = hits.getFirst() != null
                ? (Double) hits.getFirst().getOrDefault("count", 0.0) : 0.0;
        }

        return Output.builder()
            .uri(runContext.storage().putFile(tempFile))
            .totalHits(hitSize)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Add document is successful or not",
            description = "Describe if the add of the document was successful or not and the reason"
        )
        private final URI uri;
        private final Double totalHits;
    }
}
