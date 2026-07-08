package io.kestra.plugin.meilisearch;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.tenant.TenantService;
import io.kestra.core.utils.IdUtils;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class DocumentAddTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void testDocumentAddBatchesDocuments() throws Exception {
        String index = "testBatch" + IdUtils.create();
        String documents = IntStream.rangeClosed(1, 10)
            .mapToObj(i -> "{\"id\": \"" + i + "\",\"name\": \"Person" + i + "\"}")
            .collect(Collectors.joining("\n"));
        URI uri = storageInterface.put(
            TenantService.MAIN_TENANT,
            null,
            URI.create("/" + IdUtils.create() + ".ion"),
            new ByteArrayInputStream(documents.getBytes(StandardCharsets.UTF_8))
        );

        DocumentAdd documentAdd = DocumentAdd.builder()
            .from(uri.toString())
            .index(Property.ofValue(index))
            .batchSize(Property.ofValue(4))
            .url(TestUtils.URL)
            .key(TestUtils.MASTER_KEY)
            .build();

        RunContext runContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd.Output output = documentAdd.run(runContext);

        assertThat(output.getTaskUids(), hasSize(3));
        assertThat(output.getDocumentsAdded(), is(10));

        DocumentGet documentGet = TestUtils.createDocumentGet("7", index);
        Map<String, Object> document = documentGet.run(runContextFactory.of(ImmutableMap.of())).getDocument();
        assertThat(document.get("name"), is("Person7"));
    }

    @Test
    void testDocumentAddFailsWhenIndexingTaskFails() throws Exception {
        String index = "testFailure" + IdUtils.create();
        String documents = "{\"name\": \"NoPrimaryKeyCandidate\"}";
        URI uri = storageInterface.put(
            TenantService.MAIN_TENANT,
            null,
            URI.create("/" + IdUtils.create() + ".ion"),
            new ByteArrayInputStream(documents.getBytes(StandardCharsets.UTF_8))
        );

        DocumentAdd documentAdd = TestUtils.createDocumentAdd(uri.toString(), index);

        RunContext runContext = runContextFactory.of(ImmutableMap.of());
        Exception exception = assertThrows(Exception.class, () -> documentAdd.run(runContext));

        assertThat(exception.getMessage(), containsString("primary key"));
    }
}
