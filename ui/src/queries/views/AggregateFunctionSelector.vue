
<template>
  <div>
    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;">
      <os-boolean-checkbox v-model="ctx.aggFns.count" :inline-label="$t('queries.count')"
        @update:modelValue="handleInput('count')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.count_desc" @update:modelValue="handleInput('count')"
        style="flex: 1;" v-if="ctx.aggFns.count" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;">
      <os-boolean-checkbox v-model="ctx.aggFns.c_count" :inline-label="$t('queries.c_count')"
        @update:modelValue="handleInput('c_count')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.c_count_desc" @update:modelValue="handleInput('c_count')"
        style="flex: 1;" v-if="ctx.aggFns.c_count" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;" v-if="field.type != 'STRING'">
      <os-boolean-checkbox v-model="ctx.aggFns.sum" :inline-label="$t('queries.sum')"
        @update:modelValue="handleInput('sum')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.sum_desc" @update:modelValue="handleInput('sum')"
        style="flex: 1;" v-if="ctx.aggFns.sum" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;" v-if="field.type != 'STRING'">
      <os-boolean-checkbox v-model="ctx.aggFns.c_sum" :inline-label="$t('queries.c_sum')"
        @update:modelValue="handleInput('c_sum')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.c_sum_desc" @update:modelValue="handleInput('c_sum')"
        style="flex: 1;" v-if="ctx.aggFns.c_sum" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;"
      v-if="field.type == 'INTEGER' || field.type == 'FLOAT' || field.type == 'DATE'">
      <os-boolean-checkbox v-model="ctx.aggFns.min" :inline-label="$t('queries.min')"
        @update:modelValue="handleInput('min')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.min_desc" @update:modelValue="handleInput('min')"
        style="flex: 1;" v-if="ctx.aggFns.min" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;"
      v-if="field.type == 'INTEGER' || field.type == 'FLOAT' || field.type == 'DATE'">
      <os-boolean-checkbox v-model="ctx.aggFns.max" :inline-label="$t('queries.max')"
        @update:modelValue="handleInput('max')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.max_desc" @update:modelValue="handleInput('max')"
        style="flex: 1;" v-if="ctx.aggFns.max" />
    </div>

    <div style="display: flex; flex-direction: row; margin-bottom: 1.25rem;"
      v-if="field.type == 'INTEGER' || field.type == 'FLOAT'">
      <os-boolean-checkbox v-model="ctx.aggFns.avg" :inline-label="$t('queries.avg')"
        @update:modelValue="handleInput('avg')" style="flex-basis: 40%;" />
      <os-input-text v-model="ctx.aggFns.avg_desc" @update:modelValue="handleInput('avg')"
        style="flex: 1;" v-if="ctx.aggFns.avg" />
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
    field: function() {
      this._setupFns();
    }
  },

  data() {
    return { ctx: { aggFns: {} } }
  },

  created() {
    this._setupFns();
  },

  methods: {
    handleInput: function(fn) {
      const selectedFns = this.ctx.aggFns || {};

      const selected       = selectedFns[fn];
      const fnDisplayLabel = this.$t('queries.' + fn);
      if (selected) {
        this.ctx.aggFns[fn + '_desc'] = this.ctx.aggFns[fn + '_desc'] || (this.label + ' ' + fnDisplayLabel);
      } else {
        this.ctx.aggFns[fn + '_desc'] = null;
      }

      this._updateModel();
    },

    _setupFns: function() {
      const aggFns = {};
      for (let aggFn of this.aggFns || []) {
        aggFns[aggFn.name] = true;
        aggFns[aggFn.name + '_desc'] = aggFn.desc;
      }

      this.ctx.aggFns = aggFns;
    },

    _updateModel: function() {
      const aggFns = [];
      const selectedFns = this.ctx.aggFns || {};
      for (const fn of ['count', 'c_count', 'sum', 'c_sum', 'min', 'max', 'avg']) {
        if (selectedFns[fn]) {
          aggFns.push({name: fn, desc: selectedFns[fn + '_desc'] || (this.label + ' ' + this.$t('queries.'+ fn))});
        }
      }

      this.aggFns = aggFns
    }
  }
}
</script>

