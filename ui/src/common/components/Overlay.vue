
<template>
  <OverlayPanel ref="op" appendTo="body">
    <slot></slot>
  </OverlayPanel>
</template>

<script>

import OverlayPanel from 'primevue/overlaypanel';

export default {
  components: {
    OverlayPanel
  },

  emits: ['show', 'hide'],

  mounted() {
    this.$watch(() => this.$refs.op.visible, (visibility) => this.emitShowHide(visibility));
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
