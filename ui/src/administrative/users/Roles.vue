
<template>
  <div>
    <Panel>
      <template #header>
        <span>
          <span class="title">Roles</span>
          <Button left-icon="plus" label="Add Role" @click="showAddEditRole" v-if="!hideAddRole"/>
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
      all: 'All Current and Future',

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

  computed: {
    hideAddRole: function() {
      return this.ctx.userRoles && this.ctx.userRoles.some((ur) => !ur.site && !ur.collectionProtocol);
    }
  },

  methods: {
    saveRole: function() {
      let input = this.ctx.role;
      let role = {
        id: input.id,
        site: input.site == this.all ? undefined : {name: input.site },
        collectionProtocol: input.cp == this.all ? undefined : {shortTitle: input.cp},
        role: {name: input.role}
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
      if (userRole.id) {
        let site = (userRole.site && userRole.site.name) || this.all;
        let cp   = (userRole.collectionProtocol && userRole.collectionProtocol.shortTitle) || this.all;
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
      const self     = this;
      let userRoles  = this.ctx.userRoles;
      let hasSite    = userRoles.some((userRole) => !!userRole.site);
      let allCpSites = userRoles.filter((userRole) => !userRole.collectionProtocol)
        .map((userRole) => !userRole.site ? self.all : userRole.site.name);

      opts = opts || {};
      opts.name = opts.query;
      if (this.ui.currentUser.admin) {
        opts.institute = this.user.instituteName;
        opts.listAll = false;
      } else {
        opts.resource = 'User';
        opts.operation = 'Update';
      }

      return siteSvc.getSites(opts).then(
        (sites) => {
          let result = sites.map((site) => site.name);
          if (userRoles.length == 0 || !hasSite) {
            result.splice(0, 0, self.all);
          }

          return result.filter((site) => allCpSites.indexOf(site) == -1 || self.ctx.role.site == site);
        }
      );
    },

    loadCps: function(opts) {
      if (!this.ctx.role.site) {
        //
        // no site selected, empty CPs list
        //
        return new Promise((resolve) => resolve([]));
      }

      const self = this;
      const cpsToRemove = [];
      let allCps = true;
      this.ctx.userRoles.forEach(
        (userRole) => {
          if (userRole.collectionProtocol) {
            if (!userRole.site || userRole.site.name == self.ctx.role.site) {
              //
              // CPs that have been selected for all sites or current role site should not
              // appear for selection
              //
              cpsToRemove.push(userRole.collectionProtocol.shortTitle);
            }
          }

          if (!userRole.site || userRole.site.name == this.ctx.role.site) {
            if (userRole.id != this.ctx.role.id) {
              // do not show all CPs when
              // a) there exists a role for all sites with some CP and that role is not being edited
              // b) there exists a role with the current role site and that role is not being edited
              allCps = false;
            }
          }
        }
      );

      opts = opts || {};
      if (this.ctx.role.site != this.all) {
        opts.repositoryName = this.ctx.role.site;
      } else {
        opts.instituteId = this.user.instituteId;
      }

      return cpSvc.getCps(opts).then(
        (cps) => {
          let result = cps.map((cp) => cp.shortTitle)
            .filter((cp) => cpsToRemove.indexOf(cp) == -1 || self.ctx.role.cp == cp);

          if (allCps) {
            result.splice(0, 0, self.all);
          }

          return result;
        }
      );
    },

    loadRoles: function() {
      return rolesSvc.getRoles().then((roles) => roles.map((role) => role.name));
    }
  }
}

</script>
