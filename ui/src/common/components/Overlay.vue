
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

  methods: {
    toggle: function(event) {
      this.$refs.op.toggle(event);
    }
  }
}

</script>
