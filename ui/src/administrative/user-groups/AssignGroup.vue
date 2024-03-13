
<template>
  <os-button left-icon="users" :label="$t('user_groups.add_to_group')" right-icon="caret-down"
      @click="toggleGroupOptions" />

  <os-overlay class="os-assign-user-group" ref="groupOptions">
    <ul class="search">
      <li>
        <os-input-text v-model="searchTerm" :placeholder="$t('user_groups.search_group')"
          @update:modelValue="searchGroups" />
      </li>
    </ul>
    <ul class="groups">
      <li v-for="(group, idx) of groups" :key="idx" @click="addToGroup($event, group)">
        <a> {{group.name}} </a>
      </li>

      <li v-if="groups.length == 0">
        <a class="no-click" v-t="'user_groups.no_groups'"> No groups to show </a>
      </li>
    </ul>
    <ul class="actions">
      <li>
        <a @click="createNewGroup">
          <span v-t="'user_groups.create_new'">Create New </span>
        </a>
      </li>
      <li>
        <a @click="viewGroups" v-if="groups.length > 0">
          <span v-t="'user_groups.manage_groups'">Manage Groups </span>
        </a>
      </li>
    </ul>
  </os-overlay>
</template>

<script>
import routerSvc    from '@/common/services/Router.js';
import userGroupSvc from '@/administrative/services/UserGroup.js';

export default {
  emits: ['addToGroup'],

  data() {
    return {
      searchTerm: '',

      groups: []
    }
  },

  methods: {
    toggleGroupOptions: function(event) {
      this.$refs.groupOptions.toggle(event);
      if (this.groupsLoaded) {
        return;
      }

      this.groupsLoaded = true;
      this.searchGroups(null);
    },

    searchGroups: function(searchTerm) {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
        this.searchTimer = null;
      }

      if (!searchTerm && this.defGroups) {
        this.groups = this.defGroups;
        return;
      }

      this.searchTimer = setTimeout(
        () => {
          if (this.defGroups && this.defGroups.length < 100) {
            searchTerm = searchTerm.toLowerCase();
            this.groups = this.defGroups.filter(group => group.name.toLowerCase().indexOf(searchTerm) != -1);
          } else {
            this.getGroups(searchTerm).then(
              groups => {
                this.groups = groups;
                if (!searchTerm) {
                  this.defGroups = groups;
                }
              }
            );
          }
        },
        searchTerm ? 500 : 0
      );
    },

    getGroups: function(searchTerm) {
      return userGroupSvc.getGroups({query: searchTerm});
    },

    addToGroup: function(event, group) {
      if (event) {
        this.$refs.groupOptions.toggle(event);
      }

      this.$emit('addToGroup', group);
    },

    createNewGroup: function(event) {
      this.addToGroup(event, null);
    },

    viewGroups: function() {
      routerSvc.goto('UserGroupsList');
    }
  }
}
</script>

<style scoped>
.os-assign-user-group .os-input-text {
  padding: 0.5rem 1rem;
}

.os-assign-user-group ul {
  margin: 0.5rem -1.25rem;
  padding: 0rem;
  padding-bottom: 0.5rem;
  list-style: none;
  border-bottom: 1px solid #ddd;
}

.os-assign-user-group ul.groups {
  max-height: 190px;
  overflow: scroll;
}

.os-assign-user-group ul:last-child {
  margin-bottom: 0rem;
  border-bottom: 0px;
}

.os-assign-user-group ul li a {
  padding: 0.5rem 1rem;
  text-decoration: none;
  display: inline-block;
  width: 100%;
  color: #212529;
}

.os-assign-user-group ul li a:hover:not(.no-click) {
  background: #e9ecef;
}

.os-assign-user-group ul li a.no-click {
  cursor: initial;
}
</style>

<style>
.os-assign-user-group .p-overlaypanel-content {
  padding: 0.5rem 1.25rem;
}
</style>
