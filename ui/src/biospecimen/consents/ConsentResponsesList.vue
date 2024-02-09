<template>
  <os-page-toolbar v-if="!editMode && !cp.consentsSource">
    <template #default>
      <os-button left-icon="edit" :label="$t('participant_consents.edit_responses')"
        @click="editResponses" />

      <os-button left-icon="upload" :label="$t('participant_consents.upload_new_form')"
        @click="showUploadDoc" v-if="consent.consentDocumentName"/>

      <os-button left-icon="trash" :label="$t('participant_consents.delete_form')"
        @click="deleteForm" v-if="consent.consentDocumentName" />

      <os-button left-icon="upload" :label="$t('participant_consents.upload_form')"
        @click="showUploadDoc" v-if="!consent.consentDocumentName" />
    </template>
  </os-page-toolbar>

  <os-grid v-if="!editMode">
    <os-grid-column width="12">
      <os-section>
        <template #title>
          <span v-t="'participant_consents.signed_consent_form'">Signed Consent Form</span>
        </template>
        <template #content>
          <div class="signed-form">
            <div class="filename" v-if="consent.consentDocumentName">
              <a :href="docUrl" target="_blank">{{consent.consentDocumentName}}</a>
            </div>
            <div v-else>
              <span>-</span>
            </div>
          </div>
        </template>
      </os-section>
      <os-section>
        <template #title>
          <span v-t="'participant_consents.responses'">Responses</span>
        </template>
        <template #content>
          <os-overview :bg-col="true" :schema="headerFields" :object="{consent: consent}" :columns="1" />

          <div class="responses">
            <div class="response" v-for="(response, idx) of consent.responses" :key="idx">
              <div class="statement">
                <span>{{response.statement}}</span>
                <span v-if="response.code">&nbsp; ({{response.code}})</span>
              </div>
              <div class="answer">{{response.response || '-'}}</div>
            </div>
          </div>
        </template>
      </os-section>
    </os-grid-column>
  </os-grid>

  <os-form :schema="formSchema" :data="dataCtx" v-if="editMode">
    <os-button primary :label="$t('common.buttons.update')" @click="updateResponses" />
    <os-button text :label="$t('common.buttons.cancel')" @click="cancelEditResponses" />
  </os-form>

  <os-confirm-delete ref="confirmDeleteFormDialog" :captcha="false">
    <template #message>
      <span v-t="{path: 'participant_consents.confirm_form_delete', args: consent}">Are you sure you want to delete the signed consent form: {{consent.consentDocumentName}}?</span>
    </template>
  </os-confirm-delete>

  <os-dialog ref="uploadDocDialog">
    <template #header>
      <span v-t="'participant_consents.upload_consent_form'">Upload Consent Form</span>
    </template>
    <template #content>
      <div>
        <os-label>
          <span v-t="'participant_consents.choose_signed_consent_form'">Choose the signed consent form</span>
        </os-label>
        <os-file-upload ref="consentDocUploader" :url="docUrl" :auto="false" :headers="reqHeaders" />
      </div>
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')" @click="cancelDocUpload" />
      <os-button primary :label="$t('common.buttons.upload')" @click="uploadDoc" />
    </template>
  </os-dialog>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import cprSvc from '@/biospecimen/services/Cpr.js';
import http from '@/common/services/HttpClient.js';
import pvSvc from '@/common/services/PermissibleValue.js';
import util from '@/common/services/Util.js';

export default {
  props: ['cp', 'cpr'],

  data() {
    return {
      consent: {responses: []},

      editMode: false,

      responsePvs: null,

      dataCtx: {}
    }
  },

  created() {
    this._loadConsent();
  },

  computed: {
    docUrl: function() {
      return cprSvc.getConsentDocUrl(this.cpr);
    },

    reqHeaders: function() {
      return http.headers;
    },

    headerFields: function() {
      return [
        {
          name: "consent.consentSignatureDate",
          type: "datePicker",
          labelCode: "participant_consents.sign_date"
        },
        {
          name: "consent.witness",
          type: "user",
          labelCode: "participant_consents.witness"
        },
        {
          name: "consent.comments",
          type: "text",
          labelCode: "participant_consents.comments"
        }
      ]
    },

    formSchema: function() {
      const rows = this.headerFields.map(field => ({fields: [field]}));

      let idx = 0;
      for (let response of this.consent.responses) {
        rows.push({
          fields: [{
            type: 'radio',
            label: response.statement + (response.code ? ' (' + response.code + ')' : ''),
            name: 'consent.responses.' + idx + '.response',
            options: this.responsePvs || [],
            optionsPerRow: Math.min((this.responsePvs && this.responsePvs.length), 5),
            clearOption: true
          }]
        });

        ++idx;
      }
      
      return {
        rows: rows
      }
    }
  },

  methods: {
    editResponses: function() {
      this.editMode = true;
      this.dataCtx = {consent: util.clone(this.consent || {})};
      if (!this.responsePvs) {
        pvSvc.getPvs('consent_response').then(
          responses => this.responsePvs = responses.map(({value}) => ({caption: value, value}))
        );
      }
    },

    updateResponses: function() {
      cprSvc.updateConsents(this.cpr, this.dataCtx.consent).then(
        (savedConsent) => {
          this.consent = savedConsent;
          alertsSvc.success({code: 'participant_consents.consents_updated'});
          this.cancelEditResponses();
        }
      );
    },

    cancelEditResponses: function() {
      this.editMode = false;
      this.dataCtx = {consent: {}};
    },

    deleteForm: function() {
      this.$refs.confirmDeleteFormDialog.open().then(
        resp => {
          if (resp == 'proceed') {
            cprSvc.deleteConsentDoc(this.cpr).then(
              () => {
                alertsSvc.success({code: 'participant_consents.signed_form_deleted'});
                this.consent.consentDocumentName = null;
              }
            );
          }
        }
      );
    },

    showUploadDoc: function() { 
      this.$refs.uploadDocDialog.open();
    },

    uploadDoc: function() {
      this.$refs.consentDocUploader.upload().then(
        (consentDocumentName) => {
          alertsSvc.success({code: 'participant_consents.signed_form_uploaded'});
          this.consent.consentDocumentName = consentDocumentName;
          this.cancelDocUpload();
        }
      );
    },

    cancelDocUpload: function() {
      this.$refs.uploadDocDialog.close();
    },

    _loadConsent: function() {
      cprSvc.getConsents(this.cpr).then(consent => this.consent = consent);
    }
  }
}
</script>

<style scoped>
.signed-form {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.signed-form .filename {
  margin-right:  0.5rem;
}

.responses .response {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  border-radius: 0.5rem;
  padding: 1rem;
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 2px 6px 2px rgba(60,64,67,.15);
  margin-bottom: 1rem;
}

.responses .response .statement {
  font-weight: bold;
  font-style: italic;
  opacity: 0.6;
  margin-bottom: 0.5rem;
}
</style>
