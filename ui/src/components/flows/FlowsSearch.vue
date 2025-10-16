<template>
    <TopNavBar :title="routeInfo.title" :breadcrumb="routeInfo.breadcrumb" />
    <section class="container" v-if="ready">
        <div>
            <DataTable
                @page-changed="onPageChanged"
                striped
                hover
                ref="dataTable"
                :total="flowStore.total"
            >
                <template #navbar>
                    <el-form-item>
                        <SearchField />
                    </el-form-item>
                    <el-form-item>
                        <NamespaceSelect
                            data-type="flow"
                            v-if="$route.name !== 'flows/update'"
                            :value="$route.query.namespace"
                            @update:model-value="onDataTableValue('namespace', $event)"
                        />
                    </el-form-item>
                </template>

                <template #table>
                    <template v-for="(item, i) in flowStore.search" :key="`card-${i}`">
                        <el-card class="mb-2" shadow="never">
                            <template #header>
                                <router-link :to="{path: `/flows/edit/${item.model.namespace}/${item.model.id}/source`}">
                                    {{ item.model.namespace }}.{{ item.model.id }}
                                </router-link>
                            </template>
                            <template v-for="(fragment, j) in item.fragments" :key="`pre-${i}-${j}`">
                                <small>
                                    <pre class="mb-1 text-sm-left" v-html="sanitize(fragment)" />
                                </small>
                            </template>
                        </el-card>
                    </template>

                    <NoData v-if="flowStore.search === undefined || flowStore.search.length === 0" />
                </template>
            </DataTable>
        </div>
    </section>
</template>

<script setup lang="ts">
    import {ref, computed} from "vue";
    import {useRoute, useRouter, LocationQueryRaw} from "vue-router";
    import {useI18n} from "vue-i18n";
    import {useFlowStore} from "../../stores/flow";
    import NamespaceSelect from "../namespaces/components/NamespaceSelect.vue";
    import {useDataTableActions} from "../../composables/useDataTableActions";
    import useRestoreUrl from "../../composables/useRestoreUrl";
    import DataTable from "../layout/DataTable.vue";
    import SearchField from "../layout/SearchField.vue";
    import NoData from "../layout/NoData.vue";
    import _escape from "lodash/escape";
    import _merge from "lodash/merge";
    import TopNavBar from "../layout/TopNavBar.vue";

    // Types
    interface QueryBase {
        size?: number;
        page?: number;
        sort?: string;
    }

    // Initialize composables
    const route = useRoute();
    const router = useRouter();
    const {t} = useI18n();
    const flowStore = useFlowStore();
    const {queryWithFilter} = useDataTableActions();
    const {saveRestoreUrl} = useRestoreUrl();

    // Data
    const ready = ref(true);
    const dataTable = ref<InstanceType<typeof DataTable> | null>(null);

    // Computed
    const routeInfo = computed(() => ({
        title: t("source search"),
        breadcrumb: [
            {
                label: t("flows"),
                link: {
                    name: "flows/list",
                }
            },
        ]
    }));

    // Methods
    const sanitize = (content: string): string => {
        return _escape(content)
            .replaceAll("[mark]", "<mark>")
            .replaceAll("[/mark]", "</mark>");
    };

    const loadQuery = (base: QueryBase) => {
        const queryFilter = queryWithFilter();
        return _merge(base, queryFilter);
    };

    const loadData = (callback: () => void): void => {
        if (route.query.q !== undefined) {
            flowStore
                .searchFlows(loadQuery({
                    size: parseInt(String(route.query.size) || "25"),
                    page: parseInt(String(route.query.page) || "1"),
                    sort: String(route.query.sort || "")
                }))
                .finally(() => {
                    saveRestoreUrl();
                    callback();
                });
        } else {
            flowStore.total = 0;
            flowStore.search = undefined;
            callback();
        }
    };

    const onPageChanged = (): void => {
        loadData(() => {});
    };

    const onDataTableValue = (field: string, value: string | number | boolean): void => {
        const query: LocationQueryRaw = {
            ...route.query,
            [field]: value.toString()
        };
        router.push({query});
    };
</script>
