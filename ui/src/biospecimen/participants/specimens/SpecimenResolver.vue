<template>
  <span>resolving specimen view...</span>
</template>

<script>
import http      from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['specimenId', 'query'],

  created() {
    http.get('object-state-params', this._getQueryParams()).then(
      ({cpId, cprId, visitId, specimenId}) => {
        const {view, ...otherQuery} = this.query || {};
        if (view) {
          routerSvc.goto(view, {cpId, cprId, visitId, specimenId}, otherQuery);
        } else {
          routerSvc.goto('ParticipantsListItemSpecimenDetail.Overview', {cpId, cprId, visitId, specimenId});
        }
      }
    );
  },

  methods: {
    _getQueryParams: function() {
      return {objectName: 'specimen', key: 'id', value: this.specimenId};
    }
  }
}
</script>
