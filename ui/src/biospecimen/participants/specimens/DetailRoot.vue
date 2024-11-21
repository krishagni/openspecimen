
<template>
  <router-view :cpr="cpr" :visit="visit" :specimen="specimen" :key="viewKey" v-if="viewKey && loaded" />
</template>

<script>

import {provide, ref} from 'vue';

import cpSvc    from '@/biospecimen/services/CollectionProtocol.js';
import formUtil from '@/common/services/FormUtil.js';
import spmnSvc  from '@/biospecimen/services/Specimen.js';

export default {
  props: ['cpr', 'visit', 'specimenId', 'reqId'],

  async setup() {
    const specimen = ref({});
    provide('specimen', specimen);

    const loaded = ref(false);
    return { specimen, loaded };
  },

  created() {
    if (this.specimenId > 0) {
      this._loadSpecimen(this.specimenId);
    } else if (this.reqId > 0) {
      this._loadRequirement(this.reqId);
    }
  },

  computed: {
    viewKey: function() {
      if (this.specimenId > 0) {
        return 's-' + this.specimenId;
      } else if (this.reqId > 0) {
        return 'sr-' + this.cpr.cpId + '-' + this.visit.eventId + '-' + this.reqId;
      }

      return 'unknown';
    }
  },

  watch: {
    '$route.params.specimenId': function(newVal, oldVal) {
      if (newVal == oldVal || newVal == this.specimen.id) {
        return;
      }

      if (newVal > 0) {
        this._loadSpecimen(newVal);
      }
    },

    '$route.query.reqId': function(newVal, oldVal) {
      if (this.specimenId > 0 || newVal == oldVal || newVal == this.specimen.reqId) {
        return;
      }

      this._loadRequirement(newVal);
    }
  },

  methods: {
    _loadSpecimen: async function(specimenId) {
      this.specimen = {};
      this.specimen = await spmnSvc.getById(specimenId);
      formUtil.createCustomFieldsMap(this.specimen, true);
      this.loaded = true;
    },

    _loadRequirement: async function(reqId) {
      this.specimen = {};
      const req = await cpSvc.getSpecimenRequirement(reqId);
      this.specimen = this._toSpecimen(req);
      this.loaded = true;
    },

    _toSpecimen: function(req) {
      const specimen = {};
      specimen.reqId = req.id;
      specimen.cpId = this.cpr.cpId;
      specimen.cprId = this.cpr.id;
      specimen.ppid = this.cpr.ppid;
      specimen.eventId = this.visit.eventId;
      specimen.eventCode = this.visit.eventCode;
      specimen.eventLabel = this.visit.eventLabel;
      specimen.visitId = this.visit.id;
      specimen.visitName = this.visit.name;
      specimen.visitStatus = this.visit.status;
      specimen.sprNo = this.visit.surgicalPathologyNumber;
      specimen.visitDate = this.visit.visitDate;
      specimen.cpShortTitle = this.cpr.cpShortTitle;
      specimen.reqId = req.id;
      specimen.sortOrder = req.sortOrder;
      specimen.type = req.type;
      specimen.specimenClass = req.specimenClass;
      specimen.lineage = req.lineage;
      specimen.anatomicSite = req.anatomicSite;
      specimen.laterality = req.laterality;
      specimen.status = 'Pending';
      specimen.reqLabel = req.name;
      specimen.pathology = req.pathology;
      specimen.initialQty = req.initialQty;
      specimen.concentration = req.concentration;
      specimen.collectionContainer = req.collectionContainer;
      specimen.extensionDetail = req.extensionDetail;
      if (specimen.extensionDetail) {
        formUtil.createCustomFieldsMap(specimen, true);
      }

      delete req.id;

      specimen.children = [];
      for (let child of (req.children || [])) {
        specimen.children.push(this._toSpecimen(child));
      }

      return specimen;
    }
  }
}
</script>
