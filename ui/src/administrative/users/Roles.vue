
<template>
  <div>
    <os-panel>
      <template #header>
        <span>
          <span class="title">Roles</span>
          <os-button left-icon="plus" label="Add Role" @click="showAddEditRole" v-if="!hideAddRole && updateAllowed" />
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
              <os-button-group v-if="isRoleUpdateAllowed(userRole)">
                <os-button left-icon="edit" size="small" @click="showAddEditRole(userRole)" />
                <os-button left-icon="trash" size="small" @click="deleteRole(userRole)" />
              </os-button-group>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else>
        <os-message type="info">
          <span>No roles to display. Add a new role by clicking on Add Role button.</span>
        </os-message>
      </div>
    </os-panel>

    <os-dialog ref="roleFormDialog">
      <template #header>
        <span v-if="!ctx.role.id">Add Role</span>
        <span v-else>Update Role</span>
      </template>
      <template #content>
        <os-form ref="roleForm" :schema="ctx.roleFs" :data="ctx.role" @input="handleRoleChange($event)" />
      </template>
      <template #footer>
        <os-button label="Cancel" @click="cancelAddEditRole" />
        <os-button label="Add" @click="saveRole" v-if="!ctx.role.id"/>
        <os-button label="Update" @click="saveRole" v-else />
      </template>
    </os-dialog>
  </div>
</template>

<script>

import userSvc from '@/administrative/services/User.js';
import siteSvc from '@/administrative/services/Site.js';
import rolesSvc from '@/administrative/services/Role.js';
import userResources from '@/administrative/users/Resources.js';
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import authSvc from '@/common/services/Authorization.js';

export default {
  props: ['user'],

  inject: ['ui'],

  data() {
    let self = this;
    return {
      allSites: {name: 'All Current and Future'},

      allCps: {shortTitle: 'All Current and Future'},

      ctx: {
        roleFs: {
          rows: [
            {
              fields: [
                {
                  type: "dropdown", name: "site", label: "Site",
                  listSource: {
                    displayProp: 'name', selectProp: 'name',
                    loadFn: (opts) => self.loadSites(opts)
                  },
                  validations: {
                    required: {
                      message: "Site is mandatory"
                    }
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: "dropdown", name: "cp", label: "Collection Protocol",
                  listSource: {
                    displayProp: 'shortTitle', selectProp: 'shortTitle',
                    loadFn: (opts) => self.loadCps(opts)
                  },
                  validations: {
                    required: {
                      message: "Collection Protocol is mandatory"
                    }
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: "dropdown", name: "role", label: "Role",
                  listSource: {
                    displayProp: 'name', selectProp: 'name',
                    loadFn: (opts) => self.loadRoles(opts)
                  },
                  validations: {
                    required: {
                      message: "Role is mandatory"
                    }
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
    },

    updateAllowed: function() {
      return userResources.isUpdateAllowed();
    }
  },

  methods: {
    isRoleUpdateAllowed: function(role) {
      if (this.ui.currentUser.admin) {
        return true;
      }

      if (role.site && role.site.name != this.allSites.name) {
        return authSvc.isAllowed({resource: 'User', operations: ['Update'], sites: [role.site.name]});
      } else if (this.ui.currentUser.instituteName == this.user.instituteName) {
        return userResources.isUpdateAllowed();
      }

      return false;
    },

    saveRole: function() {
      if (!this.$refs.roleForm.validate()) {
        return;
      }

      let input = this.ctx.role;
      let role = {
        id: input.id,
        site: input.site == this.allSites.name ? undefined : { name: input.site },
        collectionProtocol: input.cp == this.allCps.shortTitle ? undefined : { shortTitle: input.cp },
        role: { name: input.role }
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
        let site = (userRole.site && userRole.site.name) || this.allSites.name;
        let cp   = (userRole.collectionProtocol && userRole.collectionProtocol.shortTitle) || this.allCps.shortTitle;
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
        .map((userRole) => !userRole.site ? self.allSites.name : userRole.site.name);

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
          let result = sites.map((site) => ({name: site.name}));
          if (userRoles.length == 0 || !hasSite) {
            result.splice(0, 0, self.allSites);
          }

          return result.filter((site) => allCpSites.indexOf(site.name) == -1 || self.ctx.role.site == site.name);
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
      if (this.ctx.role.site != this.allSites.name) {
        opts.repositoryName = this.ctx.role.site;
      } else {
        opts.instituteId = this.user.instituteId;
      }

      return cpSvc.getCps(opts).then(
        (cps) => {
          let result = cps.map((cp) => ({shortTitle: cp.shortTitle}))
            .filter((cp) => cpsToRemove.indexOf(cp.shortTitle) == -1 || self.ctx.role.cp == cp.shortTitle);

          if (allCps) {
            result.splice(0, 0, self.allCps);
          }

          return result;
        }
      );
    },

    loadRoles: function() {
      return rolesSvc.getRoles().then((roles) => roles.map((role) => ({name: role.name})));
    }
  }
}

</script>
