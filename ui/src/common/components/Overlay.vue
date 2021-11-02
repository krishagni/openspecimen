
<template>
  <OverlayPanel ref="op" appendTo="body" :dismissable="isDismissable" :showCloseIcon="!isDismissable">
    <slot></slot>
  </OverlayPanel>
</template>

<script>

import OverlayPanel from 'primevue/overlaypanel';

export default {
  props: ['dismissable'],

  components: {
    OverlayPanel
  },

  emits: ['show', 'hide'],

  mounted() {
    this.$watch(() => this.$refs.op.visible, (visibility) => this.emitShowHide(visibility));
  },

  computed: {
    isDismissable: function() {
      return this.dismissable != false;
    }
  },

  methods: {
    toggle: function(event) {
      this.$refs.op.toggle(event);
    },

    emitShowHide: function(visibility) {
      this.$emit(visibility ? 'show' : 'hide');
    }
  }
}

</script>
