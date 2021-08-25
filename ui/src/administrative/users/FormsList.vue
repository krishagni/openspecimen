<template>
  <div>
    <Grid>
      <GridColumn width="3">
        <ListGroup :list="forms" :selected="ctx.selectedForm" @on-item-select="onSelect($event)">
          <template #header>
            <span>Forms</span>
          </template>
          <template #default="{item}">
            <div class="heading">
              <strong>{{item.formCaption}}</strong>
            </div>
            <div class="content" v-if="item.records">
              <span v-if="item.records.length == 1">
                <div>{{$filters.username(item.records[0].user)}}</div>
                <div>{{$filters.dateTime(item.records[0].updateTime)}}</div>
              </span>
              <span v-else-if="item.records.length > 1">
                <div>{{item.records.length}} records</div>
              </span>
            </div>
          </template>
        </ListGroup>
      </GridColumn>
      <GridColumn width="9">
        <Panel>
          <template #header>
            <span>
              <span class="title">{{ctx.selectedForm.formCaption}}</span>
              <span v-if="!ctx.selectedRecord || !ctx.selectedRecord.recordId">
                <Button left-icon="plus" label="Add" @click="addRecord" />
              </span>
              <span v-else>
                <Button left-icon="edit" label="Edit" @click="editRecord(ctx.selectedRecord)" />

                <Button left-icon="trash" label="Delete" @click="deleteRecord(ctx.selectedRecord)" />
              </span>
            </span>
          </template>

          <span v-if="!ctx.selectedRecord || !ctx.selectedRecord.recordId">
            <span v-if="!ctx.selectedForm.records || ctx.selectedForm.records.length == 0">
              <span>No records to display</span>
            </span>
            <span v-else-if="ctx.selectedForm.records.length > 0">
              <table class="os-table os-table-hover">
                <thead>
                  <tr>
                    <th>Record</th>
                    <th>Updated By</th>
                    <th>Update Time</th>
                    <th>&nbsp;</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="record of ctx.selectedForm.records" :key="record.recordId">
                    <td>
                      <router-link :to="{
                        name: 'UserFormsList',
                        query: {formId: record.formId, formCtxtId: record.fcId, recordId: record.recordId}
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
                      <ButtonGroup>
                        <Button left-icon="edit" size="small" />
                        <Button left-icon="trash" size="small" @click="deleteRecord(record)" />
                      </ButtonGroup>
                    </td>
                  </tr>
                </tbody>
              </table>
            </span>
          </span>
          <span v-else>
            <pre v-if="!ctx.selectedRecord">{{ctx.selectedRecord}}</pre>
            <FormRecordOverview v-else :record="ctx.record" />
          </span>
        </Panel>
      </GridColumn>
    </Grid>

    <DeleteFormRecord ref="deleteFormDialog" />
  </div>
</template>

<script>

import {reactive, watchEffect} from 'vue';
import { useRouter } from 'vue-router'

import ListGroup from '@/common/components/ListGroup.vue';
import Grid from '@/common/components/Grid.vue';
import GridColumn from '@/common/components/GridColumn.vue';
import Panel from '@/common/components/Panel.vue';
import Button from '@/common/components/Button.vue';
import ButtonGroup from '@/common/components/ButtonGroup.vue';

import FormRecordOverview from '@/forms/components/FormRecordOverview.vue';
import DeleteFormRecord from '@/forms/components/DeleteFormRecord.vue';

import formSvc from '@/forms/services/Form.js';

export default {
  props: ['objectId', 'forms', 'records', 'formId', 'formCtxtId', 'recordId'],

  components: {
    ListGroup,
    Grid,
    GridColumn,
    Panel,
    Button,
    ButtonGroup,
    FormRecordOverview,
    DeleteFormRecord
  },

  setup(props) {
    let ctx = reactive({
      selectedRecord: {}
    });

    let router = useRouter();
    watchEffect(
      () => {
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
              name: 'UserFormsList',
              query: {
                formId: ctx.selectedForm.formId,
                formCtxtId: ctx.selectedForm.formCtxtId,
                recordId: ctx.selectedForm.records[0].recordId
              }
            });
          }
        }
      }
    );

    return { ctx };
  },

  methods: {
    onSelect: function(event, replace) {
      let form = event.item;
      let method = replace ? this.$router.replace : this.$router.push;
      method({
        name: 'UserFormsList',
        query: {
          formId: form.formId,
          formCtxtId: form.formCtxtId,
          recordId: (form.records && form.records.length == 1) ? form.records[0].recordId : undefined
        }
      });
    },

    showRecord: function(record) {
      this.$router.push({
        name: 'UserFormsList',
        query: { formId: record.formId, formCtxtId: record.fcId, recordId: record.recordId }
      });
    },

    addRecord: function() {
      let selectedForm = this.ctx.selectedForm;
      this.$router.push({
        name: 'UserFormAddEdit',
        query: { formId: selectedForm.formId, formCtxtId: selectedForm.formCtxtId }
      });
    },

    editRecord: function(record) {
      let selectedForm = this.ctx.selectedForm;
      this.$router.push({
        name: 'UserFormAddEdit',
        query: {
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
          self.onSelect({item: form}, true);
        }
      );
    }
  }
}

</script>
