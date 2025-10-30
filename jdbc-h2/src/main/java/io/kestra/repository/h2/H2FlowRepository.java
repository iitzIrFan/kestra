package io.kestra.repository.h2;

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
@H2RepositoryEnabled
public class H2FlowRepository extends AbstractJdbcFlowRepository {
    @Inject
    public H2FlowRepository(@Named("flows") H2Repository<FlowInterface> repository,
                            ApplicationContext applicationContext,
                            JdbcFilterService filterService) {
        super(repository, applicationContext, filterService);
    }

    @Override
    protected Condition findCondition(String query, Map<String, String> labels) {
        return H2FlowRepositoryService.findCondition(this.jdbcRepository, query, labels);
    }

    @Override
    protected Condition findCondition(Object value, QueryFilter.Op operation) {
        return H2FlowRepositoryService.findCondition(value, operation);
    }

    @Override
    protected Condition findSourceCodeCondition(String query) {
        return H2FlowRepositoryService.findSourceCodeCondition(this.jdbcRepository, query);
    }

    @Override
    public ArrayListTotal<Flow> findWithLastExecutionStatus(Pageable pageable, String tenantId, List<QueryFilter> filters) {
        // Implement the logic to fetch flows with last execution status
        throw new UnsupportedOperationException("Method not implemented yet");
    }
}
