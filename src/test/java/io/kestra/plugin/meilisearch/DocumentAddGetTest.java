package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.utils.IdUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.json.JSONArray;

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
        final String id = "12";
        final String index = "testAddJson";

        Map<String, Object> documents = Map.of(
            "id", id,
            "title", "Notebook",
            "genres", new JSONArray("[\"Romance\",\"Drama\"]")
        );

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentAdd documentAdd = DocumentAdd.builder()
            .from(documents)
            .index(index)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext getRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentGet documentGet = DocumentGet.builder()
            .id(id)
            .index(index)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        DocumentGet.Output getOutput = documentGet.run(getRunContext);

        Map<String, Object> doc = (Map<String, Object>) getOutput.getDocument();
        assertThat(doc.get("id"), is(id));
    }

    @Test
    void testAddGetFileDocument() throws Exception {
        final String index = "testAddFile";

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("examples/input.json");

        URI uri = storageInterface.put(null, URI.create("/" + IdUtils.create() + ".ion"), inputStream);

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentAdd documentAdd = DocumentAdd.builder()
            .from(uri.toString())
            .index(index)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        Thread.sleep(500);

        RunContext getRunContext = runContextFactory.of(ImmutableMap.of());

        DocumentGet documentGet = DocumentGet.builder()
            .id("3")
            .index(index)
            .url("http://localhost:7700")
            .key("MASTER_KEY")
            .build();

        DocumentGet.Output getOutput = documentGet.run(getRunContext);

        Map<String, Object> doc = (Map<String, Object>) getOutput.getDocument();
        assertThat(doc.get("id"), is("3"));
        assertThat(doc.get("name"), is("Bryan"));
    }
}