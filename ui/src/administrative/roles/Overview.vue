<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="'admin'">
        <os-button left-icon="edit" :label="$t('common.buttons.edit')"
          @click="$goto('UserRoleAddEdit', {roleId: ctx.role.id})" />
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <div>
        <ul class="os-key-values os-one-col">
          <li class="item">
            <strong class="key key-sm">
              <span v-t="'roles.name'">Name</span>
            </strong>
            <span class="value value-md">
              <span>{{ctx.role.name}}</span>
            </span>
          </li>
          <li class="item">
            <strong class="key key-sm">
              <span v-t="'roles.description'">Description</span>
            </strong>
            <span class="value value-md">
              <span>{{ctx.role.description}}</span>
            </span>
          </li>
        </ul>
      </div>

      <os-section>
        <template #title>
          <span v-t="'roles.permissions'">Privileges</span>
        </template>
        <template #content>
          <table class="os-table muted-header">
            <thead>
              <tr>
                <th>
                  <span v-t="'roles.resource'">Resource</span>
                </th>
                <th>
                  <span v-t="'roles.permissions'">Permissions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="right in acl" :key="right.id">
                <td>
                  <span>{{right.resource}}</span>
                </td>
                <td>
                  <span class="role-permissions">
                    <span class="permission" v-for="op in right.operations" :key="op.idx">
                      <os-icon class="icon" :name="op.icon" />
                      <span class="name">{{op.name}}</span>
                    </span>
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </template>
      </os-section>
    </os-grid-column>
  </os-grid>
</template>

<script>
export default {
  props: ['role'],

  data() {
    return {
      ctx: {
        role: {},

        dict: [],

        routeQuery: this.$route.query
      }
    };
  },

  async created() {
    this._setupRole();
  },

  watch: {
    role: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupRole();
      }
    }
  },

  computed: {
    acl: function() {
      const order = ['Read', 'Create', 'Update', 'Delete', 'Export Import', 'Lock', 'Unlock'];
      const icons = ['eye', 'plus', 'edit', 'trash', 'upload', 'lock', 'unlock'];
      return this.ctx.role.acl.map(
        ({id, resourceName, operations}) => {
          operations = operations.map(
            ({operationName}) => {
              const idx = order.indexOf(operationName);
              return {order: idx, name: this.$t('roles.ops.' + operationName), icon: icons[idx]};
            }
          ).sort(({order: e1}, {order: e2}) => e1 - e2);

          return { id, resource: this.$t('roles.resources.' + resourceName), operations };
        }
      ).sort(({resource: e1}, {resource: e2}) => e1 < e2 ? -1 : (e1 > e2 ? 1 : 0));
    }
  },

  methods: {
    _setupRole: function() {
      this.ctx.role = this.role;
    }
  }
}
</script>

<style scoped>
.role-permissions {
  display: flex;
  flex-direction: row;
}

.role-permissions .permission {
  padding: 0.25rem 0.5rem;
  background: #ab9364;
  color: #fff;
  border-radius: 1.125rem;
  margin-right: 0.5rem;
}

.role-permissions .permission .icon {
  padding: 0rem 0.125rem 0rem 0.25rem;
}

.role-permissions .permission .name {
  padding: 0rem 0.25rem 0rem 0.125rem;
}
</style>
