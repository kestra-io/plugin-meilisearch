package io.kestra.plugin.meilisearch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@KestraTest
class DocumentAddGetTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testAddGetJsonDocument() throws Exception {

        JSONArray array = new JSONArray();
        ArrayList items = new ArrayList() {{
            add(new JSONObject().put("id", "11").put("title", "Carol").put("genres",new JSONArray("[\"Romance\",\"Drama\"]")));
        }};

        array.put(items);
        String documents = array.getJSONArray(0).toString();

        RunContext addRunContext = runContextFactory.of(ImmutableMap.of(
            "document", documents,
            "index", "test",
            "url", "http://localhost:7700",
            "key", "MASTER_KEY"
        ));

        DocumentAdd documentAdd = DocumentAdd.builder()
            .from("{{document}}")
            .index("{{index}}")
            .url("{{url}}")
            .key("{{key}}")
            .build();

        DocumentAdd.Output runOutput = documentAdd.run(addRunContext);

        RunContext getRunContext = runContextFactory.of(ImmutableMap.of(
            "id", "11",
            "index", "test",
            "url", "http://localhost:7700",
            "key", "MASTER_KEY"
        ));

        DocumentGet documentGet = DocumentGet.builder()
            .id("{{id}}")
            .index("{{index}}")
            .url("{{url}}")
            .key("{{key}}")
            .build();

        DocumentGet.Output getOutput = documentGet.run(getRunContext);

        Map<String, Object> doc = (Map<String, Object>) getOutput.getDocument();

        assertThat(doc.get("id"), is("11"));
    }
}