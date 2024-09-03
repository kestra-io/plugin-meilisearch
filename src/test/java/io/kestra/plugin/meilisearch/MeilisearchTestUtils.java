package io.kestra.plugin.meilisearch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MeilisearchTestUtils {
    @SafeVarargs
    static String getJsonTestData(Map<String, Object>... data) {
        JSONArray array = new JSONArray();
        List<JSONObject> items = new ArrayList<>();
        for(Map<String, Object> item : data) {
            items.add(new JSONObject(item));
        }
        array.put(items);
        return array.getJSONArray(0).toString();
    }
}
