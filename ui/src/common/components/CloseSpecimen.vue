
<template>
  <os-dialog ref="dialogInstance">
    <template #header>
      <span>Close Specimen</span>
    </template>
    <template #content>
      <os-form ref="form" :data="closeDetail" :schema="formSchema" @input="handleInput($event)" />
    </template>
    <template #footer>
      <os-button text label="Cancel" @click="cancel" />

      <os-button primary label="Close" @click="close" />
    </template>
  </os-dialog>
</template>

<script>
import specimenSvc from '@/biospecimen/services/Specimen.js';

export default {
  props: ['specimens'],

  data() {
    return {
      closeDetail: { },

      formSchema: {
        rows: [
          {
            fields: [
              {
                type: "user",
                label: "User",
                name: "user",
                validations: {
                  required: {
                    message: "User is mandatory"
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: "datePicker",
                label: "Date and Time",
                name: "date",
                showTime: true,
                validations: {
                  required: {
                    message: "Date and time is mandatory"
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: "textarea",
                label: "Reason",
                name: "reason",
                rows: 5,
                validations: {
                  required: {
                    message: "Reason is mandatory"
                  }
                }
              }
            ]
          }
        ]
      }
    }
  },

  methods: {
    open: function() {
      let self = this;
      this.initSpec();
      return new Promise((resolve) => {
        self.resolve = resolve;
        self.$refs.dialogInstance.open();
      });
    },

    initSpec: function() {
      this.closeDetail = {
        user: this.$ui.currentUser,
        date: Date.now()
      }
    },

    handleInput: function({data}) {
      Object.assign(this.closeDetail, data);
    },

    close: function() {
      if (!this.$refs.form.validate()) {
        return;
      }

      const cs = this.closeDetail;
      const specs = (this.specimens || []).map((specimen) => ({id: specimen.id, status: 'Closed', ...cs}));
      specimenSvc.bulkUpdateStatus(specs).then(
        () => {
          this.$refs.dialogInstance.close();
          this.resolve('closed');
        }
      );
    },

    cancel: function() {
      this.$refs.dialogInstance.close();
    }
  }
}
</script>
