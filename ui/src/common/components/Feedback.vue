
<template>
  <div class="os-user-feedback" v-if="$ui.global.appProps.feedback_enabled" v-os-tooltip.bottom="'Submit Feedback'">
    <button @click="showForm">
      <os-icon name="bullhorn" />
    </button>

    <os-dialog ref="dialog">
      <template #header>
        <span>Your feedback counts!</span>
      </template>
      <template #content>
        <os-message type="info">
          <span>Please let us know what you think of OpenSpecimen. </span>
          <span>Your feedback to improve the product is most welcome.</span>
        </os-message>

        <os-form ref="form" :schema="formSchema" @input="handleInput($event)">
          <div>
            <os-button primary label="Submit" @click="submit()" />
            <os-button label="Cancel" @click="cancel()" />
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
    return {
      feedback: {},

      formSchema: {
        "rows": [
          {
            "fields": [
              {
                "type": "text",
                "label": "Subject",
                "name": "subject",
                "validations": {
                  "required": {
                    "message": "Subject is mandatory"
                  }
                }
              }
            ]
          },
          {
            "fields": [
              {
                "type": "textarea",
                "label": "Feedback",
                "name": "feedback",
                "rows": 5,
                "validations": {
                  "required": {
                    "message": "Feedback is mandatory"
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
        alertsSvc.error('Failed to send the feedback. Contact system administrator for further help!');
      } else {
        alertsSvc.success('Feedback sent successfully!');
        this.$refs.dialog.close();
      }
    },

    cancel: function() {
      this.$refs.dialog.close();
    }
  }
}
</script>
