package meilisearch.plugin;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.SearchResult;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Search Document",
    description = "Search Document from Meilisearch"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Search Document from Meilisearch",
            code = {
                """
                    query: \\"query string to retrieve the doc\\",
                    url: \\"url of the meilisearch server\\",
                    key: \\"masterKey of the meilisearch server\\",
                    index: \\"index\\",
                """
            }
        )
    }
)

/**
 * Using Docker to open Meilisearch
 * Command on Windows : docker run -it --rm -p 7700:7700 -e MEILI_MASTER_KEY='MASTER_KEY' -v "$(pwd)/meili_data:/meili_data" getmeili/meilisearch:v1.9
 */
public class Search extends Task implements RunnableTask<Search.Output> {
    @Schema(
        title = "Basic Search",
        description = "Basic search providing URL and credentials and specific index"
    )
    @PluginProperty(dynamic = true)
    private String query;

    @PluginProperty(dynamic = true)
    private String url;

    @PluginProperty(dynamic = true)
    private String key;

    @PluginProperty(dynamic = true)
    private String index;

    @Override
    public Search.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String queryRender = runContext.render(query);
        String urlRender = runContext.render(url);
        String keyRender = runContext.render(key);
        String indexRender = runContext.render(index);

        logger.debug(queryRender);
        logger.debug(urlRender);

        Client client = new Client(new Config(urlRender, keyRender));
        SearchResult results;
        try {
            Index index = client.index(indexRender);
            results = index.search(queryRender);
        } catch (MeilisearchException e) {
            logger.debug(e.getMessage());
            return Output.builder()
                .outputMessage(e.getMessage())
                .success(false)
                .build();
        }

        if(results == null) {
            return Output.builder()
                .outputMessage("Failed search" + indexRender)
                .success(false)
                .build();
        }

        return Output.builder()
            .outputMessage("Search successful")
            .result(results.getHits())
            .success(true)
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
        private final boolean success;
        private final ArrayList<HashMap<String, Object>> result;
    }
}
