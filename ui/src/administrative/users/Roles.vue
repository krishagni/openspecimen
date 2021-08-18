
<template>
  <div>
    <Panel>
      <template #header>
        <span>
          <span class="title">Roles</span>
          <Button left-icon="plus" label="Add Role" @click="showAddEditRole" />
        </span>
      </template>

      <table class="os-table" v-if="ctx.userRoles && ctx.userRoles.length > 0">
        <thead>
          <tr>
            <th>Site</th>
            <th>Collection Protocol</th>
            <th>Role</th>
            <th>&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="userRole of ctx.userRoles" :key="userRole.id">
            <td>
              <span v-if="!userRole.site">All Current and Future</span>
              <span v-else>{{userRole.site.name}}</span>
            </td>
            <td>
              <span v-if="!userRole.collectionProtocol">All Current and Future</span>
              <span v-else>{{userRole.collectionProtocol.shortTitle}}</span>
            </td>
            <td>
              <span>{{userRole.role.name}}</span>
            </td>
            <td>
              <Button left-icon="edit" size="small" @click="showAddEditRole(userRole)" />
              <Button left-icon="trash" size="small" @click="deleteRole(userRole)" />
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else>
        <Message type="info">
          <span>No roles to display. Add a new role by clicking on Add Role button.</span>
        </Message>
      </div>
    </Panel>

    <Dialog ref="roleFormDialog">
      <template #header>
        <span v-if="!ctx.role.id">Add Role</span>
        <span v-else>Update Role</span>
      </template>
      <template #content>
        <Form ref="roleForm" :schema="ctx.roleFs" :data="ctx.role" @input="handleRoleChange($event)" />
      </template>
      <template #footer>
        <Button label="Cancel" @click="cancelAddEditRole" />
        <Button label="Add" @click="saveRole" v-if="!ctx.role.id"/>
        <Button label="Update" @click="saveRole" v-else />
      </template>
    </Dialog>
  </div>
</template>

<script>

import Button from '@/common/components/Button.vue';
import Dialog from '@/common/components/Dialog.vue';
import Panel from '@/common/components/Panel.vue';
import Message from '@/common/components/Message.vue';
import Form from '@/common/components/Form.vue';

import userSvc from '@/administrative/services/User.js';
import siteSvc from '@/administrative/services/Site.js';
import rolesSvc from '@/administrative/services/Role.js';
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';

export default {
  props: ['user'],

  inject: ['ui'],

  components: {
    Button,
    Panel,
    Message,
    Dialog,
    Form
  },

  data() {
    let self = this;
    return {
      ctx: {
        roleFs: {
          rows: [
            {
              fields: [
                {
                  type: "dropdown", name: "site", label: "Site",
                  listSource: {
                    loadFn: (opts) => self.loadSites(opts)
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: "dropdown", name: "cp", label: "Collection Protocol",
                  listSource: {
                    loadFn: (opts) => self.loadCps(opts)
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: "dropdown", name: "role", label: "Role",
                  listSource: {
                    loadFn: (opts) => self.loadRoles(opts)
                  }
                }
              ]
            }
          ]
        }
      }
    };
  },

  created() {
    userSvc.getRoles(this.user).then((roles) => this.ctx.userRoles = roles);
  },

  methods: {
    saveRole: function() {
      let role = {
        id: this.ctx.role.id,
        site: {name: this.ctx.role.site},
        collectionProtocol: {shortTitle: this.ctx.role.cp},
        role: {name: this.ctx.role.role}
      }

      let self = this;
      userSvc.updateRole(this.user, role).then(
        function() {
          userSvc.getRoles(self.user).then((roles) => self.ctx.userRoles = roles);
          self.cancelAddEditRole();
        }
      );
    },

    deleteRole: function(userRole) {
      let idx = this.ctx.userRoles.indexOf(userRole);
      userSvc.deleteRole(this.user, userRole).then(() => this.ctx.userRoles.splice(idx, 1));
    },

    showAddEditRole: function(userRole) {
      if (userRole) {
        let site = userRole.site && userRole.site.name;
        let cp   = userRole.collectionProtocol && userRole.collectionProtocol.shortTitle;
        let role = userRole.role && userRole.role.name;
        this.ctx.role = {id: userRole.id, site: site, cp: cp, role: role};
      } else {
        this.ctx.role = {};
      }

      this.$refs.roleFormDialog.open();
    },

    cancelAddEditRole: function() {
      this.$refs.roleFormDialog.close();
    },

    handleRoleChange: function(event) {
      Object.assign(this.ctx.role, event.data);
    },

    loadSites: function(opts) {
      opts = opts || {};
      opts.name = opts.query;
      if (this.ui.currentUser.admin) {
        opts.institute = this.user.instituteName;
        opts.listAll = false;
      } else {
        opts.resource = 'User';
        opts.operation = 'Update';
      }

      return siteSvc.getSites(opts)
        .then((sites) => sites.map((site) => site.name));
    },

    loadCps: function(opts) {
      opts = opts || {};
      opts.repositoryName = this.ctx.role.site;
      opts.instituteId = this.user.instituteId;
      return cpSvc.getCps(opts)
        .then((cps) => cps.map((cp) => cp.shortTitle));
    },

    loadRoles: function() {
      return rolesSvc.getRoles().then((roles) => roles.map((role) => role.name));
    }
  }
}

</script>
