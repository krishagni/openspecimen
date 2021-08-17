
<template>
  <div>
    <Panel>
      <template #header>
        <span>Roles</span>
      </template>

      <template #actions>
        <Button icon="plus" label="Add Role" />
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
  </div>
</template>

<script>

import Button from '@/common/components/Button.vue';
import Panel from '@/common/components/Panel.vue';
import Message from '@/common/components/Message.vue';

import userSvc from '@/administrative/services/User.js';

export default {
  props: ['user'],

  components: {
    Button,
    Panel,
    Message
  },

  data() {
    return {
      ctx: {}
    };
  },

  created() {
    userSvc.getRoles(this.user).then((roles) => this.ctx.userRoles = roles);
  },

  methods: {
    deleteRole: function(userRole) {
      let idx = this.ctx.userRoles.indexOf(userRole);
      userSvc.deleteRole(this.user, userRole).then(() => this.ctx.userRoles.splice(idx, 1));
    }
  }
}

</script>
