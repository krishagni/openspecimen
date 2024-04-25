<template>
  <span>resolving visit view...</span>
</template>

<script>
import http      from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['visitId'],

  created() {
    const query = {objectName: 'visit', key: 'id', value: this.visitId};
    http.get('object-state-params', query).then(
      ({cpId, cprId, visitId}) => {
        routerSvc.goto('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId});
      }
    );
  }
}
</script>
