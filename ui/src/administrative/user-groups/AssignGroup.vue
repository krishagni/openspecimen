
<template>
  <span>
    <os-button left-icon="users" label="Add to Group" right-icon="caret-down" @click="toggle" />

    <dropdown-menu class="os-assign-user-group" ref="menu" :model="ctx.items" :popup="true">
      <template #item="{item}">
        <span v-if="item.search">
          <os-input-text v-model="ctx.search" placeholder="Search Group" />
        </span>
        <span v-else-if="item.items && item.items.length > 0">
          <dropdown-menu :model="item.items" :popup="false" />
        </span>
      </template>
    </dropdown-menu>
  </span>
</template>

<script>
import { reactive, ref } from 'vue';
import Menu from 'primevue/menu';

import Button from '@/common/components/Button.vue';
import InputText from '@/common/components/InputText.vue';

import routerSvc from '@/common/services/Router.js';
import userGroupSvc from '@/administrative/services/UserGroup.js';

export default {
  emits: ['addToGroup'],

  components: {
    'os-button': Button,
    'os-input-text': InputText,
    'dropdown-menu': Menu
  },

  setup(props, { emit }) {
    let ctx = reactive({});
    let menu = ref();

    ctx.items = [
      {search: true},
      {separator: true},
      { label: '', items: [] },
      {separator: true},
      {
        label: '',
        items: [ 
          {
            label: 'Create New',
            command: () => {
              menu.value.toggle();
              emit('addToGroup')
            }
          },
          {
            label: 'Manage Groups',
            command: () => routerSvc.goto('UserGroupsList')
          }
        ]
      }
    ];

    const loadUserGroups = async function(searchTerm) {
      if (!searchTerm && ctx.defGroups) {
        return ctx.defGroups;
      }

      if (ctx.defGroups && ctx.defGroups.length < 100) {
        return ctx.defGroups.filter(group => group.name.toLowerCase().indexOf(searchTerm.toLowerCase()) != -1);
      }

      return userGroupSvc.getGroups({query: searchTerm}).then(
        groups => {
          if (!searchTerm) {
            ctx.defGroups = groups;
          }

          groups.forEach(group => {
            group.label = group.name;
            group.command = () => {
              menu.value.toggle();
              emit('addToGroup', group);
            }
          });

          return groups;
        }
      );
    }

    const toggle = (event) => {
      menu.value.toggle(event);
      loadUserGroups(ctx.search).then(groups => ctx.items[2].items = groups);
    }

    return { ctx, menu, loadUserGroups, toggle };
  },

  methods: {
    debounce: (function() {
      let timeout = null;
      return function(fn, delayMs) {
        clearTimeout(timeout);
        timeout = setTimeout(() => { fn(); }, delayMs || 500);
      };
    })()
  },

  watch: {
    'ctx.search': function(searchTerm) {
      let self = this;
      this.debounce(() => self.loadUserGroups(searchTerm).then(groups => self.ctx.items[2].items = groups));
    }
  }
}
</script>

<style>

/* minimum width of the user group menu */
.os-assign-user-group {
  min-width: 250px!important;
}

.os-assign-user-group .os-input-text {
  padding: 0.75rem 1rem;
}

/* make the search menu as big as the user group menu */
.os-assign-user-group .os-input-text input {
  width: 100%;
}

/* fix the height of submenus to ensure a scroll appears */
/* mostly for dynamic group names list */
.os-assign-user-group .p-submenu-header {
  padding: 0px;
  max-height: 150px;
  overflow: auto;
}

.os-assign-user-group .p-submenu-header .p-menu {
  border: 0px;
  width: 100%;
  padding: 0px;
}

</style>
