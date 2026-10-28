package io.kestra.plugin.meilisearch;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.model.TaskInfo;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class DocumentDeleteTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void shouldDeleteSingleDocument() throws Exception {
        final String index = "testDeleteSingle%s".formatted(IdUtils.create());
        final String id = "101";

        Map<String, Object> document = Map.of(
            "id", id,
            "title", "The Matrix"
        );
        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd documentAdd = TestUtils.createDocumentAdd(document, index);
        documentAdd.run(addRunContext);

        RunContext searchRunContext = runContextFactory.of(ImmutableMap.of());
        Search search = TestUtils.createSearch("Matrix", index);
        Search.Output searchOutput = search.run(searchRunContext);
        assertThat(searchOutput.getTotalHits(), is(1L));

        RunContext deleteRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = TestUtils.createDocumentDelete(id, index);
        DocumentDelete.Output deleteOutput = documentDelete.run(deleteRunContext);

        assertThat(deleteOutput.getTaskUid(), notNullValue());
        assertThat(deleteOutput.getDocumentsDeleted(), is(1));

        Search.Output afterDeleteOutput = search.run(searchRunContext);
        assertThat(afterDeleteOutput.getTotalHits(), is(0L));
    }

    @Test
    void shouldDeleteMultipleDocuments() throws Exception {
        final String index = "testDeleteBatch%s".formatted(IdUtils.create());
        List<Map<String, Object>> documents = List.of(
            Map.of("id", "201", "title", "Inception"),
            Map.of("id", "202", "title", "Interstellar"),
            Map.of("id", "203", "title", "Memento")
        );
        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd documentAdd = TestUtils.createDocumentAdd(documents, index);
        documentAdd.run(addRunContext);

        RunContext searchRunContext = runContextFactory.of(ImmutableMap.of());
        Search search = TestUtils.createSearch("", index);
        Search.Output searchOutput = search.run(searchRunContext);
        assertThat(searchOutput.getTotalHits(), is(3L));

        RunContext deleteRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = TestUtils.createDocumentDeleteBatch(List.of("201", "202"), index);
        DocumentDelete.Output deleteOutput = documentDelete.run(deleteRunContext);

        assertThat(deleteOutput.getTaskUid(), notNullValue());
        assertThat(deleteOutput.getDocumentsDeleted(), is(2));

        Search.Output afterDeleteOutput = search.run(searchRunContext);
        assertThat(afterDeleteOutput.getTotalHits(), is(1L));
    }

    @Test
    void shouldDeleteAllDocuments() throws Exception {
        final String index = "testDeleteAll%s".formatted(IdUtils.create());
        List<Map<String, Object>> documents = List.of(
            Map.of("id", "301", "title", "Dune"),
            Map.of("id", "302", "title", "Dune 2")
        );
        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd documentAdd = TestUtils.createDocumentAdd(documents, index);
        documentAdd.run(addRunContext);

        RunContext searchRunContext = runContextFactory.of(ImmutableMap.of());
        Search search = TestUtils.createSearch("", index);
        Search.Output searchOutput = search.run(searchRunContext);
        assertThat(searchOutput.getTotalHits(), is(2L));

        RunContext deleteRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = TestUtils.createDocumentDeleteAll(index);
        DocumentDelete.Output deleteOutput = documentDelete.run(deleteRunContext);

        assertThat(deleteOutput.getTaskUid(), notNullValue());
        assertThat(deleteOutput.getDocumentsDeleted(), is(2));

        Search.Output afterDeleteOutput = search.run(searchRunContext);
        assertThat(afterDeleteOutput.getTotalHits(), is(0L));
    }

    @Test
    void shouldDeleteDocumentsByFilter() throws Exception {
        final String index = "testDeleteByFilter%s".formatted(IdUtils.create());
        Client client = new Client(new Config("http://localhost:7700", "MASTER_KEY"));
        TaskInfo settingsTask = client.index(index).updateFilterableAttributesSettings(new String[] { "genre" });
        client.index(index).waitForTask(settingsTask.getTaskUid(), 10000, 500);

        List<Map<String, Object>> documents = List.of(
            Map.of("id", "401", "title", "The Hangover", "genre", "comedy"),
            Map.of("id", "402", "title", "Superbad", "genre", "comedy"),
            Map.of("id", "403", "title", "The Shining", "genre", "horror")
        );
        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd documentAdd = TestUtils.createDocumentAdd(documents, index);
        documentAdd.run(addRunContext);

        RunContext searchRunContext = runContextFactory.of(ImmutableMap.of());
        Search search = TestUtils.createSearch("", index);
        Search.Output searchOutput = search.run(searchRunContext);
        assertThat(searchOutput.getTotalHits(), is(3L));

        RunContext deleteRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = TestUtils.createDocumentDeleteByFilter("genre = 'horror'", index);
        DocumentDelete.Output deleteOutput = documentDelete.run(deleteRunContext);

        assertThat(deleteOutput.getTaskUid(), notNullValue());
        assertThat(deleteOutput.getDocumentsDeleted(), is(1));

        Search.Output afterDeleteOutput = search.run(searchRunContext);
        assertThat(afterDeleteOutput.getTotalHits(), is(2L));
    }

    @Test
    void shouldDeleteWithoutWaitingForIndexing() throws Exception {
        final String index = "testDeleteNoWait%s".formatted(IdUtils.create());
        Map<String, Object> document = Map.of(
            "id", "501",
            "title", "Memento"
        );
        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd documentAdd = TestUtils.createDocumentAdd(document, index);
        documentAdd.run(addRunContext);

        RunContext deleteRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = DocumentDelete.builder()
            .documentId(Property.ofValue("501"))
            .index(Property.ofValue(index))
            .waitForIndexing(Property.ofValue(false))
            .url(TestUtils.URL)
            .key(TestUtils.MASTER_KEY)
            .build();
        DocumentDelete.Output deleteOutput = documentDelete.run(deleteRunContext);

        assertThat(deleteOutput.getTaskUid(), notNullValue());
        assertThat(deleteOutput.getDocumentsDeleted(), nullValue());
    }

    @Test
    void shouldThrowExceptionWhenFilterTaskFails() throws Exception {
        final String index = "testDeleteFilterFail%s".formatted(IdUtils.create());
        Map<String, Object> document = Map.of(
            "id", "601",
            "title", "The Matrix"
        );
        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd documentAdd = TestUtils.createDocumentAdd(document, index);
        documentAdd.run(addRunContext);

        RunContext deleteRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = DocumentDelete.builder()
            .filter(Property.ofValue("invalidAttribute = 'value'"))
            .index(Property.ofValue(index))
            .url(TestUtils.URL)
            .key(TestUtils.MASTER_KEY)
            .build();

        Exception exception = assertThrows(IllegalStateException.class, () -> documentDelete.run(deleteRunContext));
        assertThat(exception.getMessage(), containsString("Meilisearch task"));
    }

    @Test
    void shouldThrowExceptionWhenNoTargetSpecified() {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = DocumentDelete.builder()
            .index(Property.ofValue("emptyIndex%s".formatted(IdUtils.create())))
            .url(TestUtils.URL)
            .key(TestUtils.MASTER_KEY)
            .build();

        assertThrows(IllegalArgumentException.class, () -> documentDelete.run(runContext));
    }

    @Test
    void shouldThrowExceptionWhenMultipleTargetsSpecified() {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = DocumentDelete.builder()
            .index(Property.ofValue("multipleTargetsIndex%s".formatted(IdUtils.create())))
            .documentId(Property.ofValue("123"))
            .deleteAll(Property.ofValue(true))
            .url(TestUtils.URL)
            .key(TestUtils.MASTER_KEY)
            .build();

        assertThrows(IllegalArgumentException.class, () -> documentDelete.run(runContext));
    }
}
