<template>
  <os-dialog ref="dialogInstance">
    <template #header>
      <span>Delete Confirmation</span>
    </template>

    <template #content>
      <os-form-group dense>
        <os-cell :width="12">
          <slot name="message"> </slot>
        </os-cell>
      </os-form-group>

      <os-form-group dense v-if="captcha != false">
        <os-cell :width="12">
          <div>Type 'DELETE ANYWAY' to continue.</div>
          <os-input-text v-model="input" />
        </os-cell>
      </os-form-group>

      <os-form-group dense v-if="collectReason == true">
        <os-cell :width="12">
          <div>Specify the reason, at least 10 characters in length, for deletion:</div>
          <os-textarea v-model="reason" :rows="3" />
        </os-cell>
      </os-form-group>
    </template>

    <template #footer>
      <os-button text label="Cancel" @click="cancel" />
      <os-button danger label="Yes" @click="proceed" :disabled="disabled" />
    </template>
  </os-dialog>
</template>

<script>

import Button from '@/common/components/Button.vue';
import Col from '@/common/components/Col.vue';
import Dialog from '@/common/components/Dialog.vue';
import FormGroup from '@/common/components/FormGroup.vue';
import InputText from '@/common/components/InputText.vue';

export default {
  props: ['captcha', 'collectReason'],

  components: {
    'os-button': Button,
    'os-cell': Col,
    'os-dialog': Dialog,
    'os-form-group': FormGroup,
    'os-input-text': InputText
  },

  data() {
    return {
      input: '',

      reason: ''
    }
  },

  computed: {
    disabled: function() {
      if (this.captcha != false && this.input != 'DELETE ANYWAY') {
        return true;
      } else if (this.collectReason == true && (!this.reason || this.reason.length < 10)) {
        return true;
      }

      return false;
    }
  },

  methods: {
    open: function() {
      let self = this;
      return new Promise((resolve) => {
        self.resolve = resolve;
        self.$refs.dialogInstance.open();
      });
    },

    cancel: function() {
      this.input = this.reason = '';
      this.$refs.dialogInstance.close();
      this.resolve = null;
    },

    proceed: function() {
      this.resolve(this.collectReason ? {reason: this.reason} : 'proceed');
      this.input = this.reason = '';
      this.$refs.dialogInstance.close();
      this.resolve = null;
    }
  }
}
</script>
