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
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.kestra.core.repositories.ArrayListTotal;
import io.kestra.core.models.flows.Flow;

@Singleton
@MysqlRepositoryEnabled
public class MysqlFlowRepository extends AbstractJdbcFlowRepository {
    private final DSLContext dslContext;
    @Inject
    public MysqlFlowRepository(@Named("flows") MysqlRepository<FlowInterface> repository,
                               ApplicationContext applicationContext,
                               JdbcFilterService filterService) {
        super(repository, applicationContext, filterService);
        this.dslContext = applicationContext.getBean(DSLContext.class);
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
        return this.jdbcRepository
            .getDslContextWrapper()
            .transactionResult(configuration -> {
                DSLContext context = DSL.using(configuration);
                var latestExec = context.select(
                        DSL.field(DSL.quotedName("tenant_id")).as("tenant_id"),
                        DSL.field(DSL.quotedName("namespace")).as("namespace"),
                        DSL.field(DSL.quotedName("flow_id")).as("flow_id"),
                        DSL.field(DSL.quotedName("state_current")).as("state_current"),
                        DSL.rowNumber().over(
                            DSL.partitionBy(
                                DSL.field(DSL.quotedName("tenant_id")),
                                DSL.field(DSL.quotedName("namespace")),
                                DSL.field(DSL.quotedName("flow_id"))
                            ).orderBy(DSL.coalesce(
                                DSL.field(DSL.quotedName("end_date")),
                                DSL.field(DSL.quotedName("start_date"))
                            ).desc())
                        ).as("row_num")
                    )
                    .from(DSL.table("executions"))
                    .where(DSL.field(DSL.quotedName("tenant_id")).eq(tenantId))
                    .asTable("e_latest");

                Select<?> select = context
                    .select(DSL.field(DSL.quotedName("ft", "value")), DSL.field(DSL.quotedName("ft", "namespace")), DSL.field(DSL.quotedName("ft", "tenant_id")))
                    .from(fromLastRevision(false))
                    .join(jdbcRepository.getTable().as("ft"))
                        .on(
                            DSL.field(DSL.quotedName("ft", "key")).eq(DSL.field(DSL.quotedName("rev", "key")))
                            .and(DSL.field(DSL.quotedName("ft", "revision")).eq(DSL.field(DSL.quotedName("rev", "revision"))))
                        )
                    .leftJoin(latestExec)
                        .on(DSL.field(DSL.quotedName("ft", "id")).eq(DSL.field(DSL.quotedName("e_latest", "flow_id")))
                            .and(DSL.field(DSL.quotedName("ft", "namespace")).eq(DSL.field(DSL.quotedName("e_latest", "namespace"))))
                            .and(DSL.field(DSL.quotedName("ft", "tenant_id")).eq(DSL.field(DSL.quotedName("e_latest", "tenant_id"))))
                            .and(DSL.field(DSL.quotedName("e_latest", "row_num")).eq(1))
                        )
                    .where(
                        DSL.field(DSL.quotedName("ft", "tenant_id")).eq(tenantId)
                    )
                    .and(
                        DSL.field(DSL.quotedName("ft", "deleted")).eq(false)
                    )
                    // keep flows without executions (left join) and still order by state_current with nulls last
                    .orderBy(
                        DSL.case_().when(DSL.field(DSL.quotedName("e_latest", "state_current")).isNull(), 1).otherwise(0).asc(),
                        DSL.field(DSL.quotedName("e_latest", "state_current")).desc()
                    )
                    .limit(pageable.getSize())
                    .offset(pageable.getOffset());

                List<Flow> flows = jdbcRepository.fetch(select)
                    .stream()
                    .map(flow -> (Flow) flow)
                    .toList();

                return new ArrayListTotal<>(flows, flows.size());
            });
    }
}
