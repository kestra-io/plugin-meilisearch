package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractMeilisearchConnection extends Task implements MeilisearchConnectionInterface {
    private String url;
    private String key;

    public Client createClient(RunContext runContext) throws IllegalVariableEvaluationException {
        Config config = new Config(runContext.render(url), runContext.render(key));
        return new Client(config);
    }
}
