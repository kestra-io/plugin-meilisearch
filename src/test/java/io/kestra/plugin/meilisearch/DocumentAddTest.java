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

import static org.junit.jupiter.api.Assertions.*;

@KestraTest
class DocumentAddTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {

        JSONArray array = new JSONArray();
        ArrayList items = new ArrayList() {{
            add(new JSONObject().put("id", "1").put("title", "Carol").put("genres",new JSONArray("[\"Romance\",\"Drama\"]")));
            add(new JSONObject().put("id", "2").put("title", "Wonder Woman").put("genres",new JSONArray("[\"Action\",\"Adventure\"]")));
            add(new JSONObject().put("id", "3").put("title", "Life of Pi").put("genres",new JSONArray("[\"Adventure\",\"Drama\"]")));
            add(new JSONObject().put("id", "4").put("title", "Mad Max: Fury Road").put("genres",new JSONArray("[\"Adventure\",\"Science Fiction\"]")));
            add(new JSONObject().put("id", "5").put("title", "Moana").put("genres",new JSONArray("[\"Fantasy\",\"Action\"]")));
            add(new JSONObject().put("id", "6").put("title", "Philadelphia").put("genres",new JSONArray("[\"Drama\"]")));
        }};

        array.put(items);
        String documents = array.getJSONArray(0).toString();

        RunContext runContext = runContextFactory.of(ImmutableMap.of(
            "document", documents,
            "index", "test",
            "url", "http://localhost:7700",
            "key", "MASTER_KEY"
        ));

        DocumentAdd documentAdd = DocumentAdd.builder()
            .document("{{document}}")
            .index("{{index}}")
            .url("{{url}}")
            .key("{{key}}")
            .build();

        DocumentAdd.Output runOutput = documentAdd.run(runContext);

        assertFalse(runOutput.isSuccess());
    }
}