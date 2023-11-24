
<template>
  <OverlayPanel class="os-overlay" ref="op" appendTo="body" :dismissable="isDismissable"
    :showCloseIcon="!isDismissable">
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

    show: function(event) {
      this.$refs.op.show(event);
    },

    hide: function(event) {
      this.$refs.op.hide(event);
    },

    emitShowHide: function(visibility) {
      this.$emit(visibility ? 'show' : 'hide');
    }
  }
}

</script>

<style scoped>
.os-overlay ::v-slotted(ul.menu-options-list) {
  margin: -1.25rem;
  list-style: none;
  padding: 0.5rem 0rem;
}

.os-overlay ::v-slotted(ul.menu-options-list li) {
  display: block;
  padding: 0.75rem 1rem;
  transition: box-shadow 0.15s;
}

.os-overlay ::v-slotted(ul.menu-options-list li a) {
  text-decoration: none;
  color: inherit;
}

.os-overlay ::v-slotted(ul.menu-options-list li:not(.divider):hover) {
  background: #e9ecef;
}

.os-overlay ::v-slotted(ul.menu-options-list li.divider) {
  padding: 0.25rem 0rem;
  margin: -1rem 0rem;
}
</style>
