package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Property;

import java.util.List;

public class TestUtils {


    public static final Property<String> URL = Property.of("http://localhost:7700");
    public static final Property<String> MASTER_KEY = Property.of("MASTER_KEY");

    public static DocumentAdd createDocumentAdd(Object from, String index) {
        return DocumentAdd.builder()
            .from(from)
            .index(Property.ofValue(index))
            .url(URL)
            .key(MASTER_KEY)
            .build();
    }

    public static DocumentGet createDocumentGet(String id, String index) {
        return DocumentGet.builder()
            .documentId(Property.ofValue(id))
            .index(Property.ofValue(index))
            .url(URL)
            .key(MASTER_KEY)
            .build();
    }

    public static Search createSearch(String pattern, String index) {
        return Search.builder()
            .query(Property.ofValue(pattern))
            .index(Property.ofValue(index))
            .url(URL)
            .key(MASTER_KEY)
            .build();
    }

    public static FacetSearch createFacetSearch(String facetName, String facetQuery, List<String> filters, String index) {
        return FacetSearch.builder()
            .facetName(Property.ofValue(facetName))
            .facetQuery(Property.ofValue(facetQuery))
            .filters(Property.ofValue(filters))
            .index(Property.ofValue(index))
            .url(URL)
            .key(MASTER_KEY)
            .build();
    }
}
