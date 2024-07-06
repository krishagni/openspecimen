
<template>
  <span class="field-type-exchange" v-if="allowedTypes && allowedTypes.length > 0">
    <Button
      icon="pi pi-sort-alt"
      class="p-button-text p-button-plain"
      v-tooltip.bottom="'Change Field Type'"
      @click="toggleShowTypes"
    />

    <Menu
      ref="menu"
      :model="allowedTypes"
      :popup="true"
    />
  </span>
</template>

<script>
import Button from 'primevue/button';
import Menu from 'primevue/menu';

import utility from "../services/Utility.js";

export default {
  props: ['field'],


  emits: ['changeTo'],

  components: {
    Menu,
    Button
  },

  methods: {
    toggleShowTypes: function(event) {
      this.$refs.menu.toggle(event);
    }
  },

  computed: {
    allowedTypes: function() {
      let types = utility.getInterchangeableTypes(this.field);
      if (!types || types.length == 0) {
        return [];
      }

      return types.map(type => {
        return {
          label: type.caption,
          command: () => this.$emit('changeTo', type)
        }
      });
    }
  }
}
</script>

<style scoped>
  .field-type-exchange :deep(button.p-button) {
    transform: rotate(90deg);
  }

  .field-type-exchange :deep(.p-menu.p-component.p-menu-overlay) {
    top: inherit!important;
    left: inherit!important;
  }
</style>
