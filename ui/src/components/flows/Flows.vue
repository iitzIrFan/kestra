<template>
    <TopNavBar v-if="topbar" :title="routeInfo.title">
        <template #additional-right>
            <ul>
                <li>
                    <el-button :icon="Upload" @click="file?.click()">
                        {{ t("import") }}
                    </el-button>
                    <input ref="file" type="file" accept=".zip, .yml, .yaml" @change="importFlows()" class="d-none">
                </li>
                <li>
                    <router-link :to="{name: 'flows/search'}">
                        <el-button :icon="TextBoxSearch">
                            {{ t("source search") }}
                        </el-button>
                    </router-link>
                </li>
                <li>
                    <router-link
                        :to="{
                            name: 'flows/create',
                            query: {namespace: $route.query.namespace},
                        }"
                        v-if="canCreate"
                    >
                        <el-button :icon="Plus" type="primary">
                            {{ t("create") }}
                        </el-button>
                    </router-link>
                </li>
            </ul>
        </template>
    </TopNavBar>
    <section data-component="FILENAME_PLACEHOLDER" :class="{container: topbar}" v-if="ready">
        <div>
            <DataTable
                @page-changed="onPageChanged"
                ref="dataTable"
                :total="flowStore.total"
                :size="internalPageSize"
                :page="internalPageNumber"
                :hideTopPagination="!!namespace"
            >
                <template #navbar>
                    <KestraFilter
                        prefix="flows"
                        :language="FlowFilterLanguage"
                        :buttons="{
                            refresh: {
                                shown: true,
                                callback: refreshExecutionStatuses
                            },
                            settings: {shown: false}
                        }"
                        :properties="{
                            shown: true,
                            columns: optionalColumns,
                            displayColumns,
                            storageKey: 'flows',
                        }"
                        @update-properties="updateDisplayColumns"
                    />
                </template>

                <template #table>
                    <SelectTable
                        ref="selectTable"
                        :data="flowStore.flows"
                        :defaultSort="{prop: 'id', order: 'ascending'}"
                        tableLayout="auto"
                        fixed
                        @row-dblclick="onRowDoubleClick"
                        @sort-change="onSort"
                        :rowClassName="rowClasses"
                        @selection-change="handleSelectionChange"
                        :selectable="canCheck"
                        :no-data-text="t('no_results.flows')"
                        class="flows-table"
                    >
                        <template #select-actions>
                            <BulkSelect
                                :selectAll="queryBulkAction"
                                :selections="selection"
                                :total="flowStore.total"
                                @update:select-all="toggleAllSelection"
                                @unselect="toggleAllUnselected"
                            >
                                <el-button v-if="canRead" :icon="Download" @click="exportFlows()">
                                    {{ t("export") }}
                                </el-button>
                                <el-button v-if="canDelete" @click="deleteFlows" :icon="TrashCan">
                                    {{ t("delete") }}
                                </el-button>
                                <el-button
                                    v-if="canUpdate && anyFlowDisabled()"
                                    @click="enableFlows"
                                    :icon="FileDocumentCheckOutline"
                                >
                                    {{ t("enable") }}
                                </el-button>
                                <el-button
                                    v-if="canUpdate && anyFlowEnabled()"
                                    @click="disableFlows"
                                    :icon="FileDocumentRemoveOutline"
                                >
                                    {{ t("disable") }}
                                </el-button>
                            </BulkSelect>
                        </template>
                        <template #default>
                            <el-table-column
                                prop="id"
                                sortable="custom"
                                :sortOrders="['ascending', 'descending']"
                                :label="t('id')"
                            >
                                <template #default="scope">
                                    <div class="flow-id">
                                        <router-link
                                            :to="{
                                                name: 'flows/update',
                                                params: {
                                                    namespace:
                                                        scope.row.namespace,
                                                    id: scope.row.id,
                                                },
                                            }"
                                            class="me-1"
                                        >
                                            {{
                                                FILTERS.invisibleSpace(
                                                    scope.row.id,
                                                )
                                            }}
                                        </router-link>
                                        <MarkdownTooltip
                                            :id="scope.row.namespace +
                                                '-' +
                                                scope.row.id
                                            "
                                            :description="scope.row.description"
                                            :title="scope.row.namespace +
                                                '.' +
                                                scope.row.id
                                            "
                                        />
                                    </div>
                                </template>
                            </el-table-column>

                            <el-table-column v-if="displayColumn('labels')" :label="t('labels')">
                                <template #default="scope">
                                    <Labels :labels="scope.row.labels" />
                                </template>
                            </el-table-column>

                            <el-table-column
                                prop="namespace"
                                v-if="displayColumn('namespace')"
                                sortable="custom"
                                :sortOrders="['ascending', 'descending']"
                                :label="t('namespace')"
                                :formatter="(_: any, __: any, cellValue: string) =>
                                    FILTERS.invisibleSpace(cellValue)
                                "
                            />

                            <el-table-column
                                prop="state.startDate"
                                v-if="
                                    displayColumn('state.startDate') &&
                                        user.hasAny(permission.EXECUTION)
                                "
                                :label="t('last execution date')"
                            >
                                <template #default="scope">
                                    <DateAgo
                                        v-if="lastExecutionByFlowReady"
                                        :inverted="true"
                                        :date="getLastExecution(scope.row)
                                            ?.startDate
                                        "
                                    />
                                </template>
                            </el-table-column>

                            <el-table-column
                                prop="state.current"
                                v-if="
                                    displayColumn('state.current') &&
                                        user.hasAny(permission.EXECUTION)
                                "
                                :label="t('last execution status')"
                            >
                                <template #default="scope">
                                    <div
                                        v-if="lastExecutionByFlowReady && getLastExecution(scope.row)?.status"
                                        class="d-flex justify-content-between align-items-center"
                                    >
                                        <!-- Use a counter-based key for more efficient reactivity -->
                                        <Status 
                                            :key="`flow-status-${scope.row.id}-${scope.row.namespace}-${statusUpdateCounter.value}`"
                                            :status="getLastExecution(scope.row)?.status" 
                                            size="small" 
                                        />
                                    </div>
                                </template>
                            </el-table-column>

                            <el-table-column
                                prop="state"
                                v-if="displayColumn('state') &&
                                    user.hasAny(permission.EXECUTION)"
                                :label="t('execution statistics')"
                                className="row-graph"
                            >
                                <template #default="scope">
                                    <TimeSeries
                                        :chart="mappedChart(scope.row.id, scope.row.namespace)"
                                        showDefault
                                        short
                                    />
                                </template>
                            </el-table-column>

                            <el-table-column
                                v-if="displayColumn('triggers')"
                                :label="t('triggers')"
                                className="row-action"
                            >
                                <template #default="scope">
                                    <TriggerAvatar :flow="scope.row" />
                                </template>
                            </el-table-column>

                            <el-table-column columnKey="action" className="row-action" :label="t('actions')">
                                <template #default="scope">
                                    <router-link
                                        :to="{
                                            name: 'flows/update',
                                            params: {
                                                namespace: scope.row.namespace,
                                                id: scope.row.id,
                                            },
                                        }"
                                    >
                                        <Kicon :tooltip="t('details')" placement="left">
                                            <TextSearch />
                                        </Kicon>
                                    </router-link>
                                </template>
                            </el-table-column>
                        </template>
                    </SelectTable>
                </template>
            </DataTable>
        </div>
    </section>
</template>


<script setup lang="ts">
    import {ref, computed, onMounted, onUnmounted, watch, useTemplateRef} from "vue";
    import {useRoute, useRouter} from "vue-router";
    import {useI18n} from "vue-i18n";
    import {useExecutionsStore} from "../../stores/executions";
    import {useFlowStore} from "../../stores/flow";
    import {useAuthStore} from "override/stores/auth";
    import _merge from "lodash/merge";
    import permission from "../../models/permission";
    import action from "../../models/action";
    import BulkSelect from "../layout/BulkSelect.vue";
    // @ts-expect-error select-table is too big for ts conversion yet
    import SelectTable from "../layout/SelectTable.vue";
    import Plus from "vue-material-design-icons/Plus.vue";
    import TextBoxSearch from "vue-material-design-icons/TextBoxSearch.vue";
    import Download from "vue-material-design-icons/Download.vue";
    import TrashCan from "vue-material-design-icons/TrashCan.vue";
    import FileDocumentRemoveOutline from "vue-material-design-icons/FileDocumentRemoveOutline.vue";
    import FileDocumentCheckOutline from "vue-material-design-icons/FileDocumentCheckOutline.vue";
    import Upload from "vue-material-design-icons/Upload.vue";
    import KestraFilter from "../filter/KestraFilter.vue";
    import FlowFilterLanguage from "../../composables/monaco/languages/filters/impl/flowFilterLanguage";
    import TimeSeries from "../dashboard/sections/TimeSeries.vue";
    import TextSearch from "vue-material-design-icons/TextSearch.vue";
    import TopNavBar from "../../components/layout/TopNavBar.vue";
    // @ts-expect-error data-table is too big for ts conversion yet
    import DataTable from "../layout/DataTable.vue";
    import DateAgo from "../layout/DateAgo.vue";
    import Status from "../Status.vue";
    import TriggerAvatar from "./TriggerAvatar.vue";
    import MarkdownTooltip from "../layout/MarkdownTooltip.vue";
    import Kicon from "../Kicon.vue";
    import Labels from "../layout/Labels.vue";
    import {defaultNamespace} from "../../composables/useNamespaces";
    import * as YAML_UTILS from "@kestra-io/ui-libs/flow-yaml-utils";
    import {useToast} from "../../utils/toast";
    import {useDataTableActions} from "../../composables/useDataTableActions";
    import {useSelectTableActions} from "../../composables/useSelectTableActions";
    import * as FILTERS from "../../utils/filters";

    // Props
    const props = withDefaults(defineProps<{
        topbar?: boolean;
        namespace?: string;
        id?: string | null;
    }>(), {
        topbar: true,
        namespace: undefined,
        id: undefined,
    });

    // Stores
    const executionsStore = useExecutionsStore();
    const flowStore = useFlowStore();
    const authStore = useAuthStore();

    // Route
    const route = useRoute();
    const router = useRouter();

    const toast = useToast()
    const {t} = useI18n();

    // State
    const file = ref<HTMLInputElement | null>(null);
    const ready = ref(true);
    const internalPageSize = ref(25);
    const internalPageNumber = ref(1);
    const lastExecutionByFlowReady = ref(false);
    const latestExecutions = ref<any[]>([]);
    const statusUpdateCounter = ref(0); // Counter to force Status component update
    
    // Cache last executions to improve performance
    const flowExecutionsMap = computed(() => {
        if (!latestExecutions.value) return new Map();
        
        const map = new Map();
        latestExecutions.value.forEach((execution: any) => {
            if (execution && execution.flowId && execution.namespace) {
                const key = `${execution.namespace}/${execution.flowId}`;
                if (!map.has(key)) {
                    map.set(key, []);
                }
                map.get(key).push(execution);
            }
        });
        
        return map;
    });

    const optionalColumns: {
        label: string;
        prop: string;
        default: boolean;
        condition?: () => boolean;
    }[] = [
        {label: t("labels"), prop: "labels", default: true},
        {label: t("namespace"), prop: "namespace", default: true},
        {label: t("last execution date"), prop: "state.startDate", default: true},
        {label: t("last execution status"), prop: "state.current", default: true},
        {label: t("execution statistics"), prop: "state", default: true},
        {label: t("triggers"), prop: "triggers", default: true},
    ];

    const displayColumns = ref<string[]>([]);

    // Permission helpers
    const user = computed(() => authStore.user);
    const canRead = computed(() => user.value?.isAllowed(permission.FLOW, action.READ, route.query.namespace));
    const canDelete = computed(() => user.value?.isAllowed(permission.FLOW, action.DELETE, route.query.namespace));
    const canUpdate = computed(() => user.value?.isAllowed(permission.FLOW, action.UPDATE, route.query.namespace));
    const canCreate = computed(() => user.value?.hasAnyActionOnAnyNamespace(permission.FLOW, action.CREATE));
    const canCheck = computed(() => canRead.value || canDelete.value || canUpdate.value);

    const routeInfo = computed(() => ({title: t("flows")}));

    const selectTableRef = useTemplateRef<typeof SelectTable>("selectTable");
    const {queryWithFilter, onPageChanged, onRowDoubleClick, onSort} = useDataTableActions({dblClickRouteName: "flows/update"});

    function selectionMapper({id, namespace, disabled}: {id: string; namespace: string; disabled: boolean}) {
        return {
            id,
            namespace,
            enabled: !disabled,
        };
    }

    const {selection, queryBulkAction, handleSelectionChange, toggleAllUnselected, toggleAllSelection} = useSelectTableActions({
        dataTableRef: selectTableRef,
        selectionMapper
    });

    const selectionIds = computed(() => selection.value.map((flow) => ({id: flow.id, namespace: flow.namespace})));

    interface ChartDefinition {
        id: string;
        type: string;
        chartOptions: {
            displayName: string;
            description: string;
            legend: {enabled: boolean};
            column: string;
            colorByColumn: string;
            width: number;
        };
        data: {
            type: string;
            columns: {
                date: {field: string; displayName: string};
                state: {field: string};
                total: {displayName: string; agg: string};
                duration: {field: string; displayName: string; agg: string};
            };
            where: {field: string; type: string; value: string}[];
        };
        content?: string;
    }

    // Chart definition for mappedChart
    const CHART_DEFINITION: ChartDefinition = {
        id: "total_executions_timeseries",
        type: "io.kestra.plugin.core.dashboard.chart.TimeSeries",
        chartOptions: {
            displayName: "Total Executions",
            description: "Executions duration and count per date",
            legend: {enabled: false},
            column: "date",
            colorByColumn: "state",
            width: 12,
        },
        data: {
            type: "io.kestra.plugin.core.dashboard.data.Executions",
            columns: {
                date: {field: "START_DATE", displayName: "Date"},
                state: {field: "STATE"},
                total: {displayName: "Executions", agg: "COUNT"},
                duration: {field: "DURATION", displayName: "Duration", agg: "SUM"},
            },
            where: [
                {field: "NAMESPACE", type: "EQUAL_TO", value: "${namespace}"},
                {field: "FLOW_ID", type: "EQUAL_TO", value: "${flow_id}"},
            ],
        },
    };
    CHART_DEFINITION.content = YAML_UTILS.stringify(CHART_DEFINITION);



    function loadDisplayColumns(): string[] {
        const storedColumns = localStorage.getItem("columns_flows");
        if (storedColumns) {
            return storedColumns.split(",");
        }
        return optionalColumns.filter(col => col.default && (!col.condition || col.condition())).map(col => col.prop);
    }

    function displayColumn(column: string): boolean {
        return displayColumns.value.includes(column);
    }

    function updateDisplayColumns(newColumns: string[]) {
        displayColumns.value = newColumns;
    }

    function exportFlows() {
        toast.confirm(
            t("flow export", {flowCount: queryBulkAction.value ? flowStore.total : selection.value.length}),
            () => {
                const flowCount = queryBulkAction.value ? flowStore.total : selection.value.length;
                if (queryBulkAction.value) {
                    return flowStore.exportFlowByQuery(loadQuery()).then(() => {
                        toast.success(t("flows exported", {count: flowCount}));
                    });
                } else {
                    return flowStore.exportFlowByIds({ids: selection.value}).then(() => {
                        toast.success(t("flows exported", {count: flowCount}));
                    });
                }
            }
        );
    }

    function disableFlows() {
        toast.confirm(
            t("flow disable", {flowCount: queryBulkAction.value ? flowStore.total : selection.value.length}),
            () => {
                if (queryBulkAction.value) {
                    return flowStore.disableFlowByQuery(loadQuery()).then((r: any) => {
                        toast.success(t("flows disabled", {count: r.data.count}));
                        loadData(() => { });
                    });
                } else {
                    return flowStore.disableFlowByIds({ids: selectionIds.value}).then((r: any) => {
                        toast.success(t("flows disabled", {count: r.data.count}));
                        loadData(() => { });
                    });
                }
            }
        );
    }

    function anyFlowDisabled() {
        return selection.value.some((flow: any) => !flow.enabled);
    }
    function anyFlowEnabled() {
        return selection.value.some((flow: any) => flow.enabled);
    }

    function enableFlows() {

        toast.confirm(
            t("flow enable", {flowCount: queryBulkAction.value ? flowStore.total : selection.value.length}),
            () => {
                if (queryBulkAction.value) {
                    return flowStore.enableFlowByQuery(loadQuery()).then((r: any) => {
                        toast.success(t("flows enabled", {count: r.data.count}));
                        loadData(() => { });
                    });
                } else {
                    return flowStore.enableFlowByIds({ids: selectionIds.value}).then((r: any) => {
                        toast.success(t("flows enabled", {count: r.data.count}));
                        loadData(() => { });
                    });
                }
            }
        );
    }

    function deleteFlows() {
        toast.confirm(
            t("flow delete", {flowCount: queryBulkAction.value ? flowStore.total : selection.value.length}),
            () => {
                if (queryBulkAction.value) {
                    return flowStore.deleteFlowByQuery(loadQuery()).then((r: any) => {
                        toast.success(t("flows deleted", {count: r.data.count}));
                        loadData(() => { });
                    });
                } else {
                    return flowStore.deleteFlowByIds({ids: selectionIds.value}).then((r: any) => {
                        toast.success(t("flows deleted", {count: r.data.count}));
                        loadData(() => { });
                    });
                }
            }
        );
    }

    function importFlows() {
        const formData = new FormData();
        if (file.value && file.value.files && file.value.files[0]) {
            formData.append("fileUpload", file.value.files[0]);
            flowStore.importFlows(formData as any).then((res: any) => {
                if (res.data.length > 0) {
                    toast.warning(t("flows not imported") + ": " + res.data.join(", "));
                } else {
                    toast.success(t("flows imported"));
                }
                if (file.value) file.value.value = "";
                loadData(() => { });
            });
        }
    }

    // Enhanced cached function for getting the most relevant execution for a flow
    // with better error handling and memoization
    function getLastExecution(row: any) {
        // Check for valid inputs
        if (!row || !row.id || !row.namespace) return null;
        
        try {
            // Get executions from our cached map for better performance
            const key = `${row.namespace}/${row.id}`;
            const flowExecutions = flowExecutionsMap.value.get(key);
            
            if (!flowExecutions || flowExecutions.length === 0) return null;
            
            // Non-terminal states (in order of priority)
            const priorityStates = [
                "RUNNING",   // Active and running - highest priority
                "KILLING",   // In process of being killed
                "QUEUED",    // Waiting to run
                "RETRYING",  // In process of retry
                "PAUSED",    // Paused by user
                "BREAKPOINT",// Stopped at breakpoint
                "CREATED",   // Just created
                "RESTARTED"  // Being restarted
            ];
            
            // First check for any active executions in priority order
            for (const state of priorityStates) {
                const activeExecution = flowExecutions.find(e => e?.status === state);
                if (activeExecution) return activeExecution;
            }
            
            // Special case - if only one execution, return it directly
            if (flowExecutions.length === 1) {
                return flowExecutions[0];
            }
            
            // If multiple terminal state executions, sort by start date (newest first)
            // Create a defensive copy to avoid mutating original data
            return [...flowExecutions].sort((a, b) => {
                // Ensure we have valid dates
                if (!a?.startDate || !b?.startDate) return 0;
                
                try {
                    const dateA = new Date(a.startDate).getTime();
                    const dateB = new Date(b.startDate).getTime();
                    
                    // Check for invalid dates (NaN)
                    if (isNaN(dateA) || isNaN(dateB)) return 0;
                    
                    return dateB - dateA; // Most recent first
                } catch (error) {
                    console.warn("Date comparison error:", error);
                    return 0;
                }
            })[0];
        } catch (error) {
            console.error("Error getting last execution:", error);
            return null;
        }
    }

    function loadQuery(base?: any) {
        let queryFilter = queryWithFilter(undefined, []);
        if (props.namespace) {
            queryFilter["filters[namespace][PREFIX]"] = route.params.id || props.namespace;
        }
        return _merge(base, queryFilter);
    }

    function loadData(callback: () => void) {
        const q = route.query;
        flowStore
            .findFlows(
                loadQuery({
                    size: parseInt(props.namespace ? internalPageSize.value.toString() : (q.size as string) ?? "25"),
                    page: parseInt(props.namespace ? internalPageNumber.value.toString() : (q.page as string) ?? "1"),
                    sort: (q.sort as string) ?? "id:asc",
                })
            )
            .then((data: any) => {
                if (user.value?.hasAnyActionOnAnyNamespace(permission.EXECUTION, action.READ)) {
                    executionsStore.loadLatestExecutions({
                        flowFilters: data.results.map((flow: any) => ({id: flow.id, namespace: flow.namespace})),
                    }).then((latestExecs: any) => {
                        latestExecutions.value = latestExecs;
                        lastExecutionByFlowReady.value = true;
                    });
                }
            })
            .finally(callback);
    }

    function rowClasses(row: any) {
        return row && row.row && row.row.disabled ? "disabled" : "";
    }

    function mappedChart(id: string, namespace: string) {
        let MAPPED_CHARTS = JSON.parse(JSON.stringify(CHART_DEFINITION));
        MAPPED_CHARTS.content = MAPPED_CHARTS.content.replace("${namespace}", namespace).replace("${flow_id}", id);
        return MAPPED_CHARTS;
    }

    // Track active/pending requests to avoid race conditions
    const activeRefreshRequest = ref<boolean>(false);
    
    // Refresh timer for execution statuses
    let statusRefreshTimer: number | undefined = undefined;
    
    // Function to refresh execution statuses with improved reliability
    function refreshExecutionStatuses() {
        // If component is being unmounted or there's an active request, skip this refresh
        if (activeRefreshRequest.value || !ready.value) {
            // Skip this refresh cycle silently
            return;
        }
        
        // Check if we have the necessary permissions and data
        if (!user.value?.hasAnyActionOnAnyNamespace(permission.EXECUTION, action.READ) || 
            !flowStore.flows || 
            flowStore.flows.length === 0) {
            return;
        }
        
        // Set the flag to indicate we're making a request
        activeRefreshRequest.value = true;
        
        // Extract unique namespace-flow ID combinations to avoid duplicates
        const flowFilters = flowStore.flows.map((flow: any) => ({
            id: flow.id, 
            namespace: flow.namespace
        }));
        
        // Start execution status refresh
        
        // First, get the latest executions
        executionsStore.loadLatestExecutions({
            flowFilters: flowFilters
        }).then((latestExecs: any) => {
            // Store these executions
            if (latestExecs) {
                latestExecutions.value = latestExecs;
                lastExecutionByFlowReady.value = true;
            }
            
            // Then, query for active executions (non-terminal states)
            return executionsStore.findExecutions({
                commit: false,
                size: 100,
                filters: JSON.stringify([
                    {
                        key: "state.current", 
                        operator: "IN",
                        value: ["RUNNING", "QUEUED", "CREATED", "RESTARTED", "KILLING", "PAUSED", "RETRYING", "BREAKPOINT"]
                    }
                ])
            });
        }).then((activeExecs: any) => {
            // If we have active executions, prioritize them in our list
            if (activeExecs?.results?.length > 0 && latestExecutions.value) {
                // Build the combined execution list by merging latest and active executions
                const combinedExecutions: any[] = [];
                
                // Create an index for fast lookups
                const executionsByKey = new Map();
                
                // Process both latest and active executions
                if (latestExecutions.value?.length > 0) {
                    latestExecutions.value.forEach((exec: any) => {
                        if (exec && exec.flowId && exec.namespace) {
                            const key = `${exec.namespace}/${exec.flowId}`;
                            if (!executionsByKey.has(key)) {
                                executionsByKey.set(key, []);
                            }
                            executionsByKey.get(key).push(exec);
                            combinedExecutions.push(exec);
                        }
                    });
                }
                
                // Process active executions, potentially adding them or updating existing ones
                activeExecs.results.forEach((activeExec: any) => {
                    if (activeExec && activeExec.flowId && activeExec.namespace) {
                        const key = `${activeExec.namespace}/${activeExec.flowId}`;
                        
                        // Create a structured execution object
                        const executionObj = {
                            id: activeExec.id,
                            flowId: activeExec.flowId,
                            namespace: activeExec.namespace,
                            startDate: activeExec.state.startDate,
                            status: activeExec.state.current
                        };
                        
                        // Add to our lookup map
                        if (!executionsByKey.has(key)) {
                            executionsByKey.set(key, []);
                        }
                        executionsByKey.get(key).push(executionObj);
                        
                        // Also add to combined list
                        combinedExecutions.push(executionObj);
                    }
                });
                
                // Update our reference with the combined results
                latestExecutions.value = combinedExecutions;
            }
            
            // Increment the counter to force Status components to update
            // Use setTimeout to ensure Vue's reactivity has a chance to process the data changes first
            setTimeout(() => {
                statusUpdateCounter.value++;
            }, 0);
            
            return true;
        }).catch(error => {
            console.error("Error refreshing execution statuses:", error);
        }).finally(() => {
            // Always clear the flag when done
            activeRefreshRequest.value = false;
        });
    }
    
    // Start status refresh timer with a more reliable interval management
    function startStatusRefresh() {
        // Always ensure we stop any existing timers first for safety
        stopStatusRefresh();
        
        // Set ready state to true to allow refresh operations
        ready.value = true;
        
        // Load initial data right away
        refreshExecutionStatuses();
        
        // Set up the interval timer - using 5 seconds to be less resource-intensive
        statusRefreshTimer = window.setInterval(() => {
            if (document.visibilityState === "visible") {
                refreshExecutionStatuses();
            }
        }, 5000);
        
        // Add visibility change listener to pause/resume refreshes based on tab visibility
        document.addEventListener("visibilitychange", handleVisibilityChange);
    }
    
    // Handle visibility changes to pause refreshes when tab is not visible
    function handleVisibilityChange() {
        if (document.visibilityState === "visible" && ready.value) {
            // When tab becomes visible again, refresh immediately
            refreshExecutionStatuses();
        }
    }
    
    // Stop status refresh timer with improved cleanup
    function stopStatusRefresh() {
        // Clear the interval timer
        if (statusRefreshTimer !== undefined) {
            window.clearInterval(statusRefreshTimer);
            statusRefreshTimer = undefined;
        }
        
        // Remove the visibility change listener
        document.removeEventListener("visibilitychange", handleVisibilityChange);
        
        // Mark component as not ready for refresh
        ready.value = false;
    }
    
    // Lifecycle with improved component management
    onMounted(() => {
        
        // Reset state
        latestExecutions.value = [];
        lastExecutionByFlowReady.value = false;
        activeRefreshRequest.value = false;
        statusUpdateCounter.value = 0;
        
        // Load display columns
        displayColumns.value = loadDisplayColumns();
        
        // Load data and start refresh
        loadData(() => {
            // Start automatically refreshing execution statuses
            startStatusRefresh();
        });
    });
    
    // Clean up when component is unmounted
    onUnmounted(() => {
        
        // Always stop the refresh timer and clean up event listeners
        stopStatusRefresh();
        
        // Clear execution data to prevent stale data on remount
        latestExecutions.value = [];
        lastExecutionByFlowReady.value = false;
    });

    // Watch for route changes to handle navigation properly
    watch(() => route.fullPath, async (newPath, oldPath) => {
        // Handle navigation between views
        
        // If navigating away from Flows view, clean up
        if (!newPath.includes("/flows") && oldPath.includes("/flows")) {
            stopStatusRefresh();
        }
        
        // If navigating to Flows view, restart refresh
        if (newPath.includes("/flows") && !oldPath.includes("/flows")) {
            startStatusRefresh();
        }
    });
    
    // Watch for route.query changes to handle filtering and searching
    watch(() => route.query, async () => {
        // Route query changed, reload data
        // Stop the refresh timer while we reload data
        stopStatusRefresh();
        
        // Reset state
        latestExecutions.value = [];
        lastExecutionByFlowReady.value = false;
        
        // Reload data and restart refresh
        await loadData(() => {
            // Restart refresh after data is loaded
            startStatusRefresh();
        });
    }, {deep: true});

    watch(route, (newRoute) => {
        if (typeof window !== "undefined") {
            let queryHasChanged = false;
            const query = {...newRoute.query};
            const queryKeys = Object.keys(query);
            if (defaultNamespace() && !queryKeys.some(key => key.startsWith("filters[namespace]"))) {
                query["filters[namespace][PREFIX]"] = defaultNamespace();
                queryHasChanged = true;
            }
            if (!queryKeys.some(key => key.startsWith("filters[scope]"))) {
                query["filters[scope][EQUALS]"] = "USER";
                queryHasChanged = true;
            }
            if (queryHasChanged) {
                router.replace({...route, query});
            }
        }
    }, {immediate: true, deep: true});

</script>

<style lang="scss" scoped>
.shadow {
    box-shadow: 0px 2px 4px 0px var(--ks-card-shadow) !important;
}

:deep(nav .dropdown-menu) {
    display: flex;
    width: 20rem;
}

.flow-id {
    min-width: 200px;
}

.flows-table .el-table__cell {
    vertical-align: middle;
}

:deep(.flows-table) .el-scrollbar__thumb {
    background-color: var(--ks-border-active) !important;
}
</style>