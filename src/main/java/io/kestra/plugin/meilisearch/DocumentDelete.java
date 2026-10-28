package io.kestra.plugin.meilisearch;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;

import com.meilisearch.sdk.model.TaskInfo;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
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
@Metric(name = "documentDeleted", type = "counter", description = "The number of documents deleted.")
@Schema(
    title = "Delete documents from Meilisearch",
    description = """
        Deletes one, multiple, or all documents, or documents matching a filter expression from a Meilisearch index.
        Exactly one target (`documentId`, `documentIds`, `filter`, or `deleteAll: true`) must be specified.
        When using `filter`, the referenced attributes must be configured in the index's `filterableAttributes` settings.
        """
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
        ),
        @Example(
            title = "Delete all documents in an index",
            full = true,
            code = """
                id: meilisearch_delete_all
                namespace: company.team

                tasks:
                  - id: purge_index
                    type: io.kestra.plugin.meilisearch.DocumentDelete
                    url: "http://localhost:7700"
                    key: "{{ secret('MEILISEARCH_MASTER_KEY') }}"
                    index: "movies"
                    deleteAll: true
                """
        )
    }
)
public class DocumentDelete extends AbstractMeilisearchConnection implements RunnableTask<DocumentDelete.Output> {

    @NotNull
    @Schema(title = "Index", description = "Name of the Meilisearch index.")
    @PluginProperty(group = "main")
    private Property<String> index;

    @Schema(
        title = "Document ID",
        description = """
            Identifier of a single document to delete. Exactly one target (`documentId`, `documentIds`, `filter`, or `deleteAll: true`) must be specified.
            """
    )
    @PluginProperty(group = "main")
    private Property<String> documentId;

    @Schema(
        title = "Document IDs",
        description = """
            List of document identifiers to delete. Exactly one target (`documentId`, `documentIds`, `filter`, or `deleteAll: true`) must be specified.
            """
    )
    @PluginProperty(group = "main")
    private Property<List<String>> documentIds;

    @Schema(
        title = "Filter",
        description = """
            Filter expression matching documents to delete. Exactly one target (`documentId`, `documentIds`, `filter`, or `deleteAll: true`) must be specified.
            Note: Filter expressions require the attributes used to be declared in the index's `filterableAttributes` settings.
            """
    )
    @PluginProperty(group = "main")
    private Property<String> filter;

    @Schema(
        title = "Delete all",
        description = """
            Whether to delete all documents in the index. Defaults to false.
            Must be explicitly set to true when deleting all documents. Exactly one target (`documentId`, `documentIds`, `filter`, or `deleteAll: true`) must be specified.
            """
    )
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<Boolean> deleteAll = Property.ofValue(false);

    @Schema(
        title = "Wait for indexing",
        description = "Whether to wait for the deletion task to complete; the run fails if the task fails. Disable for fire-and-forget behavior."
    )
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Boolean> waitForIndexing = Property.ofValue(true);

    @Schema(
        title = "Wait timeout",
        description = "Maximum time to wait for the deletion task when `waitForIndexing` is enabled."
    )
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Duration> waitTimeout = Property.ofValue(DEFAULT_WAIT_TIMEOUT);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        var client = this.createClient(runContext);
        var rIndex = runContext.render(this.index).as(String.class).orElseThrow(() -> new IllegalArgumentException("Index is required"));
        var searchIndex = client.index(rIndex);

        var rDeleteAll = runContext.render(this.deleteAll).as(Boolean.class).orElse(false);
        var rDocumentId = runContext.render(this.documentId).as(String.class).orElse(null);
        var rDocumentIds = runContext.render(this.documentIds).asList(String.class);
        var rFilter = runContext.render(this.filter).as(String.class).orElse(null);

        var isDeleteAll = Boolean.TRUE.equals(rDeleteAll);
        var hasDocumentId = rDocumentId != null && !rDocumentId.isBlank();
        var hasDocumentIds = !rDocumentIds.isEmpty();
        var hasFilter = rFilter != null && !rFilter.isBlank();

        var targetCount = (isDeleteAll ? 1 : 0)
            + (hasDocumentId ? 1 : 0)
            + (hasDocumentIds ? 1 : 0)
            + (hasFilter ? 1 : 0);

        if (targetCount != 1) {
            throw new IllegalArgumentException(
                "Exactly one of 'documentId', 'documentIds', 'filter', or 'deleteAll: true' must be specified, but found %d.".formatted(targetCount)
            );
        }

        TaskInfo taskInfo;
        if (isDeleteAll) {
            taskInfo = searchIndex.deleteAllDocuments();
        } else if (hasDocumentId) {
            taskInfo = searchIndex.deleteDocument(rDocumentId);
        } else if (hasDocumentIds) {
            taskInfo = searchIndex.deleteDocuments(rDocumentIds);
        } else {
            taskInfo = searchIndex.deleteDocumentsByFilter(rFilter);
        }

        Integer taskUid = taskInfo.getTaskUid();
        Integer deletedCount = null;

        if (runContext.render(this.waitForIndexing).as(Boolean.class).orElse(true)) {
            var timeout = runContext.render(this.waitTimeout).as(Duration.class).orElse(DEFAULT_WAIT_TIMEOUT);
            var task = this.waitForTask(searchIndex, taskUid, timeout);
            if (task.getDetails() != null) {
                deletedCount = task.getDetails().getDeletedDocuments();
                runContext.metric(Counter.of("documentDeleted", deletedCount));
                logger.info("Successfully deleted {} documents from index '{}'", deletedCount, rIndex);
            } else {
                logger.info("Successfully deleted documents from index '{}'", rIndex);
            }
        } else {
            logger.info("Successfully initiated document deletion on index '{}' with task UID {}", rIndex, taskUid);
        }

        return Output.builder()
            .taskUid(taskUid)
            .documentsDeleted(deletedCount)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Task UID", description = "UID of the Meilisearch deletion task enqueued.")
        private final Integer taskUid;

        @Schema(title = "Documents deleted", description = "Total number of documents deleted. Only available when `waitForIndexing` is true.")
        private final Integer documentsDeleted;
    }
}
