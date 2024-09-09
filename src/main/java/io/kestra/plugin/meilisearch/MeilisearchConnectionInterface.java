package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public interface MeilisearchConnectionInterface {
    @NotNull
    @Schema(
        title = "The connection url."
    )
    Property<String> getUrl();

    @NotNull
    @Schema(
        title = "The connection key."
    )
    Property<String> getKey();
}
