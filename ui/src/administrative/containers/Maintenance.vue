
<template>
  <os-tabs @tabChanged="onTabChange">
    <os-tab name="schedule">
      <template #header>
        <span>Scheduled Activities</span>
      </template>

      <div style="margin-bottom: 1.25rem;" v-if="allowEdits">
        <os-button left-icon="plus" label="Create" @click="showAddEditScheduledActivityForm({activity: {}})" />
      </div>

      <os-list-view :data="activities"
        :schema="scheduledActivitiesSchema"
        :loading="loadingScheduledActivities"
        :showRowActions="allowEdits">

        <template #rowActions="slotProps">
          <os-button-group>
            <os-button size="small" left-icon="edit" v-os-tooltip.bottom="'Edit'"
             @click="showAddEditScheduledActivityForm(slotProps.rowObject)" />

            <os-button size="small" left-icon="archive" v-os-tooltip.bottom="'Archive'"
              @click="confirmArchiveScheduledActivity(slotProps.rowObject)" />
          </os-button-group>
        </template>

      </os-list-view>
    </os-tab>

    <os-tab name="activities">
      <template #header>
        <span>Activities Log</span>
      </template>

      <div style="margin-bottom: 1.25rem;">
        <os-button left-icon="plus" label="Create" @click="showAddEditActivityLogForm({activityLog: {}})"
          v-if="allowEdits" style="margin-right: 0.25rem;" />

        <os-button left-icon="download" label="Export" @click="downloadActivityLogs()" />
      </div>

      <os-list-view :data="activityLogs"
        :schema="activityLogsSchema"
        :loading="loadingActivityLogs"
        :showRowActions="allowEdits">

        <template #rowActions="slotProps">
          <os-button-group>
            <os-button size="small" left-icon="edit" v-os-tooltip.bottom="'Edit'"
             @click="showAddEditActivityLogForm(slotProps.rowObject)" />

            <os-button size="small" left-icon="archive" v-os-tooltip.bottom="'Archive'"
              @click="confirmArchiveActivity(slotProps.rowObject)" />
          </os-button-group>
        </template>

      </os-list-view>
    </os-tab>
  </os-tabs>

  <os-dialog ref="addEditScheduledActivityDialog">
    <template #header>
      <span>Schedule an Activity</span>
    </template>
    <template #content>
      <os-form ref="scheduledActivityForm" :schema="scheduledActivitySchema.layout" :data="{activity: activity}" />
    </template>
    <template #footer>
      <os-button text label="Cancel" @click="hideAddEditScheduledActivityForm" />

      <os-button primary :label="activity.id ? 'Update' : 'Create'" @click="addEditScheduledActivity" />
    </template>
  </os-dialog>

  <os-confirm ref="confirmArchiveScheduledActivityDialog">
    <template #title>
      <span>Archive Scheduled Activity</span>
    </template>
    <template #message>
      <span>Are you sure you want to archive the scheduled activity: <b>{{toArchive.name}}</b>?</span>
    </template>
  </os-confirm>

  <os-dialog ref="addEditActivityLogDialog">
    <template #header>
      <span v-if="activityLog.id">Update Activity Log</span>
      <span v-else>Log an Activity</span>
    </template>
    <template #content>
      <os-form ref="activityLogForm" :schema="activityLogSchema.layout" :data="{activityLog: activityLog}"
        @input="handleActivityLogInput($event)" />
    </template>
    <template #footer>
      <os-button text label="Cancel" @click="hideAddEditActivityLogForm" />

      <os-button primary :label="activityLog.id ? 'Update' : 'Create'" @click="addEditActivityLog" />
    </template>
  </os-dialog>

  <os-confirm ref="confirmArchiveActivityDialog">
    <template #title>
      <span>Archive Activity</span>
    </template>
    <template #message>
      <span>Are you sure you want to archive the activity: <b>{{toArchiveLog.scheduledActivityName || toArchiveLog.taskName}}</b>?</span>
    </template>
  </os-confirm>
</template>

<script>

import containerSvc from '@/administrative/services/Container.js';
import alertsSvc    from '@/common/services/Alerts.js';
import authSvc      from '@/common/services/Authorization.js';
import exportSvc    from '@/common/services/ExportService.js';
import routerSvc    from '@/common/services/Router.js';
import util         from '@/common/services/Util.js';

import scheduledActivitiesSchema from '@/administrative/schemas/containers/scheduled-activities.js';
import scheduledActivitySchema from '@/administrative/schemas/containers/scheduled-activity.js';
import activityLogsSchema from '@/administrative/schemas/containers/activity-logs.js';
import activityLogSchema from '@/administrative/schemas/containers/activity-log.js';

export default {
  props: ['container'],

  data() {
    return {
      allowEdits: false,

      activities: [],

      scheduledActivitiesSchema,

      loadingScheduledActivities: false,

      toArchive: {},

      scheduledActivitySchema,

      activity: {},

      activityLogs: null,

      activityLogsSchema,

      loadingActivityLogs: false,

      toArchiveLog: {},

      activityLogSchema,

      activityLog: {}
    }
  },

  created() {
    this.loadScheduledActivities();
    this.allowEdits = authSvc.isAllowed({
      resource: 'StorageContainer',
      operations: ['Update'],
      sites: [this.container.siteName]
    });
  },

  watch: {
    'container.id': function(newVal, oldVal) {
      if (newVal != oldVal && this.container.storageLocation && this.container.storageLocation.id) {
        routerSvc.goto('ContainerDetail.Overview', {containerId: newVal});
      }
    }
  },

  methods: {
    onTabChange: function({activeTab}) {
      if (activeTab == 1 && !this.activityLogs) {
        this.loadActivities();
      }
    },

    loadScheduledActivities: function() {
      this.loadingScheduledActivities = true;
      containerSvc.getScheduledActivities(this.container).then(
        activities => {
          this.loadingScheduledActivities = false;
          this.activities = activities.map(activity => ({ activity }));
        }
      );
    },

    showAddEditScheduledActivityForm: function({activity}) {
      if (activity.id) {
        this.activity = util.clone(activity);
      } else {
        this.activity = {containerId: this.container.id};
      }

      this.$refs.addEditScheduledActivityDialog.open();
    },

    hideAddEditScheduledActivityForm: function() {
      this.$refs.addEditScheduledActivityDialog.close();
    },

    addEditScheduledActivity: function() {
      if (!this.$refs.scheduledActivityForm.validate()) {
        return;
      }

      const created = !this.activity.id;
      containerSvc.saveOrUpdateScheduledActivity(this.activity).then(
        (savedActivity) => {
          if (created) {
            alertsSvc.success('Scheduled activity ' + savedActivity.name + ' created');
          } else {
            alertsSvc.success('Scheduled activity ' + savedActivity.name + ' updated');
          }

          this.loadScheduledActivities();
          this.hideAddEditScheduledActivityForm();
        }
      );
    },

    confirmArchiveScheduledActivity: function({activity}) {
      this.toArchive = activity;
      this.$refs.confirmArchiveScheduledActivityDialog.open().then(
        (resp) => {
          if (resp == 'proceed') {
            containerSvc.archiveScheduledActivity(activity).then(
              () => {
                alertsSvc.success('Scheduled activity ' + activity.name + ' archived!');
                this.loadScheduledActivities();
              }
            );
          }
        }
      );
    },

    loadActivities: function() {
      this.loadingActivityLogs = true;
      containerSvc.getActivityLogs(this.container).then(
        activityLogs => {
          this.loadingActivityLogs = false;
          this.activityLogs = activityLogs.map(activityLog => ({ activityLog }));
        }
      );
    },

    showAddEditActivityLogForm: function({activityLog}) {
      if (activityLog.id) {
        this.activityLog = util.clone(activityLog);
        this.activityLog.activity = activityLog.scheduledActivityName || activityLog.taskName;
      } else {
        this.activityLog = {
          containerId: this.container.id,
          type: 'adhoc',
          activityDate: Date.now(),
          performedBy: this.$ui.currentUser
        };
      }

      this.$refs.addEditActivityLogDialog.open();
    },

    hideAddEditActivityLogForm: function() {
      this.$refs.addEditActivityLogDialog.close();
    },

    handleActivityLogInput: function({field, value}) {
      if (field.name != 'activityLog.type') {
        return;
      }

      if (value == 'adhoc') {
        this.activityLog.scheduledActivityName = null;
      } else {
        this.activityLog.taskName = null;
      }
    },

    addEditActivityLog: function() {
      if (!this.$refs.activityLogForm.validate()) {
        return;
      }

      const created = !this.activityLog.id;
      containerSvc.saveOrUpdateActivityLog(this.activityLog).then(
        (savedActivity) => {
          const name = (savedActivity.scheduledActivityName || savedActivity.taskName);
          alertsSvc.success('Activity ' + name + (created ? ' created' : ' updated'));
          this.loadActivities();
          this.hideAddEditActivityLogForm();
        }
      );
    },

    confirmArchiveActivity: function({activityLog}) {
      this.toArchiveLog = activityLog;
      this.$refs.confirmArchiveActivityDialog.open().then(
        (resp) => {
          if (resp == 'proceed') {
            containerSvc.archiveActivityLog(activityLog).then(
              () => {
                const name = (activityLog.scheduledActivityName || activityLog.taskName);
                alertsSvc.success('Activity ' + name + ' archived!');
                this.loadActivities();
              }
            );
          }
        }
      );
    },

    downloadActivityLogs: function() {
      exportSvc.exportRecords({objectType: 'containerActivityLog', params: {containerId: this.container.id}});
    }
  }
}
</script>
