package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.utils.IdUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URI;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@KestraTest
class DocumentAddGetTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void testAddGetJsonDocument() throws Exception {
        final String id = "13";
        final String index = "testAddJson";

        Map<String, Object> document = Map.of(
            "id", id,
            "title", "Notebook",
            "genres", new String[]{"Romance","Drama"}
        );
        Data<Map> data = Data.<Map>builder().fromMap(Property.of(document)).build();

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd documentAdd = TestUtils.createDocumentAdd(data, index);

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext getRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentGet documentGet = TestUtils.createDocumentGet(id, index);

        DocumentGet.Output getOutput = documentGet.run(getRunContext);

        Map<String, Object> doc = (Map<String, Object>) getOutput.getDocument();
        assertThat(doc.get("id"), is(id));
    }

    @Test
    void testAddGetFileDocument() throws Exception {
        final String index = "testAddFile";
        final String id = "3";

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("examples/documentAdd");

        URI uri = storageInterface.put(null, URI.create("/" + IdUtils.create() + ".ion"), inputStream);
        Data<Map> data = Data.<Map>builder().fromURI(Property.of(uri)).build();

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());
        DocumentAdd documentAdd = TestUtils.createDocumentAdd(data, index);

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext getRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentGet documentGet = TestUtils.createDocumentGet(id, index);

        DocumentGet.Output getOutput = documentGet.run(getRunContext);

        Map<String, Object> doc = (Map<String, Object>) getOutput.getDocument();
        assertThat(doc.get("id"), is(id));
        assertThat(doc.get("name"), is("Bryan"));
    }
}