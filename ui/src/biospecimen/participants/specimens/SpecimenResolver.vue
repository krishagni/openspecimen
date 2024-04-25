<template>
  <span>resolving specimen view...</span>
</template>

<script>
import http      from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['specimenId'],

  created() {
    const query = {objectName: 'specimen', key: 'id', value: this.specimenId};
    http.get('object-state-params', query).then(
      ({cpId, cprId, visitId, specimenId}) => {
        routerSvc.goto('ParticipantsListItemSpecimenDetail.Overview', {cpId, cprId, visitId, specimenId});
      }
    );
  }
}
</script>
