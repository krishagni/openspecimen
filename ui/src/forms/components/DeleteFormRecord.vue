<template>
  <os-dialog ref="dialogInstance">
    <template #header>
      <span>Deleting form record #{{record.recordId}} of {{record.formCaption}}</span>
    </template>

    <template #content>
      <span>Form record #{{record.recordId}} of {{record.formCaption}} will be deleted forever. Are you sure you want to proceed?</span>
    </template>

    <template #footer>
      <os-button text label="Cancel" type="text" @click="cancel" />
      <os-button danger label="Yes" type="primary" @click="proceed" />
    </template>
  </os-dialog>
</template>

<script>

import formSvc from '@/forms/services/Form.js';

export default {
  methods: {
    execute: function(record) {
      let self = this;
      this.record = record;

      return new Promise((resolve) => {
        self.resolve = resolve;
        self.$refs.dialogInstance.open();
      });
    },

    cancel: function() {
      this.$refs.dialogInstance.close();
      this.resolve = null;
    },

    proceed: function() {
      let self = this;
      formSvc.deleteRecord(this.record).then(
        () => {
          self.resolve(self.record);
          self.$refs.dialogInstance.close();
          self.resolve = null;
        }
      );
    }
  }
}
</script>
