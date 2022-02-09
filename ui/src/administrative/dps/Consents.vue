<template>
  <div v-if="!ctx.hasEc">
    <os-page-toolbar>
      <template #default>
        <os-button left-icon="plus" label="Add" @click="showAddConsent" />
      </template>
    </os-page-toolbar>

    <div v-if="ctx.loading">
      <os-message type="info">
        <span>Loading consents. Please wait for a moment...</span>
      </os-message>
    </div>

    <div v-else-if="!ctx.consents || ctx.consents.length == 0">
      <os-message type="info">
        <span>No consents to display</span>
      </os-message>
    </div>
    
    <div class="os-card-items" v-else>
      <div class="item" v-for="consent of ctx.consents" :key="consent.id">
        <span class="left">{{consent.statement}} ({{consent.statementCode}})</span>
        <span class="right">
          <os-button left-icon="trash" size="small" v-os-tooltip.bottom="'Delete'"
            @click="confirmDeleteConsent(consent)" />
        </span>
      </div>
    </div>

    <os-dialog ref="addConsentDialog">
      <template #header>
        <span>Add Consent</span>
      </template>
      <template #content>
        <os-label>
          <span>Select the consent to add:</span>
        </os-label>
        <os-dropdown v-model="ctx.toAdd" :context="ctx" :listSource="consentsDdLs" />
      </template>
      <template #footer>
        <os-button text label="Cancel" @click="hideAddConsent" />
        <os-button primary label="Add" @click="addConsent" />
      </template>
    </os-dialog>

    <os-dialog ref="deleteConsentDialog">
      <template #header> 
        <span>Delete confirmation</span>
      </template>
      <template #content>
        <span>
          Are you sure you want to delete the consent - <b>{{ctx.toDelete.statement}} ({{ctx.toDelete.statementCode}})</b>?
        </span>
      </template>
      <template #footer>
        <os-button text label="Cancel" @click="cancelDeleteConsent" />
        <os-button danger label="Yes, remove" @click="deleteConsent" />
      </template>
    </os-dialog>
  </div>

  <div v-else>
    <ConsentValidationRules :dp="dp" />
  </div>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import dpSvc from '@/administrative/services/DistributionProtocol.js';

import ConsentValidationRules from './ConsentValidationRules.vue';

export default {
  props: ['dp'],

  components: {
    ConsentValidationRules
  },

  data() {
    return {
      ctx: {
        loading: false,

        consents: [],

        hasEc: this.$ui.global.appProps.plugins.indexOf('econsents') != -1
      },

      consentsDdLs: {
        loadFn: ({query}) => this.loadStatements(query),
        displayProp: 'caption'
      }
    }
  },

  created() {
    if (!this.ctx.hasEc) {
      this.loadConsents();
    }
  },

  methods: {
    loadConsents: async function() {
      this.ctx.loading = true;
      this.ctx.consents = await dpSvc.getConsents(this.dp);
      this.ctx.loading = false;
    },

    showAddConsent: function() { 
      this.ctx.toAdd = null;
      this.$refs.addConsentDialog.open();
    },

    hideAddConsent: function() { 
      this.$refs.addConsentDialog.close();
    },

    addConsent: async function() {
      if (!this.ctx.toAdd || !this.ctx.toAdd.id) {
        alertsSvc.error('Select the consent to add...');
        return;
      }

      const consent = await dpSvc.addConsent(this.dp, this.ctx.toAdd);
      this.ctx.consents.push(consent);
      this.hideAddConsent();
      alertsSvc.success('Consent added!');
    },

    confirmDeleteConsent: function(consent) {
      this.ctx.toDelete = consent;
      this.$refs.deleteConsentDialog.open();
    },

    deleteConsent: async function() {
      const deleted = await dpSvc.deleteConsent(this.dp, this.ctx.toDelete);
      const idx = this.ctx.consents.findIndex(consent => consent.id == deleted.id);
      this.ctx.consents.splice(idx, 1);
      alertsSvc.success('Consent deleted!');
      this.cancelDeleteConsent();
    },

    cancelDeleteConsent: function() {
      this.$refs.deleteConsentDialog.close();
    },

    loadStatements: async function(searchTerm) {
      if (!searchTerm) {
        if (this.ctx.defStatements) {
          return this.ctx.defStatements;
        }
      } else if (this.ctx.defStatements && this.ctx.defStatements.length < 100) {
        searchTerm = searchTerm.toLowerCase();
        return this.ctx.defStatements.filter(
          stmt => this.matches(stmt.statement, searchTerm) || this.matches(stmt.code, searchTerm)
        );
      }

      const statements = await dpSvc.getConsentStatements(searchTerm);
      statements.forEach(stmt => stmt.caption = stmt.statement + ' (' + stmt.code + ')');
      if (!searchTerm) {
        this.ctx.defStatements = statements;
      }

      return statements;
    },

    matches: function(source, test) {
      return source && source.toLowerCase().indexOf(test) > -1;
    }
  }
}

</script>

<style scoped>

.os-card-items .item {
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 1.25rem;
}

.os-card-items .item:after {
  content: ' ';
  display: block;
  clear: both;
}

.os-card-items .item .left {
  float: left;
}

.os-card-items .item .right {
  float: right;
}

</style>
