<template>
  <os-dialog ref="columnsDialog" size="lg">
    <template #header>
      <span v-t="'queries.columns'">Columns</span>
    </template>

    <template #content>
      <os-steps ref="columnsWizard">
        <os-step :title="$t('queries.select_fields')">
          <span>UI to show selected fields and select fields</span>
        </os-step>
        <os-step :title="$t('queries.field_labels')">
          <span>UI to assign names to the selected fields</span>
        </os-step>
        <os-step :title="$t('queries.aggregates')">
          <span>Choose aggregate functions</span>
        </os-step>
        <os-step :title="$t('queries.reporting_options')">
          <span>Choose reporting options - pivot, column summary etc</span>
        </os-step>
      </os-steps>
    </template>

    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="closeColumnsDialog" />
      <os-button secondary :label="$t('common.buttons.previous')" @click="previous" v-if="!isFirstStep()" />
      <os-button primary :label="$t('common.buttons.next')" @click="next" v-if="!isLastStep()" />
      <os-button primary :label="$t('common.buttons.done')" @click="closeColumnsDialog" v-else />
    </template>
  </os-dialog> 
</template>

<script>
import util from '@/common/services/Util.js';

export default {
  data() {
    return {
      ctx: {
      }
    }
  },

  methods: {
    open: function(query) {
      const self = this;
      this.ctx.query = util.clone(query);
      return new Promise((resolve) => {
        self.resolve = resolve;
        self.$refs.columnsDialog.open();
      });
    },

    isFirstStep: function() {
      if (!this.$refs.columnsWizard) {
        return false;
      }

      console.log(this.$refs.columnsWizard);
      return this.$refs.columnsWizard.isFirstStep();
    },

    isLastStep: function() {
      if (!this.$refs.columnsWizard) {
        return false;
      }

      return this.$refs.columnsWizard.isLastStep();
    },

    next: function() {
      this.$refs.columnsWizard.next();
    },

    previous: function() {
      this.$refs.columnsWizard.previous();
    },

    closeColumnsDialog: function() {
      this.$refs.columnsDialog.close();
      this.resolve('cancel');
    }
  }
}
</script>


