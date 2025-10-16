<template>
    <div v-if="isLoading" class="d-flex justify-content-center py-4">
        <el-progress type="circle" indeterminate />
    </div>
    <template v-else-if="hasConcurrency">
        <div v-if="shouldShowContent" :class="{'d-none': !runningCountSet}">
            <el-card class="mb-3">
                <div class="row mb-3">
                    <span class="col d-flex align-items-center">
                        <h5 class="m-3">RUNNING</h5> {{ runningCount }}/{{ concurrencyLimit }} {{ $t('active-slots') }}
                    </span>
                    <span class="col d-flex justify-content-end align-items-center">
                        {{ $t('behavior') }}: <Status class="mx-2" :status="concurrencyBehavior" size="small" />
                    </span>
                </div>
                <div class="progressbar mb-3">
                    <el-progress :stroke-width="16" color="#5BB8FF" :percentage="progress" :showText="false" />
                </div>
            </el-card>
            <el-card>
                <Executions
                    :restoreUrl="false"
                    :topbar="false"
                    :namespace="flowNamespace"
                    :flowId="flowId"
                    isConcurrency
                    :statuses="executionStatuses"
                    @state-count="setRunningCount"
                    filter
                />
            </el-card>
        </div>
        <Empty v-else type="concurrency_executions" />
    </template>
    <Empty v-else type="concurrency_limit" />
</template>

<script setup lang="ts">
    import {ref, computed, onMounted} from "vue";
    import {storeToRefs} from "pinia";
    import type {Ref} from "vue";
    import Executions from "../executions/Executions.vue";
    import Empty from "../layout/empty/Empty.vue";
    import {State} from "@kestra-io/ui-libs";
    import Status from "../Status.vue";
    import {useFlowStore} from "../../stores/flow";

    // Types
    interface ConcurrencyConfig {
        limit: number;
        behavior: string;
    }

    interface Flow {
        namespace: string;
        id: string;
        concurrency?: ConcurrencyConfig;
    }

    interface CountObject {
        runningCount: number;
        totalCount: number;
    }

    // Props and Emits
    defineEmits<{
        (e: "expand-subflow"): void;
    }>();

    // State
    const isLoading = ref<boolean>(true);
    const runningCount = ref<number>(0);
    const totalCount = ref<number>(0);
    const runningCountSet = ref<boolean>(false);

    // Store
    const flowStore = useFlowStore();
    const {flow} = storeToRefs(flowStore) as {
        flow: Ref<Flow>;
    };

    // Computed
    const hasConcurrency = computed(() => {
        return Boolean(flow.value?.concurrency);
    });

    const concurrencyLimit = computed(() => {
        return flow.value?.concurrency?.limit ?? 0;
    });

    const concurrencyBehavior = computed(() => {
        return flow.value?.concurrency?.behavior ?? "";
    });

    const flowNamespace = computed(() => {
        return flow.value?.namespace ?? "";
    });

    const flowId = computed(() => {
        return flow.value?.id ?? "";
    });

    const progress = computed(() => {
        if (!concurrencyLimit.value) return 0;
        return Math.min(100, (runningCount.value / concurrencyLimit.value) * 100);
    });

    const shouldShowContent = computed(() => {
        return totalCount.value > 0 || !runningCountSet.value;
    });

    const executionStatuses = computed(() => {
        return [State.QUEUED, State.RUNNING, State.PAUSED];
    });

    // Methods
    const setRunningCount = (count: number | CountObject): void => {
        if (typeof count === "object") {
            runningCount.value = count.runningCount;
            totalCount.value = count.totalCount;
        } else {
            runningCount.value = count;
            totalCount.value = count;
        }
        runningCountSet.value = true;
        isLoading.value = false;
    };

    // Lifecycle
    onMounted(() => {
        if (!flow.value?.concurrency) {
            isLoading.value = false;
        }
    });
</script>

<style scoped lang="scss">
    .img-size {
        max-width: 200px;
    }
    .bg-purple {
        height: 100%;
        width: 100%;
    }
    h5 {
        font-weight: bold;
        margin-left: 0 !important;
    }

    :deep(.el-progress) {
        .el-progress-bar, .el-progress-bar__outer, .el-progress-bar__inner {
            border-radius: var(--bs-border-radius);
        }
    }

    :deep(.el-card) {
        background-color: var(--ks-background-panel);
    }
</style>