package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;

import java.util.List;
import java.util.Map;

public class TestUtils {


    public static final String URL = "http://localhost:7700";
    public static final String MASTER_KEY = "MASTER_KEY";

    public static DocumentAdd createDocumentAdd(Data<Map> data, String index) {
        return DocumentAdd.builder()
            .data(data)
            .index(Property.of(index))
            .url(URL)
            .key(MASTER_KEY)
            .build();
    }

    public static DocumentGet createDocumentGet(String id, String index) {
        return DocumentGet.builder()
            .id(id)
            .index(index)
            .url(URL)
            .key(MASTER_KEY)
            .build();
    }

    public static Search createSearch(String pattern, String index) {
        return Search.builder()
            .query(pattern)
            .index(index)
            .url(URL)
            .key(MASTER_KEY)
            .build();
    }

    public static FacetSearch createFacetSearch(String facetName, String facetQuery, List<String> filters, String index) {
        return FacetSearch.builder()
            .facetName(facetName)
            .facetQuery(facetQuery)
            .filters(filters)
            .index(index)
            .url(URL)
            .key(MASTER_KEY)
            .build();
    }
}
