
<template>
  <os-button :left-icon="icon"
    :label="label" :no-outline="noOutline"
    :right-icon="label ? 'caret-down' : ''"
    v-bind="$attrs" @click="toggle" />

  <dropdown-menu class="os-menu" ref="menu" :model="items" :popup="true">
    <template #item="{item}">
      <os-divider v-if="item.divider" />
      <a class="os-menu-item p-menuitem-link" :style="item.anchorStyle" @click="item.command" v-else>
        <os-icon class="os-menu-item-icon" :name="item.icon" v-if="item.icon" />
        <span>{{item.label}}</span>
      </a>
    </template>
  </dropdown-menu>
</template>

<script>
import Menu from 'primevue/menu';

export default {
  props: ['icon', 'label', 'options', 'no-outline'],

  components: {
    'dropdown-menu': Menu
  },

  emits: ['menu-toggled'],

  computed: {
    items: function() {
      return (this.options || []).map(
        ({icon, caption, onSelect, divider, anchorStyle}) =>
          ({icon, label: caption, divider, anchorStyle, command: (event) => this.exec(event, onSelect)})
      );
    }
  },

  methods: {
    toggle(event) {
      this.$refs.menu.toggle(event);
      this.$emit('menu-toggled');
    },

    exec: function(event, cmd) {
      this.$refs.menu.hide();
      cmd(event);
    }
  },
}
</script>

<style scoped>
.os-menu-item-icon {
  padding-right: 15px;
}

.os-menu .os-menu-item {
  min-width: 12rem;
  padding: 0.5rem 1rem!important;
  color: inherit;
}
</style>

<style>
.os-menu {
  width: auto;
  max-width: 32rem;
  max-height: 35rem;
  overflow: auto;
}

</style>
