<template>
  <Dialog ref="dialogInstance">
    <template #header>
      <span>Deleting form record #{{record.recordId}} of {{record.formCaption}}</span>
    </template>

    <template #content>
      <span>Form record #{{record.recordId}} of {{record.formCaption}} will be deleted forever. Are you sure you want to proceed?</span>
    </template>

    <template #footer>
      <Button label="Cancel" type="text" @click="cancel" />
      <Button label="Yes" type="primary" @click="proceed" />
    </template>
  </Dialog>
</template>

<script>

import Button from '@/common/components/Button.vue';
import Dialog from '@/common/components/Dialog.vue';

import formSvc from '@/forms/services/Form.js';

export default {
  components: {
    Button,
    Dialog
  },

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
