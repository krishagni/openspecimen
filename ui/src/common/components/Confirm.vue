<template>
  <os-dialog ref="dialogInstance">
    <template #header>
      <slot name="title"></slot>
    </template>

    <template #content>
      <slot name="message"></slot>
    </template>

    <template #footer>
      <os-button text    :label="noButtonLabel"  @click="cancel" />
      <os-button primary :label="yesButtonLabel" @click="proceed" />
    </template>
  </os-dialog>
</template>

<script>

import Button from '@/common/components/Button.vue';
import Dialog from '@/common/components/Dialog.vue';

export default {
  props: ['yesButton', 'noButton'],

  components: {
    'os-button': Button,
    'os-dialog': Dialog
  },

  computed: {
    yesButtonLabel: function() {
      return this.yesButton || this.$t('common.buttons.yes');
    },

    noButtonLabel: function() {
      return this.noButton || this.$t('common.buttons.no');
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
      this.resolve('cancel');
      this.$refs.dialogInstance.close();
      this.resolve = null;
    },

    proceed: function() {
      this.resolve('proceed');
      this.$refs.dialogInstance.close();
      this.resolve = null;
    }
  }
}
</script>
