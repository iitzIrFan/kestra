package io.kestra.repository.postgres;

import io.kestra.core.models.QueryFilter;
import io.kestra.core.models.flows.FlowInterface;
import io.kestra.jdbc.AbstractJdbcRepository;
import io.kestra.jdbc.services.JdbcFilterService;
import io.micronaut.context.ApplicationContext;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.kestra.core.repositories.ArrayListTotal;
import io.kestra.core.models.flows.Flow;
import io.kestra.jdbc.repository.AbstractJdbcFlowRepository;

@Singleton
@PostgresRepositoryEnabled
public class PostgresFlowRepository extends AbstractJdbcFlowRepository {
    private final DSLContext dslContext;

    public PostgresFlowRepository(
        AbstractJdbcRepository<FlowInterface> jdbcRepository,
        ApplicationContext applicationContext,
        JdbcFilterService jdbcFilterService
    ) {
        super(jdbcRepository, applicationContext, jdbcFilterService);
        this.dslContext = applicationContext.getBean(DSLContext.class);
    }

    @Override
    protected Condition findCondition(String query, Map<String, String> labels) {
        return PostgresFlowRepositoryService.findCondition(this.jdbcRepository, query, labels);
    }

    @Override
    protected Condition findCondition(Object value, QueryFilter.Op operation) {
        return PostgresFlowRepositoryService.findCondition( value, operation);
    }

    @Override
    protected Condition findSourceCodeCondition(String query) {
        return PostgresFlowRepositoryService.findSourceCodeCondition(this.jdbcRepository, query);
    }

    @Override
    public ArrayListTotal<Flow> findWithLastExecutionStatus(Pageable pageable, String tenantId, List<QueryFilter> filters) {
        Select<?> select = dslContext.select(DSL.field("f.*"), DSL.field("e.state_current", String.class).as("lastExecutionStatus"))
            .from(DSL.table("flows").as("f"))
            .leftJoin(DSL.table("executions").as("e"))
                .on(DSL.field("f.id").eq(DSL.field("e.flow_id")))
            .where(DSL.field("f.tenant_id").eq(tenantId))
            .and(DSL.field("e.state_current").isNotNull()) // Ensure state_current is not null
            .orderBy(DSL.field("e.state_current").desc())
            .limit(pageable.getSize())
            .offset(pageable.getOffset());

        List<Flow> flows = jdbcRepository.fetch(select)
            .stream()
            .map(flow -> (Flow) flow)
            .collect(Collectors.toList());

        return new ArrayListTotal<>(flows, flows.size());
    }
}
