package org.kestra.task.templates;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.kestra.core.models.annotations.PluginProperty;
import org.kestra.core.models.tasks.RunnableTask;
import org.kestra.core.models.tasks.Task;
import org.kestra.core.runners.RunContext;
import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Short description for this task",
    description = "Full description of this task"
)
public class Example extends Task implements RunnableTask<Example.Output> {
    @Schema(
        title = "Short description for this input",
        description = "Full description of this input"
    )
    @PluginProperty(dynamic = true) // If the variables will be rendered with template {{ }}
    private String format;

    @Override
    public Example.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String render = runContext.render(format);
        logger.debug(render);

        return Output.builder()
            .child(new OutputChild(StringUtils.reverse(render)))
            .build();
    }

    /**
     * Input or Output can nested as you need
     */
    @Builder
    @Getter
    public static class Output implements org.kestra.core.models.tasks.Output {
        @Schema(
            title = "Short description for this output",
            description = "Full description of this output"
        )
        private final OutputChild child;
    }

    @Builder
    @Getter
    public static class OutputChild implements org.kestra.core.models.tasks.Output {
        @Schema(
            title = "Short description for this output",
            description = "Full description of this output"
        )
        private final String value;
    }
}