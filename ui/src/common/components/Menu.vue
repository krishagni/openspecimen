
<template>
  <template v-if="items.length > 1">
    <os-button :size="!label ? 'small' : ''" :left-icon="icon"
      :label="label" :no-outline="noOutline"
      :right-icon="label ? 'caret-down' : ''"
      v-bind="$attrs" @click="toggle" />

    <dropdown-menu class="os-menu" ref="menu" :model="items" :popup="true">
      <template #item="{item}">
        <os-divider v-if="item.divider" />
        <a class="os-menu-item p-menuitem-link" :href="item.url" :style="item.anchorStyle" @click="item.command" v-else>
          <os-icon class="os-menu-item-icon" :name="item.icon" v-if="item.icon" />
          <span class="os-menu-item-label">{{item.label}}</span>
        </a>
      </template>
    </dropdown-menu>
  </template>
  <os-button v-else-if="items.length == 1 && !items[0].url"
    :left-icon="items[0].icon" :label="items[0].label" @click="items[0].command" />
  <os-button-link v-else-if="items.length == 1 && items[0].url"
    :left-icon="items[0].icon" :label="items[0].label" :url="item[0].url" />
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
        ({icon, caption, onSelect, url, divider, anchorStyle}) =>
          ({icon, label: caption, url, divider, anchorStyle, command: (event) => this.exec(event, onSelect)})
      );
    }
  },

  methods: {
    toggle(event) {
      this.$refs.menu.toggle(event);
      this.$emit('menu-toggled');
    },

    exec: function(event, cmd) {
      if (this.$refs.menu) {
        this.$refs.menu.hide();
      }

      if (cmd) {
        cmd(event);
      }
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
  display: flex;
}

.os-menu .os-menu-item .os-menu-item-icon {
  min-width: 2rem;
}

.os-menu .os-menu-item .os-menu-item-label {
  flex: 1;
}
</style>

<style>
.os-menu {
  width: auto;
  max-width: 32rem;
  max-height: 35rem;
  overflow: auto;

  border-radius: 0.5rem;
  margin-top: 0.5rem;
}

.os-menu .p-divider.p-divider-horizontal {
  margin: 0.5rem 0rem!important;
}
</style>
