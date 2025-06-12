<template>
  <os-dialog ref="dialogInstance">
    <template #header>
      <span v-t="'common.close_specimen.title'">Close Specimen</span>
    </template>
    <template #content>
      <os-form ref="form" :data="closeDetail" :schema="formSchema" @input="handleInput($event)" />
    </template>
    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />

      <os-button primary :label="$t('common.buttons.close')" @click="close" />
    </template>
  </os-dialog>
</template>

<script>
import specimenSvc from '@/biospecimen/services/Specimen.js';

export default {
  props: ['specimens'],

  data() {
    const i18n = this.$t;

    return {
      closeDetail: { },

      formSchema: {
        rows: [
          {
            fields: [
              {
                type: "user",
                label: i18n("common.close_specimen.user"),
                name: "user",
                validations: {
                  required: {
                    message: i18n("common.close_specimen.user_req")
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: "datePicker",
                label: i18n("common.close_specimen.date_time"),
                name: "date",
                showTime: true,
                validations: {
                  required: {
                    message: i18n("common.close_specimen.date_time_req")
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: "pv",
                attribute: "specimen_dispose_reason",
                selectProp: "value",
                label: i18n("common.close_specimen.reason"),
                name: "reason",
                validations: {
                  required: {
                    message: i18n("common.close_specimen.reason_req"),
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: "textarea",
                label: i18n("common.comments"),
                name: "comments",
                rows: 5
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

      const cs      = this.closeDetail;
      const payload = (this.specimens || []).map((specimen) => ({id: specimen.id, status: 'Closed', ...cs}));
      specimenSvc.bulkUpdateStatus(payload).then(
        (response) => {
          this.$refs.dialogInstance.close();
          this.resolve(response);
        }
      );
    },

    cancel: function() {
      this.$refs.dialogInstance.close();
    }
  }
}
</script>
