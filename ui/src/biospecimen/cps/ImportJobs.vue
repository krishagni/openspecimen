<template>
  <os-panel>
    <template #header>
      <span v-t="'import.jobs_list'">Bulk Import Jobs</span>
    </template>

    <template #actions>
      <os-button left-icon="sync" :label="$t('common.buttons.refresh')" @click="refreshJobs" />
    </template>

    <template #default>
      <os-import-jobs ref="jobsListView" :object-types="['cpe', 'sr']" :object-params="{cpId: cp.id}" />
    </template>
  </os-panel>
</template>

<script>
export default {
  props: ['cp'],

  methods: {
    refreshJobs: function() {
      this.$refs.jobsListView.reload();
    }
  }
}
</script>
