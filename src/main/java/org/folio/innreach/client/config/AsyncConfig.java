package org.folio.innreach.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final int TASK_EXECUTOR_CORE_POOL_SIZE = 10;
    private static final int TASK_EXECUTOR_MAX_POOL_SIZE = 10;

    @Bean(name = "asyncTaskExecutor")
    public TaskExecutor getAsyncTaskExecutor() {
        var threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(TASK_EXECUTOR_CORE_POOL_SIZE);
        threadPoolTaskExecutor.setMaxPoolSize(TASK_EXECUTOR_MAX_POOL_SIZE);
        return threadPoolTaskExecutor;
    }

}