<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>
          <span v-if="!dataCtx.role.id" v-t="'roles.create_role'">Create Role</span>
          <span v-else v-t="{path: 'common.update', args: dataCtx.role}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form class="roles-matrix" ref="roleForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <template #static-fields>
          <div class="row">
            <div class="field">
              <os-label>
                <span v-t="'roles.name'">Name</span>
                <span class="required-indicator" v-os-tooltip.bottom="$t('roles.name_req')">
                  <span>*</span>
                </span>
              </os-label>
              <os-input-text v-model="dataCtx.role.name" @update:model-value="this.dataCtx.nameDirty = true" />
              <div v-if="this.dataCtx.nameDirty && !this.dataCtx.role.name">
                <os-inline-message><span v-t="'roles.name_req'"></span></os-inline-message>
              </div>
            </div>
          </div>

          <div class="row">
            <div class="field">
              <os-label>
                <span v-t="'roles.description'">Description</span>
                <span class="required-indicator" v-os-tooltip.bottom="$t('roles.description_req')">
                  <span>*</span>
                </span>
              </os-label>
              <os-textarea v-model="dataCtx.role.description" @update:model-value="this.dataCtx.descDirty = true" />
              <div v-if="this.dataCtx.descDirty && !this.dataCtx.role.description">
                <os-inline-message><span v-t="'roles.description_req'"></span></os-inline-message>
              </div>
            </div>
          </div>

          <div class="row">
            <div class="field">
              <os-label>
                <span v-t="'roles.permissions'">Permissions</span>
              </os-label>
              <div>
                <table class="os-table">
                  <thead>
                    <tr>
                      <th><span v-t="'roles.resource'"></span></th>
                      <th><span v-t="'roles.ops.Read'"></span></th>
                      <th><span v-t="'roles.ops.Create'"></span></th>
                      <th><span v-t="'roles.ops.Update'"></span></th>
                      <th><span v-t="'roles.ops.Delete'"></span></th>
                      <th><span v-t="'roles.ops.Export Import'"></span></th>
                      <th v-if="showLockUnlock"><span v-t="'roles.ops.Lock'"></span></th>
                      <th v-if="showLockUnlock"><span v-t="'roles.ops.Unlock'"></span></th>
                      <th>&nbsp;</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(el, idx) of dataCtx.role.acl" :key="idx">
                      <td>
                        <os-dropdown v-model="el.name" :list-source="ctx.resourcesLs" />
                      </td>
                      <td>
                        <os-boolean-checkbox v-model="el.read" @update:model-value="readToggled(el)" />
                      </td>
                      <td>
                        <os-boolean-checkbox v-model="el.create" @update:model-value="createToggled(el)" />
                      </td>
                      <td>
                        <os-boolean-checkbox v-model="el.update" @update:model-value="updateToggled(el)" />
                      </td>
                      <td>
                        <os-boolean-checkbox v-model="el.del" />
                      </td>
                      <td>
                        <os-boolean-checkbox v-model="el.expImp" />
                      </td>
                      <td v-if="showLockUnlock">
                        <os-boolean-checkbox v-model="el.lock"
                          v-show="el.name == 'SurgicalPathologyReport' || (hasEc && el.name == 'Consent')" />
                      </td>
                      <td v-if="showLockUnlock">
                        <os-boolean-checkbox v-model="el.unlock"
                          v-show="el.name == 'SurgicalPathologyReport' || (hasEc && el.name == 'Consent')" />
                      </td>
                      <td>
                        <os-button left-icon="times" size="small" @click="removeResource(idx)" />
                      </td>
                    </tr>
                  </tbody>
                </table>

                <os-button class="inline-button" left-icon="plus"
                  :label="$t(dataCtx.role.acl && dataCtx.role.acl.length > 0 ?
                           'common.buttons.add_another' : 'common.buttons.add')"
                  @click="addResource" />
              </div>
            </div>
          </div>
        </template>

        <div>
          <os-button primary :label="$t(!dataCtx.role.id ? 'common.buttons.create' : 'common.buttons.update')"
            @click="saveOrUpdate" />
          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc  from '@/common/services/Alerts.js';
import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';

import roleSvc   from '@/administrative/services/UserRole.js';

export default {
  props: ['roleId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('UserRolesList', {roleId: -1}), label: i18n.msg('roles.list')}
      ],

      addEditFs: {rows: []},

      resourcesLs: {
        selectProp: 'name',
        displayProp: 'label',
        options: []
      }
    });

    let dataCtx = reactive({
      role: { acl: [] },

      currentUser: ui.currentUser
    });

    const promises = [];
    if (props.roleId && +props.roleId > 0) {
      promises.push(roleSvc.getRole(+props.roleId));
    }

    promises.push(roleSvc.getResources());
    Promise.all(promises).then(
      (resps) => {
        let respIdx = -1;
        if (promises.length > 1) {
          const role = dataCtx.role = resps[++respIdx];
          for (let el of role.acl || []) {
            el.name = el.resourceName;
            for (let {operationName} of el.operations) {
              if (operationName == 'Read') {
                el.read = true;
              } else if (operationName == 'Create') {
                el.create = true;
              } else if (operationName == 'Update') {
                el.update = true;
              } else if (operationName == 'Delete') {
                el.del = true;
              } else if (operationName == 'Export Import') {
                el.expImp = true;
              } else if (operationName == 'Lock') {
                el.lock = true;
              } else if (operationName == 'Unlock') {
                el.unlock = true;
              }
            }

            delete el.resourceName;
            delete el.operations;
          }

          role.acl = role.acl.sort(({name: n1}, {name: n2}) => n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
        }

        const resources = resps[++respIdx];
        ctx.resourcesLs.options = resources.map(({name}) => ({name, label: i18n.msg('roles.resources.' + name)}));
      }
    );

    return { ctx, dataCtx };
  },

  computed: {
    showLockUnlock: function() {
      return this._showLockUnlock(this.dataCtx.role);
    },

    hasEc: function() {
      return !!this.$osSvc.ecDocSvc;
    }
  },

  methods: {
    handleInput: function(event) {
      Object.assign(this.dataCtx, event.data);
    },

    saveOrUpdate: async function() {
      const {role: {id, name, description, acl}} = this.dataCtx;
      this.dataCtx.nameDirty = this.dataCtx.descDirty = true;
      if (!name || !description) {
        alertSvc.error({code: 'common.form_validation_error'});
        return;
      }

      const payload = {id, name, description, acl: []};
      const seenResources = [];
      for (let el of acl) {
        const operations = [];
        if (el.read)   { operations.push({operationName: 'Read'}) }
        if (el.create) { operations.push({operationName: 'Create'}) }
        if (el.update) { operations.push({operationName: 'Update'}) }
        if (el.del)    { operations.push({operationName: 'Delete'}) }
        if (el.expImp) { operations.push({operationName: 'Export Import'}) }
        if (el.lock)   { operations.push({operationName: 'Lock'}) }
        if (el.unlock) { operations.push({operationName: 'Unlock'}) }

        if (seenResources.indexOf(el.name) != -1) {
          alertSvc.error({code: 'roles.multiple_resource_acls', args: {resource: i18n.msg('roles.resources.' + el.name)}});
          return;
        }

        payload.acl.push({ id: el.id, resourceName: el.name, operations });
        seenResources.push(el.name);
      } 

      const savedRole = await roleSvc.saveOrUpdate(payload);
      alertSvc.success({code: 'roles.saved', args: savedRole});
      routerSvc.goto('UserRolesListItemDetail.Overview', {roleId: savedRole.id});
    },

    cancel: function() {
      routerSvc.back();
    },

    addResource: function() {
      this.dataCtx.role.acl.push({});
    },

    removeResource: function(elIdx) {
      this.dataCtx.role.acl.splice(elIdx, 1);
    },

    readToggled: function(el) {
      if (el.create) {
        el.read = true;
      }
    },

    createToggled: function(el) {
      if (el.create) {
        el.read = el.update = true;
      }
    },

    updateToggled: function(el) {
      if (el.create) {
        el.update = true;
      }
    },

    _showLockUnlock: function(role) {
      for (let el of role.acl || []) {
        if (el.name == 'SurgicalPathologyReport' || (this.$osSvc.ecDocSvc && el.name == 'Consent')) {
          return true;
        }
      }

      return false;
    }
  }
}
</script>

<style scoped>
.roles-matrix table {
  margin-bottom: 1.25rem;
}

.roles-matrix .os-table thead th {
  padding: 0.5rem 0.75rem;
  color: #707070;
  background: #efefef;
  border-bottom: 1px solid #dee2e6; 
  font-weight: bold;
}

.roles-matrix .os-table thead th:first-child {
  min-width: 250px;
  text-align: center;
}

.roles-matrix .os-table thead th:not(first-child) {
  width: 100px;
  text-align: center;
}

.roles-matrix .os-table tbody td {
  border-top: 0px;
  vertical-align: middle;
}

.roles-matrix :deep(.p-field-checkbox) {
  margin-bottom: 0rem;
  justify-content: center;
}
</style>
