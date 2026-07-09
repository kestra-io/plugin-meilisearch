package io.kestra.plugin.meilisearch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.TaskError;
import com.meilisearch.sdk.model.TaskStatus;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static io.kestra.core.utils.Rethrow.throwFunction;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Add documents to Meilisearch",
    description = "Adds one or multiple documents to a Meilisearch index using the [add-or-replace API](https://www.meilisearch.com/docs/reference/api/documents#add-or-replace-documents). Documents are read from the `from` source, rendered by Kestra, and sent in batches; by default the task waits for the indexing tasks to complete and fails if any of them fails. Requires index URL and API key."
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
public class DocumentAdd extends AbstractMeilisearchConnection implements RunnableTask<DocumentAdd.Output>, Data.From {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofMinutes(5);
    private static final int WAIT_INTERVAL_MS = 500;

    @NotNull
    @PluginProperty(group = "main")
    private Object from;

    @NotNull
    @Schema(title = "Index", description = "Name of the Meilisearch index to add documents to.")
    @PluginProperty(group = "main")
    private Property<String> index;

    @Schema(title = "Batch size", description = "Number of documents sent to Meilisearch per request; each batch is enqueued as one indexing task.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Integer> batchSize = Property.ofValue(DEFAULT_BATCH_SIZE);

    @Schema(title = "Wait for indexing", description = "Whether to wait for the enqueued indexing tasks to complete; the run fails if a task fails. Disable for fire-and-forget behavior.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Boolean> waitForIndexing = Property.ofValue(true);

    @Schema(title = "Wait timeout", description = "Maximum time to wait for each indexing task when `waitForIndexing` is enabled.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Duration> waitTimeout = Property.ofValue(DEFAULT_WAIT_TIMEOUT);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        Client client = this.createClient(runContext);
        var renderedIndex = runContext.render(this.index).as(String.class).orElseThrow();
        Index documentIndex = client.index(renderedIndex);
        var renderedBatchSize = runContext.render(this.batchSize).as(Integer.class).orElse(DEFAULT_BATCH_SIZE);

        List<Integer> taskUids = new ArrayList<>();
        Integer count = Data.from(from).read(runContext)
            .buffer(renderedBatchSize)
            .map(throwFunction(batch ->
            {
                taskUids.add(documentIndex.addDocuments(MAPPER.writeValueAsString(batch)).getTaskUid());
                return batch.size();
            }))
            .reduce(Integer::sum)
            .blockOptional()
            .orElse(0);

        if (runContext.render(this.waitForIndexing).as(Boolean.class).orElse(true)) {
            int timeoutMs = (int) runContext.render(this.waitTimeout).as(Duration.class).orElse(DEFAULT_WAIT_TIMEOUT).toMillis();
            for (Integer taskUid : taskUids) {
                documentIndex.waitForTask(taskUid, timeoutMs, WAIT_INTERVAL_MS);
                var task = documentIndex.getTask(taskUid);
                if (TaskStatus.FAILED.equals(task.getStatus()) || TaskStatus.CANCELED.equals(task.getStatus())) {
                    TaskError error = task.getError();
                    throw new RuntimeException(String.format(
                        "Meilisearch indexing task %d on index '%s' ended with status %s%s",
                        taskUid,
                        renderedIndex,
                        task.getStatus(),
                        error != null ? ": " + error.getMessage() + " (" + error.getCode() + ")" : ""
                    ));
                }
            }
        }

        runContext.metric(Counter.of("documentAdded", count));
        logger.info("Successfully added {} documents to index {} in {} batches", count, renderedIndex, taskUids.size());

        return Output.builder()
            .taskUids(taskUids)
            .documentsAdded(count)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Task UIDs", description = "UIDs of the Meilisearch indexing tasks enqueued for the added documents, one per batch.")
        private final List<Integer> taskUids;
        @Schema(title = "Documents added", description = "Number of documents sent to Meilisearch.")
        private final Integer documentsAdded;
    }
}
