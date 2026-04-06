package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Property;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import io.kestra.core.models.annotations.PluginProperty;

public interface MeilisearchConnectionInterface {
    @NotNull
    @Schema(
        title = "Meilisearch connection URL"
    )
    @PluginProperty(group = "main")
    Property<String> getUrl();

    @NotNull
    @Schema(
        title = "Meilisearch connection key"
    )
    @PluginProperty(group = "main")
    Property<String> getKey();
}
