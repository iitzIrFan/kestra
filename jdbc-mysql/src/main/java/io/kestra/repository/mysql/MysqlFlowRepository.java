package io.kestra.repository.mysql;

import io.kestra.core.models.QueryFilter;
import io.kestra.core.models.flows.FlowInterface;
import io.kestra.jdbc.repository.AbstractJdbcFlowRepository;
import io.kestra.jdbc.services.JdbcFilterService;
import io.micronaut.context.ApplicationContext;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.kestra.core.repositories.ArrayListTotal;
import io.kestra.core.models.flows.Flow;

@Singleton
@MysqlRepositoryEnabled
public class MysqlFlowRepository extends AbstractJdbcFlowRepository {
    @Inject
    public MysqlFlowRepository(@Named("flows") MysqlRepository<FlowInterface> repository,
                               ApplicationContext applicationContext,
                               JdbcFilterService filterService) {
        super(repository, applicationContext, filterService);
    }

    @Override
    protected Condition findCondition(String query, Map<String, String> labels) {
        return MysqlFlowRepositoryService.findCondition(this.jdbcRepository, query, labels);
    }

    @Override
    protected Condition findCondition(Object value, QueryFilter.Op operation) {
        return MysqlFlowRepositoryService.findCondition(value, operation);
    }

    @Override
    protected Condition findSourceCodeCondition(String query) {
        return MysqlFlowRepositoryService.findSourceCodeCondition(this.jdbcRepository, query);
    }

    @Override
    public ArrayListTotal<Flow> findWithLastExecutionStatus(Pageable pageable, String tenantId, List<QueryFilter> filters) {
        // Implement the logic to fetch flows with last execution status
        throw new UnsupportedOperationException("Method not implemented yet");
    }
}
