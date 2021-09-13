<template>
  <Dialog ref="dialogInstance">
    <template #header>
      <div v-if="ctx.dependents.length > 0">
        <span>Error: Cannot delete {{input.title}}</span>
      </div>
      <div v-else>
        <span>Delete Confirmation</span>
      </div>
    </template>

    <template #content>
      <div v-if="ctx.dependents.length > 0">
        <div class="message">
          <span>Cannot delete {{input.type}} <b>{{input.title}}</b> because of the following dependent objects:</span>
        </div>

        <div class="dependents">
          <table class="os-table muted-header os-border">
            <thead>
              <tr>
                <th>Name</th>
                <th>Count</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(dependent, idx) of ctx.dependents" :key="idx">
                <td>{{dependent.name}}</td>
                <td>{{dependent.count}}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-else>
        <div class="message">
          <span>{{input.type}} <b>{{input.title}}</b> and any dependent data will be deleted. Are you sure you want to proceed?</span>
        </div>
      </div>
    </template>

    <template #footer>
      <div v-if="ctx.dependents.length > 0">
        <Button label="Ok" @click="cancel" />
      </div>
      <div v-else>
        <Button label="Cancel" @click="cancel" />

        <Button label="Yes" @click="proceed" />
      </div>
    </template>
  </Dialog>
</template>

<script>
import Dialog from '@/common/components/Dialog.vue';
import Button from '@/common/components/Button.vue';

import alertSvc from '@/common/services/Alerts.js';

export default {
  props: ['input'],

  components: {
    Dialog,
    Button
  },

  data() {
    return {
      ctx: { dependents: [] }
    }
  },

  methods: {
    execute: function() {
      let self = this;
      return new Promise((resolve) => {
        self.resolve = resolve;

        if (typeof self.input.dependents == 'function') {
          self.input.dependents().then(
            (dependents) => {
              self.ctx.dependents = dependents;
              self.$refs.dialogInstance.open();
            }
          );
        } else {
          self.cancel();
        }
      });
    },

    proceed: function() {
      let self = this;
      if (typeof this.input.deleteObj == 'function') {
        this.input.deleteObj().then(() => {
          alertSvc.success(self.input.type + ' ' + self.input.title + ' deleted!');
          self.close('deleted');
        });
      } else {
        this.cancel();
      }
    },

    close: function(resp) {
      this.resolve(resp);
      this.$refs.dialogInstance.close();
      this.resolve = null;
    },

    cancel: function() {
      this.close('cancel');
    }
  }
}
</script>

<style scoped>
.message {
  margin-bottom: 1.25rem;
  display: inline-block;
}

.dependents {
  margin-bottom: 1.25rem;
}
</style>
