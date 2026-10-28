package io.kestra.plugin.meilisearch;

import java.time.Duration;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.TaskStatus;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
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
    protected static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofMinutes(5);
    protected static final int WAIT_INTERVAL_MS = 500;

    protected Property<String> url;
    protected Property<String> key;

    public Client createClient(RunContext runContext) throws IllegalVariableEvaluationException {
        Config config = new Config(
            runContext.render(this.url).as(String.class).orElseThrow(),
            runContext.render(this.key).as(String.class).orElseThrow()
        );
        return new Client(config);
    }

    // The SDK's waitForTask returns normally on FAILED or CANCELED, so the status is checked here.
    protected com.meilisearch.sdk.model.Task waitForTask(Index index, Integer taskUid, Duration waitTimeout) {
        index.waitForTask(taskUid, (int) waitTimeout.toMillis(), WAIT_INTERVAL_MS);
        var task = index.getTask(taskUid);
        if (TaskStatus.FAILED.equals(task.getStatus()) || TaskStatus.CANCELED.equals(task.getStatus())) {
            var error = task.getError();
            var errorMessage = error != null ? ": %s (%s)".formatted(error.getMessage(), error.getCode()) : "";
            throw new IllegalStateException(
                "Meilisearch task %d on index '%s' ended with status %s%s".formatted(
                    taskUid,
                    index.getUid(),
                    task.getStatus(),
                    errorMessage
                )
            );
        }
        return task;
    }
}
