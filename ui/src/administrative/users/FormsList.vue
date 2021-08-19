
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
            <span>{{ctx.selectedForm.formCaption}}</span>
          </template>

          <span v-if="!ctx.selectedRecord.recordId">
            <span v-if="!ctx.selectedForm.records || ctx.selectedForm.records.length == 0">
              <span>No records to display</span>
            </span>
            <span v-else-if="ctx.selectedForm.records.length > 0">
              <table class="os-table">
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
                      <a>#{{record.recordId}}</a>
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
                        <Button left-icon="trash" size="small" />
                      </ButtonGroup>
                    </td>
                  </tr>
                </tbody>
              </table>
            </span>
          </span>
        </Panel>
      </GridColumn>
    </Grid>
  </div>
</template>

<script>

import {reactive, watchEffect} from 'vue';
import ListGroup from '@/common/components/ListGroup.vue';
import Grid from '@/common/components/Grid.vue';
import GridColumn from '@/common/components/GridColumn.vue';
import Panel from '@/common/components/Panel.vue';
import Button from '@/common/components/Button.vue';
import ButtonGroup from '@/common/components/ButtonGroup.vue';

export default {
  props: ['forms', 'records', 'formId', 'formCtxtId'],

  components: {
    ListGroup,
    Grid,
    GridColumn,
    Panel,
    Button,
    ButtonGroup
  },

  setup(props) {
    let ctx = reactive({
      selectedRecord: {}
    });

    watchEffect(
      () => {
        if (props.formCtxtId) {
          ctx.selectedForm = props.forms.find((form) => form.formCtxtId == props.formCtxtId);
        } else if (props.forms.length > 0) {
          ctx.selectedForm = props.forms[0];
        }
      }
    );

    return { ctx };
  },

  methods: {
    onSelect: function(event) {
      this.ctx.selectedForm = event.item;
    }
  }
}

</script>
