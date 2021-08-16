
<template>
  <Section>
    <template #title>
      <span>Audit Trail</span>
    </template>

    <template #content>
      <ul class="os-key-values os-one-col">
        <li class="item">
          <strong class="key key-sm">Entered By</strong>
          <span class="value value-md">{{$filters.username(ctx.auditTrail.createdBy)}}</span>
        </li>
        <li class="item">
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
          </span>
        </li>
      </ul>
    </template>
  </Section>
</template>

<script>

import { reactive, watch } from 'vue';

import Section from '@/common/components/Section.vue';

import auditLogSvc from '@/common/services/AuditLogs.js';

export default {
  props: ['objects'],

  components: {
    Section
  },

  setup(props) {
    let ctx = reactive({
      auditTrail: {}
    });

    watch(
      () => props.objects,
      (newVal) => {
        ctx.auditTrail = {};
        if (!newVal || newVal.length <= 0) {
          return;
        }

        auditLogSvc.getSummary(newVal).then(
          (infoList) => {
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
          }
        );
      }
    );

    return { ctx };
  }
}

</script>


