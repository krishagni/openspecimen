
<template>
  <os-dialog ref="dialog">
    <template #header>
      <span>Broadcast announcement to active users</span>
    </template>

    <template #content>
      <os-form-group dense>
        <os-column :width="12">
          <os-label>Subject</os-label>
          <os-input-text v-model="announcement.subject" />
        </os-column>
      </os-form-group>

      <os-form-group dense>
        <os-column :width="12">
          <os-label>Message</os-label>
          <os-textarea rows="5" v-model="announcement.message" />
        </os-column>
      </os-form-group>
    </template>

    <template #footer>
      <os-button text label="Cancel" @click="close" />
      <os-button primary label="Send" @click="send" />
    </template>
  </os-dialog>
</template>

<script>
import alertSvc from '@/common/services/Alerts.js';
import userSvc from '@/administrative/services/User.js';

export default {
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
