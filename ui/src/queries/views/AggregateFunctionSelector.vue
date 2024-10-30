
<template>
  <div>
    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;">
      <os-boolean-checkbox v-model="ctx.aggFns.count"
        :inline-label="$t('queries.count')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.count_desc" style="flex: 1;"
        v-if="ctx.aggFns.count" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;">
      <os-boolean-checkbox v-model="ctx.aggFns.c_count"
        :inline-label="$t('queries.c_count')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.c_count_desc" style="flex: 1;"
        v-if="ctx.aggFns.c_count" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;"
      v-if="field.type != 'STRING'">
      <os-boolean-checkbox v-model="ctx.aggFns.sum"
        :inline-label="$t('queries.sum')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.sum_desc" style="flex: 1;"
        v-if="ctx.aggFns.sum" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;"
      v-if="field.type != 'STRING'">
      <os-boolean-checkbox v-model="ctx.aggFns.c_sum"
        :inline-label="$t('queries.c_sum')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.c_sum_desc" style="flex: 1;"
        v-if="ctx.aggFns.c_sum" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;"
      v-if="field.type == 'INTEGER' || field.type == 'FLOAT'">
      <os-boolean-checkbox v-model="ctx.aggFns.min"
        :inline-label="$t('queries.min')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.min_desc" style="flex: 1;"
        v-if="ctx.aggFns.min" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;"
      v-if="field.type == 'INTEGER' || field.type == 'FLOAT'">
      <os-boolean-checkbox v-model="ctx.aggFns.max"
        :inline-label="$t('queries.max')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.max_desc" style="flex: 1;"
        v-if="ctx.aggFns.max" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;"
      v-if="field.type == 'INTEGER' || field.type == 'FLOAT'">
      <os-boolean-checkbox v-model="ctx.aggFns.avg"
        :inline-label="$t('queries.avg')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.avg_desc" style="flex: 1;"
        v-if="ctx.aggFns.avg" />
    </div>
  </div>
</template>

<script>
export default {
  props: ['modelValue', 'label', 'field'],

  computed: {
    aggFns: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  watch: {
    'ctx.aggFns': {
      handler: function(selectedFns) {
        const aggFns = [];

        selectedFns = selectedFns || {};
        for (let fn of ['count', 'c_count', 'sum', 'c_sum', 'min', 'max', 'avg']) {
          if (selectedFns[fn]) {
            aggFns.push({name: fn, desc: selectedFns[fn + '_desc'] || (fn + '(' + this.label + ')')});
          }
        }

        this.aggFns = aggFns
      },

      deep: true
    }
  },

  data() {
    return { ctx: { aggFns: {} } }
  },

  created() {
    const aggFns = {};
    for (let aggFn of this.aggFns || []) {
      aggFns[aggFn.name] = true;
      aggFns[aggFn.name + '_desc'] = aggFn.desc;
    }

    this.ctx.aggFns = aggFns;
  }
}
</script>

