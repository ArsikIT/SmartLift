package com.smartlift.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@TestConfiguration
public class AsyncTestConfig {

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
