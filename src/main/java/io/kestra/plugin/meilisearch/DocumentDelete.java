package io.kestra.plugin.meilisearch;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.TaskError;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete documents from Meilisearch",
    description = "Deletes one, multiple, or all documents, or documents matching a filter expression from a Meilisearch index using the delete documents API."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a single document by ID",
            full = true,
            code = """
                id: meilisearch_delete_single
                namespace: company.team

                tasks:
                  - id: delete_document
                    type: io.kestra.plugin.meilisearch.DocumentDelete
                    url: "http://localhost:7700"
                    key: "{{ secret('MEILISEARCH_MASTER_KEY') }}"
                    index: "movies"
                    documentId: "123"
                """
        ),
        @Example(
            title = "Delete multiple documents by ID list",
            full = true,
            code = """
                id: meilisearch_delete_batch
                namespace: company.team

                tasks:
                  - id: delete_documents
                    type: io.kestra.plugin.meilisearch.DocumentDelete
                    url: "http://localhost:7700"
                    key: "{{ secret('MEILISEARCH_MASTER_KEY') }}"
                    index: "movies"
                    documentIds:
                      - "123"
                      - "456"
                """
        ),
        @Example(
            title = "Delete documents by filter",
            full = true,
            code = """
                id: meilisearch_delete_by_filter
                namespace: company.team

                tasks:
                  - id: delete_filtered
                    type: io.kestra.plugin.meilisearch.DocumentDelete
                    url: "http://localhost:7700"
                    key: "{{ secret('MEILISEARCH_MASTER_KEY') }}"
                    index: "movies"
                    filter: "genre = 'horror'"
                """
        )
    }
)
public class DocumentDelete extends AbstractMeilisearchConnection implements RunnableTask<DocumentDelete.Output> {
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofMinutes(5);
    private static final int WAIT_INTERVAL_MS = 500;

    @NotNull
    @Schema(title = "Index", description = "Name of the Meilisearch index.")
    @PluginProperty(group = "main")
    private Property<String> index;

    @Schema(title = "Document ID", description = "Identifier of a single document to delete.")
    @PluginProperty(group = "main")
    private Property<String> documentId;

    @Schema(title = "Document IDs", description = "List of document identifiers to delete.")
    @PluginProperty(group = "main")
    private Property<List<String>> documentIds;

    @Schema(title = "Filter", description = "Filter expression matching documents to delete.")
    @PluginProperty(group = "main")
    private Property<String> filter;

    @Schema(title = "Delete all", description = "Whether to delete all documents in the index. Defaults to false.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<Boolean> deleteAll = Property.ofValue(false);

    @Schema(title = "Wait for indexing", description = "Whether to wait for the deletion task to complete; the run fails if the task fails. Disable for fire-and-forget behavior.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Boolean> waitForIndexing = Property.ofValue(true);

    @Schema(title = "Wait timeout", description = "Maximum time to wait for the deletion task when `waitForIndexing` is enabled.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Duration> waitTimeout = Property.ofValue(DEFAULT_WAIT_TIMEOUT);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        Client client = this.createClient(runContext);
        String renderedIndex = runContext.render(this.index).as(String.class).orElseThrow();
        Index searchIndex = client.index(renderedIndex);

        Boolean renderedDeleteAll = runContext.render(this.deleteAll).as(Boolean.class).orElse(false);
        String renderedDocumentId = this.documentId != null ? runContext.render(this.documentId).as(String.class).orElse(null) : null;
        List<String> renderedDocumentIds = this.documentIds != null ? runContext.render(this.documentIds).asList(String.class) : null;
        String renderedFilter = this.filter != null ? runContext.render(this.filter).as(String.class).orElse(null) : null;

        TaskInfo taskInfo;
        if (Boolean.TRUE.equals(renderedDeleteAll)) {
            taskInfo = searchIndex.deleteAllDocuments();
        } else if (renderedDocumentId != null && !renderedDocumentId.isBlank()) {
            taskInfo = searchIndex.deleteDocument(renderedDocumentId);
        } else if (renderedDocumentIds != null && !renderedDocumentIds.isEmpty()) {
            taskInfo = searchIndex.deleteDocuments(renderedDocumentIds);
        } else if (renderedFilter != null && !renderedFilter.isBlank()) {
            taskInfo = searchIndex.deleteDocumentsByFilter(renderedFilter);
        } else {
            throw new IllegalArgumentException("At least one of 'documentId', 'documentIds', 'filter', or 'deleteAll: true' must be specified.");
        }

        Integer taskUid = taskInfo.getTaskUid();

        if (runContext.render(this.waitForIndexing).as(Boolean.class).orElse(true)) {
            int timeoutMs = (int) runContext.render(this.waitTimeout).as(Duration.class).orElse(DEFAULT_WAIT_TIMEOUT).toMillis();
            searchIndex.waitForTask(taskUid, timeoutMs, WAIT_INTERVAL_MS);
            var task = searchIndex.getTask(taskUid);
            if (TaskStatus.FAILED.equals(task.getStatus()) || TaskStatus.CANCELED.equals(task.getStatus())) {
                TaskError error = task.getError();
                throw new RuntimeException(String.format(
                    "Meilisearch deletion task %d on index '%s' ended with status %s%s",
                    taskUid,
                    renderedIndex,
                    task.getStatus(),
                    error != null ? ": " + error.getMessage() + " (" + error.getCode() + ")" : ""
                ));
            }
        }

        logger.info("Successfully initiated document deletion on index '{}' with task UID {}", renderedIndex, taskUid);

        return Output.builder()
            .taskUid(taskUid)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Task UID", description = "UID of the Meilisearch deletion task enqueued.")
        private final Integer taskUid;
    }
}
