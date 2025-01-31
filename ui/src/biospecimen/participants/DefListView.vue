<template>
  <div>
    <os-message type="info">
      <span v-t="'common.loading'">Loading...</span>
    </os-message>
  </div>
</template>

<script>
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';

export default {
  inject: ['cpViewCtx'],

  created() {
    const cp = this.cpViewCtx.getCp();
    cpSvc.getWorkflowProperty(cp.id, 'common', 'listView').then(
      route => {
        if (route == 'cp-specimens' || route == 'specimens_list') {
          route = 'specimens_list';
        } else {
          route = 'participants_list';
        }

        routerSvc.goto('ParticipantsList', {cpId: cp.id}, {view: route});
      }
    );
  }
}
</script>
