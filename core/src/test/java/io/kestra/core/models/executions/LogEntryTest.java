package io.kestra.core.models.executions;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import static org.assertj.core.api.Assertions.assertThat;

public class LogEntryTest {
    @Test
    void shouldFindLevelsAtOrBelow() {
        assertThat(LogEntry.findLevelsAtOrBelow(Level.TRACE)).containsExactly(
            Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR
        );
        assertThat(LogEntry.findLevelsAtOrBelow(Level.INFO)).containsExactly(
            Level.INFO, Level.WARN, Level.ERROR
        );
        assertThat(LogEntry.findLevelsAtOrBelow(Level.ERROR)).containsExactly(Level.ERROR);
        assertThat(LogEntry.findLevelsAtOrBelow(null)).containsExactly(
            Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR
        );
    }

    @Test
    public void shouldFormatToLogMap() {
        LogEntry logEntry = LogEntry.builder()
            .tenantId("tenantId")
            .namespace("namespace")
            .flowId("flowId")
            .taskId("taskId")
            .executionId("executionId")
            .taskRunId("taskRunId")
            .attemptNumber(1)
            .triggerId("triggerId")
            .thread("thread")
            .message("message")
            .build();
        Map<String, Object> logMap = logEntry.toLogMap();
        assertThat(logMap.get("tenantId")).isEqualTo("tenantId");
        assertThat(logMap.get("namespace")).isEqualTo("namespace");
        assertThat(logMap.get("flowId")).isEqualTo("flowId");
        assertThat(logMap.get("taskId")).isEqualTo("taskId");
        assertThat(logMap.get("executionId")).isEqualTo("executionId");
        assertThat(logMap.get("taskRunId")).isEqualTo("taskRunId");
        assertThat(logMap.get("attemptNumber")).isEqualTo(1);
        assertThat(logMap.get("triggerId")).isEqualTo("triggerId");
        assertThat(logMap.get("thread")).isEqualTo("thread");
        assertThat(logMap.get("message")).isEqualTo("message");
    }

}
