
<template>
  <div class="os-user-feedback" v-if="$ui.global.appProps.feedback_enabled"
    v-os-tooltip.bottom="$t('common.feedback.submit')">
    <button @click="showForm">
      <os-icon name="bullhorn" />
    </button>

    <os-dialog ref="dialog">
      <template #header>
        <span v-t="'common.feedback.title'">Your feedback counts!</span>
      </template>
      <template #content>
        <os-message type="info">
          <span v-t="'common.feedback.let_us_know'">Please let us know what you think of OpenSpecimen. Your feedback to improve the product is most welcome.</span>
        </os-message>

        <os-form ref="form" :schema="formSchema" @input="handleInput($event)">
          <div>
            <os-button primary :label="$t('common.buttons.submit')" @click="submit()" />
            <os-button text :label="$t('common.buttons.cancel')" @click="cancel()" />
          </div>
        </os-form>
      </template>
    </os-dialog>
  </div>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import commonSvc from '@/common/services/Common.js';

export default {
  data() {
    const i18n = this.$t;

    return {
      feedback: {},

      formSchema: {
        "rows": [
          {
            "fields": [
              {
                "type": "text",
                "label": i18n('common.feedback.subject'),
                "name": "subject",
                "validations": {
                  "required": {
                    "message": i18n('common.feedback.subject_req')
                  }
                }
              }
            ]
          },
          {
            "fields": [
              {
                "type": "textarea",
                "label": i18n('common.feedback.feedback'),
                "name": "feedback",
                "rows": 5,
                "validations": {
                  "required": {
                    "message": i18n('common.feedback.feedback_req')
                  }
                }
              }
            ]
          }
        ]
      }
    };
  },

  methods: {
    showForm: function() {
      this.feedback = {};
      this.$refs.dialog.open();
    },

    handleInput: function({data}) {
      Object.assign(this.feedback, data);
    },

    submit: async function() {
      if (!this.$refs.form.validate()) {
        return;
      }

      let result = await commonSvc.submitFeedback(this.feedback);
      if (result == false || result == 'false') {
        alertsSvc.error({code: 'common.feedback.failed_to_send'});
      } else {
        alertsSvc.error({code: 'common.feedback.sent'});
        this.$refs.dialog.close();
      }
    },

    cancel: function() {
      this.$refs.dialog.close();
    }
  }
}
</script>
