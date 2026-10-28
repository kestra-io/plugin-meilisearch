package io.kestra.plugin.meilisearch;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class DocumentDeleteTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void shouldDeleteSingleDocument() throws Exception {
        final String index = "testDeleteSingle";
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

        Search.Output afterDeleteOutput = search.run(searchRunContext);
        assertThat(afterDeleteOutput.getTotalHits(), is(0L));
    }

    @Test
    void shouldDeleteMultipleDocuments() throws Exception {
        final String index = "testDeleteBatch";
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

        Search.Output afterDeleteOutput = search.run(searchRunContext);
        assertThat(afterDeleteOutput.getTotalHits(), is(1L));
    }

    @Test
    void shouldDeleteAllDocuments() throws Exception {
        final String index = "testDeleteAll";
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

        Search.Output afterDeleteOutput = search.run(searchRunContext);
        assertThat(afterDeleteOutput.getTotalHits(), is(0L));
    }

    @Test
    void shouldThrowExceptionWhenNoTargetSpecified() {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = DocumentDelete.builder()
            .index(Property.ofValue("emptyIndex"))
            .url(TestUtils.URL)
            .key(TestUtils.MASTER_KEY)
            .build();

        assertThrows(IllegalArgumentException.class, () -> documentDelete.run(runContext));
    }

    @Test
    void shouldThrowExceptionWhenMultipleTargetsSpecified() {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());
        DocumentDelete documentDelete = DocumentDelete.builder()
            .index(Property.ofValue("multipleTargetsIndex"))
            .documentId(Property.ofValue("123"))
            .deleteAll(Property.ofValue(true))
            .url(TestUtils.URL)
            .key(TestUtils.MASTER_KEY)
            .build();

        assertThrows(IllegalArgumentException.class, () -> documentDelete.run(runContext));
    }
}
