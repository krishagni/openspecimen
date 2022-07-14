<template>
  <div>
    <os-grid>
      <os-grid-column width="3">
        <os-list-group :list="forms" :selected="ctx.selectedForm" @on-item-select="onFormSelect($event)">
          <template #header>
            <span v-t="'users.forms'">Forms</span>
          </template>
          <template #default="{item}">
            <div class="heading">
              <strong class="title">{{item.formCaption}}</strong>
              <span class="status" :class="item.records && item.records.length > 0 && 'completed'">
                <os-icon name="circle" />
              </span>
            </div>
            <div class="content" v-if="item.records && item.records.length > 0">
              <span v-if="item.records.length == 1">
                <div>{{$filters.username(item.records[0].user)}}</div>
                <div>{{$filters.dateTime(item.records[0].updateTime)}}</div>
              </span>
              <span v-else-if="item.records.length > 1">
                <div>{{item.records.length}} records</div>
              </span>
            </div>
          </template>
        </os-list-group>
      </os-grid-column>
      <os-grid-column width="9">
        <os-panel>
          <template #header>
            <span>
              <span class="title">{{ctx.selectedForm.formCaption}}</span>
              <span v-if="entity.isActive && entity.isUpdateAllowed">
                <span v-if="!ctx.selectedRecord || !ctx.selectedRecord.recordId">
                  <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="addRecord" />
                </span>
                <span v-else>
                  <os-button primary left-icon="edit"
                    :label="$t('common.buttons.edit')" @click="editRecord(ctx.selectedRecord)" />

                  <os-button danger left-icon="trash"
                    :label="$t('common.buttons.delete')" @click="deleteRecord(ctx.selectedRecord)" />

                  <os-button v-if="ctx.selectedForm.multiRecord" left-icon="plus"
                    :label="$t('common.buttons.add_another')" @click="addRecord" />
                </span>
              </span>
            </span>
          </template>

          <span v-if="!ctx.selectedRecord || !ctx.selectedRecord.recordId">
            <span v-if="!ctx.selectedForm.records || ctx.selectedForm.records.length == 0">
              <span v-t="'users.no_form_records'">No records to display</span>
            </span>
            <span v-else-if="ctx.selectedForm.records.length > 0">
              <table class="os-table os-table-hover">
                <thead>
                  <tr>
                    <th v-t="'users.record'">Record</th>
                    <th v-t="'common.updated_by'">Updated By</th>
                    <th v-t="'common.update_time'">Update Time</th>
                    <th>&nbsp;</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="record of ctx.selectedForm.records" :key="record.recordId">
                    <td>
                      <router-link :to="{
                        name: listView,
                        query: {...routeQuery, formId: record.formId, formCtxtId: record.fcId, recordId: record.recordId}
                      }">
                        <span>#{{record.recordId}}</span>
                      </router-link>
                    </td>
                    <td>
                      <span>{{$filters.username(record.user)}}</span>
                    </td>
                    <td>
                      <span>{{$filters.dateTime(record.updateTime)}}</span>
                    </td>
                    <td>
                      <os-button-group v-if="entity.isActive && entity.isUpdateAllowed">
                        <os-button left-icon="edit"  size="small" @click="editRecord(record)" />
                        <os-button left-icon="trash" size="small" @click="deleteRecord(record)" />
                      </os-button-group>
                    </td>
                  </tr>
                </tbody>
              </table>
            </span>
          </span>
          <span v-else>
            <FormRecordOverview :record="ctx.record" />
          </span>
        </os-panel>
      </os-grid-column>
    </os-grid>

    <DeleteFormRecord ref="deleteFormDialog" />
  </div>
</template>

<script>

import { reactive, watch } from 'vue';
import { useRouter } from 'vue-router'

import FormRecordOverview from '@/forms/components/FormRecordOverview.vue';
import DeleteFormRecord from '@/forms/components/DeleteFormRecord.vue';

import formSvc from '@/forms/services/Form.js';

export default {
  props: ['entity', 'forms', 'formId', 'formCtxtId', 'recordId', 'listView', 'addEditView', 'routeQuery'],

  components: {
    FormRecordOverview,
    DeleteFormRecord
  },

  setup(props) {
    const ctx = reactive({
      selectedRecord: {}
    });

    const router = useRouter();
    const loadRecord = () => {
      if (props.formCtxtId) {
        ctx.selectedForm = props.forms.find((form) => form.formCtxtId == props.formCtxtId);
        if (props.recordId) {
          ctx.selectedRecord = ctx.selectedForm.records.find((record) => record.recordId == props.recordId);
          if (!ctx.selectedRecord) {
            ctx.selectedRecord = {};
          }
        } else {
          ctx.selectedRecord = {};
        }

        if (ctx.selectedRecord.recordId) {
          formSvc.getRecord(ctx.selectedRecord, {includeMetadata: true}).then((record) => ctx.record = record);
        }
      } else if (props.forms.length > 0) {
        ctx.selectedForm = props.forms[0];
        if (ctx.selectedForm.records && ctx.selectedForm.records.length == 1) {
          router.push({
            name: props.listView,
            query: {
              ...props.routeQuery,
              formId: ctx.selectedForm.formId,
              formCtxtId: ctx.selectedForm.formCtxtId,
              recordId: ctx.selectedForm.records[0].recordId
            }
          });
        }
      }
    }

    loadRecord();
    watch(
      () => [props.forms, props.formCtxtId, props.recordId, props.listView, props.routeQuery],
      (newVal, oldVal) => {
        let load = false;
        for (let idx = 0; idx < newVal.length; ++idx) {
          if (newVal[idx] != oldVal[idx]) {
            load = true;
            break;
          }
        }

        if (load) {
          loadRecord();
        }
      }
    );

    return { ctx };
  },

  methods: {
    onFormSelect: function(event, replace) {
      let form = event.item;
      let method = replace ? this.$router.replace : this.$router.push;
      method({
        name: this.listView,
        query: {
          ...this.routeQuery,
          formId: form.formId,
          formCtxtId: form.formCtxtId,
          recordId: (form.records && form.records.length == 1) ? form.records[0].recordId : undefined
        }
      });
    },

    showRecord: function(record) {
      this.$router.push({
        name: this.listView,
        query: { ...this.routeQuery, formId: record.formId, formCtxtId: record.fcId, recordId: record.recordId }
      });
    },

    addRecord: function() {
      let selectedForm = this.ctx.selectedForm;
      this.$router.push({
        name: this.addEditView,
        query: { ...this.routeQuery, formId: selectedForm.formId, formCtxtId: selectedForm.formCtxtId }
      });
    },

    editRecord: function(record) {
      let selectedForm = this.ctx.selectedForm;
      this.$router.push({
        name: this.addEditView,
        query: {
          ...this.routeQuery,
          formId: selectedForm.formId,
          formCtxtId: selectedForm.formCtxtId,
          recordId: record.recordId || record.id
        }
      });
    },

    deleteRecord: function(record) {
      let self = this;
      this.$refs.deleteFormDialog.execute(record).then(
        () => {
          let form = self.ctx.selectedForm;
          let idx = form.records.indexOf(record);
          form.records.splice(idx, 1);
          self.onFormSelect({item: form}, true);
        }
      );
    }
  }
}
</script>

<style scoped>

.heading {
  display: flex;
}

.heading .title {
  flex-grow: 1;
}

.heading .status {
  display: inline-block;
  color: #f0ad4e;
  margin-left: 0.25rem;
}

.heading .status.completed {
  color: #5cb85c;
}

</style>
