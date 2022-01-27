
<template>
  <Section>
    <template #title>
      <span>Audit Trail</span>
    </template>

    <template #content>
      <ul class="os-key-values os-one-col" v-if="ctx.auditTrail.createdBy || ctx.auditTrail.lastUpdatedBy">
        <li class="item" v-if="ctx.auditTrail.createdBy">
          <strong class="key key-sm">Entered By</strong>
          <span class="value value-md">{{$filters.username(ctx.auditTrail.createdBy)}}</span>
        </li>
        <li class="item" v-if="ctx.auditTrail.createdOn">
          <strong class="key key-sm">Entered On</strong>
          <span class="value value-md">{{$filters.dateTime(ctx.auditTrail.createdOn)}}</span>
        </li>
        <li class="item" v-if="ctx.auditTrail.lastUpdatedBy">
          <strong class="key key-sm">Updated By</strong>
          <span class="value value-md">{{$filters.username(ctx.auditTrail.lastUpdatedBy)}}</span>
        </li>
        <li class="item" v-if="ctx.auditTrail.lastUpdatedOn">
          <strong class="key key-sm">Updated On</strong>
          <span class="value value-md">{{$filters.dateTime(ctx.auditTrail.lastUpdatedOn)}}</span>
        </li>
        <li class="item">
          <strong class="key key-sm">Number of Revisions</strong>
          <span class="value value-md">
            <span>{{ctx.auditTrail.revisionsCount}}</span>
            <a @click="showRevs" v-if="ctx.auditTrail.revisionsCount > 0">
              <span>(View All)</span>
            </a>
          </span>
        </li>
      </ul>
      <span v-else>
        <span>No trail to show!</span>
      </span>
    </template>
  </Section>

  <Dialog ref="revsDialog">
    <template #header>
      <span>Revisions</span>
    </template>

    <template #content>
      <table class="os-table" v-if="ctx.revisions && ctx.revisions.length > 0">
        <thead>
          <tr>
            <th>Updated On</th>
            <th>Updated By</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(rev, idx) of ctx.revisions" :key="idx">
            <td>{{$filters.dateTime(rev.changedOn)}}</td>
            <td>{{$filters.username(rev.changedBy)}}</td>
          </tr>
        </tbody>
      </table>
      <div v-else>
        <span>No revisions to show!</span>
      </div>
    </template>

    <template #footer>
      <Button label="Done" type="primary" @click="closeRevs" />
    </template>
  </Dialog>
</template>

<script>

import Button  from '@/common/components/Button.vue';
import Dialog  from '@/common/components/Dialog.vue';
import Section from '@/common/components/Section.vue';

import auditLogSvc from '@/common/services/AuditLogs.js';

export default {
  props: ['objects'],

  components: {
    Button,
    Dialog,
    Section
  },

  data() {
    return {
      ctx: {
        auditTrail: {}
      }
    };
  },

  created() {
    this.loadAuditTrail();
  },

  watch: {
    objects: function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.loadAuditTrail();
    }
  },

  methods: {
    loadAuditTrail: async function() {
      const ctx = this.ctx;

      ctx.auditTrail = {};
      if (!this.objects || this.objects.length <= 0) {
        return;
      }

      const infoList = await auditLogSvc.getSummary(this.objects);

      let trail = {};
      infoList.forEach(
        (info, idx) => {
          if (idx == 0) {
            trail = info;
          } else {
            if (trail.createdOn < info.createdOn) {
              trail.createdBy = info.createdBy;
              trail.createdOn = info.createdOn;
            }

            if (info.lastUpdatedOn > trail.lastUpdatedOn) {
              trail.lastUpdatedBy = info.lastUpdatedBy;
              trail.lastUpdatedOn = info.lastUpdatedOn;
            }

            trail.revisionsCount += info.revisionsCount;
          }
        }
      );

      ctx.auditTrail = trail;
    },

    showRevs: function() {
      auditLogSvc.getRevisions(this.objects).then(
        (revisions) => {
          this.ctx.revisions = revisions;
          this.$refs.revsDialog.open();
        }
      );
    },

    closeRevs: function() {
      this.$refs.revsDialog.close();
    }
  }
}
</script>
