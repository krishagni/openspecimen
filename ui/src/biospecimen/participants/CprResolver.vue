<template>
  <span>resolving registration view...</span>
</template>

<script>
import http      from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cprId'],

  created() {
    const query = {objectName: 'collection_protocol_registration', key: 'id', value: this.cprId};
    http.get('object-state-params', query).then(
      ({cpId, cprId}) => {
        routerSvc.goto('ParticipantsListItemDetail.Overview', {cpId, cprId});
      }
    );
  }
}
</script>
