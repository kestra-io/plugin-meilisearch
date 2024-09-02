package io.kestra.plugin.meilisearch;

import io.kestra.core.models.annotations.PluginProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public interface MeilisearchConnectionInterface {
    @NotNull
    @PluginProperty(dynamic = true)
    @Schema(
        title = "The connection string."
    )
    String getUrl();

    @NotNull
    @PluginProperty(dynamic = true)
    @Schema(
        title = "The connection key."
    )
    String getKey();
}
