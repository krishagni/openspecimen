<template>
  <os-dialog ref="dialogInstance">
    <template #header>
      <div v-if="ctx.dependents.length > 0 && !input.forceDelete">
        <span v-t="{path: 'common.delete_na', args: input}"></span>
      </div>
      <div v-else>
        <span v-t="'common.delete_confirmation'">Delete Confirmation</span>
      </div>
    </template>

    <template #content>
      <div v-if="ctx.dependents.length > 0">
        <div class="message" v-if="!input.forceDelete">
          <span v-t="{path: 'common.delete_na_reason', args: input}"></span>
        </div>
        <div class="message" v-else>
          <span v-t="{path: 'common.confirm_delete', args: input}">{{input.type}} <b>{{input.title}}</b> and any dependent data will be deleted. Are you sure you want to proceed?</span>
        </div>

        <div class="dependents">
          <table class="os-table muted-header os-border">
            <thead>
              <tr>
                <th v-t="'common.name'">Name</th>
                <th v-t="'common.count'">Count</th>
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
          <span v-t="{path: 'common.confirm_delete', args: input}">{{input.type}} <b>{{input.title}}</b> and any dependent data will be deleted. Are you sure you want to proceed?</span>
        </div>
      </div>

      <div v-if="input.askReason && (input.forceDelete || !ctx.dependents || ctx.dependents.length <= 0)">
        <div class="message">
          <span v-t="'common.specify_delete_reason'">Specify the reason for deletion</span>
        </div>

        <os-textarea v-model="ctx.reason"></os-textarea>
      </div>
    </template>

    <template #footer>
      <div v-if="ctx.dependents.length > 0 && !input.forceDelete">
        <os-button primary :label="$t('common.buttons.ok')" @click="cancel" />
      </div>
      <div v-else>
        <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />

        <os-button danger :label="$t('common.buttons.yes')" @click="proceed"
          :disabled="input.askReason && (!ctx.reason || ctx.reason.length <= 10)" />
      </div>
    </template>
  </os-dialog>
</template>

<script>
import alertSvc from '@/common/services/Alerts.js';

export default {
  props: ['input'],

  data() {
    return {
      ctx: { dependents: [], reason: '' }
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
        this.input.deleteObj(this.ctx.reason).then(
          (resp) => {
            if (resp && typeof resp == 'object' && Object.prototype.hasOwnProperty.call(resp, 'completed')) {
              if (!resp.completed) {
                self.close('in_progress');
                return;
              }
            }

            alertSvc.success({code: 'common.record_deleted', args: self.input});
            self.close('deleted');
          }
        );
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
