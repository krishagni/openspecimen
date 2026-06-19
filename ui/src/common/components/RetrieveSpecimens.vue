<template>
  <os-dialog ref="dialogInstance">
    <template #header>
      <span v-t="'common.specimen_actions.retrieve'">Retrieve</span>
    </template>
    <template #content>
      <os-form ref="form" :data="retrieveDetails" :schema="formSchema" />
    </template>
    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
      <os-button primary :label="$t('common.specimen_actions.retrieve')" @click="retrieve" />
    </template>
  </os-dialog>
</template>

<script>
import specimenSvc from '@/biospecimen/services/Specimen.js';

export default {
  props: ['specimens', 'retrieveFn'],

  emits: ['retrieved'],

  data() {
    return {
      retrieveDetails: {},

      formSchema: {
        rows: [
          {
            fields: [
              {
                type: 'user',
                labelCode: 'common.specimen_actions.transfer_user',
                name: 'transferUser',
                validations: {
                  required: {
                    messageCode: 'common.specimen_actions.transfer_user_req'
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: 'datePicker',
                labelCode: 'common.specimen_actions.transfer_time',
                showTime: true,
                name: 'transferTime',
                validations: {
                  required: {
                    messageCode: 'common.specimen_actions.transfer_time_req'
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: 'textarea',
                labelCode: 'common.comments',
                name: 'comments'
              }
            ]
          },
          {
            fields: [
              {
                type: 'booleanCheckbox',
                inlineLabelCode: 'common.specimen_actions.checkout',
                name: 'checkout'
              }
            ]
          }
        ]
      }
    };
  },

  methods: {
    open: function() {
      this.retrieveDetails = {transferUser: this.$ui.currentUser, transferTime: Date.now()};
      this.$refs.dialogInstance.open();
    },

    cancel: function() {
      this.$refs.dialogInstance.close();
    },

    retrieve: async function() {
      if (!this.$refs.form.validate()) {
        return;
      }

      const response = this.specimens != null
        ? await this._retrieveSpecimens(this.retrieveDetails)
        : await this.retrieveFn(this.retrieveDetails);
      this.$refs.dialogInstance.close();
      this.$emit('retrieved', response);
    },

    _retrieveSpecimens: function(input) {
      const specimens = this.specimens.map(
        (specimen) => ({
          id: specimen.id,
          storageLocation: {},
          transferUser: input.transferUser,
          transferTime: input.transferTime,
          transferComments: input.comments,
          checkout: input.checkout
        })
      );

      return specimenSvc.bulkUpdate(specimens);
    }
  }
};
</script>
