
<template>
  <os-dialog ref="dialog">
    <template #header>
      <span>Broadcast announcement to active users</span>
    </template>

    <template #content>
      <os-form-group dense>
        <os-cell :width="12">
          <os-label>Subject</os-label>
          <os-input-text v-model="announcement.subject" />
        </os-cell>
      </os-form-group>

      <os-form-group dense>
        <os-cell :width="12">
          <os-label>Message</os-label>
          <os-textarea rows="5" v-model="announcement.message" />
        </os-cell>
      </os-form-group>
    </template>

    <template #footer>
      <os-button label="Cancel" @click="close"/>
      <os-button label="Send" @click="send"/>
    </template>
  </os-dialog>
</template>

<script>

import Dialog from '@/common/components/Dialog.vue';
import FormGroup from '@/common/components/FormGroup.vue';
import Col from '@/common/components/Col.vue';
import Label from '@/common/components/Label.vue';
import InputText from '@/common/components/InputText.vue';
import Textarea from '@/common/components/Textarea.vue';
import Button from '@/common/components/Button.vue';

import alertSvc from '@/common/services/Alerts.js';
import userSvc from '@/administrative/services/User.js';

export default {
  components: {
    'os-dialog': Dialog,
    'os-form-group': FormGroup,
    'os-cell': Col,
    'os-label': Label,
    'os-input-text': InputText,
    'os-textarea': Textarea,
    'os-button': Button
  },

  data() {
    return {
      announcement: {}
    }
  },

  methods: {
    open: function() {
      this.announcement = {};
      this.$refs.dialog.open();
    },

    close: function() {
      this.$refs.dialog.close();
    },

    send: function() {
      userSvc.broadcast(this.announcement).then(
        () => {
          alertSvc.success('Announcement broadcasted to active users!');
          this.close();
        }
      );
    }
  }
}
</script>
