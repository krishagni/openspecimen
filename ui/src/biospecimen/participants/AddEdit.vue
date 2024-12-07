<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>
          <span v-if="!dataCtx.cpr.id" v-t="'participants.register_participant'">Register Participant</span>
          <span v-else v-t="{path: 'common.update', args: {name}}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <!-- the lookup form is displayed only at the time of registering a new participant AND
           (two step registration is enabled or lookup fields are configured -->
      <os-form ref="lookupForm" :schema="ctx.lookupFs" :data="dataCtx" v-if="showLookupForm && ctx.step == 'lookup'">
        <div>
          <os-button primary :label="$t('common.buttons.lookup')" @click="lookup('lookupForm')" />

          <os-button secondary :label="$t('common.buttons.back')" @click="back" v-if="ctx.history.length > 0" />

          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>

      <div v-else-if="ctx.step == 'choose_match'">
        <os-message type="info">
          <span v-t="'participants.following_matches_found'">Following matching participant/s were found: </span>
        </os-message>

        <os-table-form ref="selectMatchForm" :schema="ctx.selectMatchTs" :data="dataCtx" :items="ctx.matches"
          :read-only="true" :selection-mode="'radio'" @item-selected="selectMatch"
          v-if="ctx.selectMatchTs.columns.length > 0">

          <os-button primary v-if="ctx.selectedMatch" @click="useSelectedMatch"
            :label="$t(!dataCtx.cpr.id || ctx.selectedMatch.participant.id == -1 ?
              'participants.use_selected_match' : 'participants.merge_with_selected_part')" />

          <os-button primary v-else-if="ctx.allowIgnoreMatches"
            :label="$t('participants.ignore_matches_proceed')" @click="ignoreMatches" />

          <os-button secondary :label="$t('common.buttons.back')" @click="back" v-if="ctx.history.length > 0" />

          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </os-table-form>
      </div>

      <os-form ref="cprForm" :schema="ctx.addEditFs" :disabled-fields="ctx.lockedFields"
        :data="dataCtx" @input="handleInput($event)" v-else>
        <template #message v-if="ctx.autoSelectedMatch">
          <os-message type="info">
            <span v-t="{path: 'participants.following_match_found', args: {cps: autoSelectedMatchCps}}">
              Following matching participant found ({{autoSelectedMatchCps}}). Please review the details and save.
            </span>
          </os-message>
        </template>

        <div>
          <os-button primary :label="$t('common.buttons.save_draft')"
            @click="saveOrUpdate(null, true)" v-if="dataCtx.cp.draftDataEntry" />

          <os-button primary :label="$t(!dataCtx.cpr.id ? 'participants.add_participant' : 'common.buttons.update')"
            @click="saveOrUpdate()" />

          <os-button primary :label="$t('participants.proceed_to_consent')" @click="saveOrUpdate(null, false, true)"
            v-if="ctx.showProceedToConsent" />

          <os-button primary :label="$t('participants.proceed_to_collection')" @click="saveOrUpdate(cpEvents[0])"
            v-if="!dataCtx.cpr.id && cpEvents.length == 1" />

          <os-menu primary :label="$t('participants.proceed_to_collection')"
            :options="cpEventOptions" v-else-if="!dataCtx.cpr.id && cpEvents.length > 1" />

          <os-button secondary :label="$t('common.buttons.back')" @click="back" v-if="ctx.history.length > 0" />

          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>

      <os-confirm ref="confirmMergeParticipant">
        <template #title>
          <span v-t="'participants.merging_participant'">Merging Participant</span>
        </template>
        <template #message>
          <span v-t="{path: 'participants.merge_participant_warning', args: dataCtx.cpr.participant}">
            <span>Participant will be deleted after merge. Do you want to continue?</span>
          </span>
        </template>
      </os-confirm>

      <os-edc-confirm-survey-mode ref="selectSurveyMode" :object="ctx.savedCpr" v-if="ctx.showProceedToConsent" />
    </os-page-body>
  </os-page>
</template>

<script>

import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import cprSvc     from '@/biospecimen/services/Cpr.js';

import alertsSvc  from '@/common/services/Alerts.js';
import formUtil   from '@/common/services/FormUtil.js';
import pvSvc      from '@/common/services/PermissibleValue.js';
import routerSvc  from '@/common/services/Router.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['cpr', 'participantId'],

  inject: ['cpViewCtx'],

  data() {
    const cp = this.cpViewCtx.getCp();
    const copy = this.cpr ? util.clone(this.cpr) : {participant: {pmis: [], source: 'OpenSpecimen'}};
    copy.cpId = cp.id;
    if (!copy.id) {
      const cd    = new Date();
      const year  = cd.getFullYear();
      const month = (cd.getMonth() < 9 ? '0' : '') + (cd.getMonth() + 1);
      const date  = (cd.getDate() <= 9 ? '0' : '') + cd.getDate();
      copy.registrationDate = year + '-' + month + '-' + date;
    }

    if (copy.participant.extensionDetail) {
      //
      // attrs map created might be suitable for display mode
      // therefore clean it up. create the map suitable for data entry
      //
      delete copy.participant.extensionDetail.attrsMap;
    }

    formUtil.createCustomFieldsMap(copy.participant);

    return {
      dataCtx: {
        cpr: copy,

        objName: 'cpr',

        objCustomFields: 'cpr.participant.extensionDetail.attrsMap',

        cp,

        userRole: this.cpViewCtx.getRole(),

        accessBasedOnMrn: this.cpViewCtx.accessBasedOnMrn
      },

      ctx: {
        addEditFs: {rows: []},

        lookupFields: [],

        lookupFs: {rows: []},

        selectMatchTs: {columns: []},

        step: 'lookup',

        history: [],

        matches: [],

        lockedFields: [],

        bcrumb: [{url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: cp.shortTitle}],

        showProceedToConsent: false
      }
    };
  },

  async created() {
    const cpr = this.dataCtx.cpr;
    if (!cpr.id && this.participantId > 0) {
      cpr.participant = await cprSvc.getParticipant(this.participantId);
      this.ctx.step = 'register';
    }

    const p = cpr.participant || {};
    const cpCtx = this.cpViewCtx;
    const {ecDocSvc, ecValidationSvc} = this.$osSvc;
    if (ecDocSvc && !cpr.id && cpCtx.isProceedToConsentAllowed() && !this.dataCtx.cp.visitLevelConsents) {
      const {count} = await ecDocSvc.getCpDocumentsCount(this.dataCtx.cp.id, {includeOnlyActive: true});
      this.ctx.showProceedToConsent = count >= 1;
      if (count >= 1) {
        const cpRules = await ecValidationSvc.getCpRules(this.dataCtx.cp.id);
        this.ctx.hasConsentRules = cpRules && cpRules.rules && cpRules.rules.length > 0;
      }
    } else {
      this.ctx.showProceedToConsent = false;
      this.ctx.hasConsentRules = false;
    }

    let cpEventsQ = null, eventRulesQ = null;
    if (this.ctx.showProceedToConsent && this.ctx.hasConsentRules) {
      cpEventsQ = new Promise((resolve) => resolve([]));
      eventRulesQ = new Promise((resolve) => resolve(null));
    } else {
      cpEventsQ = cpCtx.getCpEvents();
      eventRulesQ = cpCtx.getAnticipatedEventsRules();
    }

    const promises = [
      cpCtx.getCprDict(true),
      cpCtx.getCprAddEditLayout(),
      cpCtx.isTwoStepEnabled(),
      cpCtx.isAddPatientOnLookupFailEnabled(),
      cpCtx.getLockedParticipantFields(p.source),
      cpEventsQ,
      eventRulesQ
    ];

    Promise.all(promises).then(
      ([fields, layout, twoStep, addOnLookupFail, lockedFields, cpEvents, eventsRules]) => {
        const formSchema = this.ctx.addEditFs = formUtil.getFormSchema(fields, layout);
        if (!cpr.id || cpr.id <= 0) {
          formUtil.setDefaultValues(formSchema, this.dataCtx);
        }

        this.ctx.twoStep = twoStep;
        this.ctx.addPatientOnLookupFail = addOnLookupFail;
        this.ctx.lockedFields = lockedFields;

        let lookupFields = fields.filter(field => field.lookupParticipant == true || field.participantLookup == true);
        if (lookupFields.length == 0 && twoStep) {
          lookupFields = cprSvc.getDefaultLookupFields(fields);
        }

        this.ctx.lookupFields = lookupFields;
        this.ctx.lookupFs = {rows: lookupFields.map(field => ({ fields: [field] }))};
        if (lookupFields.length == 0) {
          this.ctx.twoStep = false;
        }

        this.ctx.allCpEvents = cpEvents;
        this.ctx.eventsRules = eventsRules;
      }
    );

    pvSvc.getPvs('vital_status', null, {includeOnlyLeafValue: true, includeProps: true}).then(
      (vitalStatuses) => {
        this.deadStatuses = vitalStatuses
          .filter(status => status.props && (status.props.dead == true || status.props.dead == 'true'))
          .map(status => status.value);

        p.dead = this.deadStatuses.indexOf(p.vitalStatus) >= 0;
      }
    );

  },

  computed: {
    name: function() {
      return cprSvc.getFormattedTitle(this.dataCtx.cpr);
    },

    showLookupForm: function() {
      return !this.dataCtx.cpr.id && (this.ctx.lookupFields.length > 0 || this.ctx.twoStep);
    },

    autoSelectedMatchCps: function() {
      const match = this.ctx.autoSelectedMatch;
      if (match) {
        return (match.registeredCps || []).map(reg => reg.cpShortTitle).join(', ');
      }

      return 'none';
    },

    cpEvents: function() {
      const {allCpEvents, eventsRules} = this.ctx;
      const allowedEvents = cprSvc.getAllowedEvents(this.dataCtx.cpr, eventsRules);
      return (allCpEvents || [])
        .filter(cpEvent => !allowedEvents || !cpEvent.code || allowedEvents.indexOf(cpEvent.code) >= 0);
    },

    cpEventOptions: function() {
      return (this.cpEvents || [])
        .map(event => ({caption: cpSvc.getEventDescription(event), onSelect: () => this.saveOrUpdate(event)}));
    }
  },

  methods: {
    handleInput: function({field, value}) {
      const cpr = this.dataCtx.cpr;
      const p = cpr.participant;
      if (field.name == 'cpr.participant.phoneNumber' && !value) {
        p.textOptIn = false;
      } else if (field.name == 'cpr.participant.emailAddress' && !value) {
        p.emailOptIn = false;
      } else if (field.name == 'cpr.participant.birthDate') {
        if (value) {
          p.birthDateStr = util.formatDate(value, 'yyyy-MM-dd');
        } else {
          p.birthDateStr = null;
        }
      } else if (field.name == 'cpr.participant.vitalStatus') {
        p.dead = this.deadStatuses.indexOf(p.vitalStatus) >= 0;
        if (!p.dead) {
          p.deathDate = null;
        }
      }
    },

    lookup: function(formName) {
      formName = formName || 'lookupForm';
      if (!this.$refs[formName].validate()) {
        return;
      }

      const {cpr} = this.dataCtx;
      cprSvc.getMatchingParticipants(cpr).then(
        matches => this._handleParticipantMatches(matches, {step: 'lookup', data: util.clone(cpr)})
      );
    },

    selectMatch: function({index}) {
      this.ctx.selectedMatch = this.ctx.matches[index];
    },

    useSelectedMatch: function() {
      this.ctx.history.unshift({
        step: 'choose_match',
        data: {cpr: util.clone(this.dataCtx.cpr), matches: util.clone(this.ctx.matches)}
      });

      this._useSelectedMatch(this.ctx.selectedMatch);
    },

    saveOrUpdate: function(cpEvent, saveAsDraft, gotoConsents) {
      if (!saveAsDraft && !this.$refs.cprForm.validate()) {
        return;
      }

      const {cpr} = this.dataCtx;
      cpr.dataEntryStatus = saveAsDraft ? 'DRAFT' : 'COMPLETE';
      this.ctx.selectedEvent = cpEvent;
      this.ctx.gotoConsents = gotoConsents;
      if (this.ctx.step == 'lookup') {
        cprSvc.getMatchingParticipants(cpr).then(
          matches => this._handleParticipantMatches(matches, {step: 'lookup', data: util.clone(cpr)})
        );
      } else {
        this._saveOrUpdate(cpr);
      }
    },

    ignoreMatches: function() {
      const {cpr} = this.dataCtx;
      if (this.showLookupForm && this.ctx.history[0].step == 'lookup') {
        // navigate to the registration form if lookup form was displayed

        this.ctx.history.unshift({
          step: 'choose_match',
          data: {cpr: util.clone(cpr), matches: util.clone(this.ctx.matches)}
        });

        this.ctx.step = 'register';
      } else {
        // save the registration if no lookup form was displayed to the user
        this._saveOrUpdate(cpr);
      }
    },

    back: function() {
      const history = this.ctx.history;
      if (history.length == 0) {
        return;
      }

      const {step, data} = history.shift();
      if (step == this.ctx.step && history.length > 0) {
        this.back();
        return;
      }

      this.ctx.selectedMatch = null;
      if (step == 'register' || step == 'lookup') {
        this.ctx.autoSelectedMatch = null;
        this.dataCtx.cpr = data;
      } else if (step == 'lookup') {
        this.ctx.matches = data;
      }

      this.ctx.step = step;
    },

    cancel: function() {
      this.ctx.selectedEvent = null;
      this._navToNextView(this.dataCtx.cpr);
    },

    _saveOrUpdate: function(cpr) {
      const toSave = util.clone(cpr);
      toSave.participant.source = 'OpenSpecimen';
      cprSvc.saveOrUpdate(toSave).then(savedCpr => this._navToNextView(savedCpr));
    },

    _handleParticipantMatches(matches, action) {
      const ctx = this.ctx;

      ctx.matches = matches;
      ctx.autoSelectedMatch = null;
      ctx.allowIgnoreMatches = matches.every(({matchedAttrs}) => matchedAttrs.length == 1 && matchedAttrs[0] == 'lnameAndDob');

      matches.forEach(
        match => {
          if (!match.participant.id && match.participant.source != 'OpenSpecimen') {
            //
            // Ask API to not use existing participant ID
            //
            match.participant.id = -1;
          }
        }
      );

      if (!matches || matches.length == 0) {
        // no matches found
        if (this.dataCtx.cpr.id > 0) {
          // editing an existing participant, save the participant
          this._saveOrUpdate(this.dataCtx.cpr);
        } else if (!ctx.addPatientOnLookupFail && this.showLookupForm) {
          alertsSvc.error({code: 'participants.no_matching_participant'});
        } else {
          if (!this.showLookupForm) {
            this._saveOrUpdate(this.dataCtx.cpr);
          } else {
            this.ctx.step = 'register';
            setTimeout(() => this.$refs.cprForm.scrollToTop(), 500);
            this.ctx.history.unshift(action);
          }
        }
      } else if (this._isAutoSelect(matches)) {
        // match is auto selected
        const {cpr} = this.dataCtx;
        const participant = ctx.autoSelectedMatch = matches[0].participant;
        if (ctx.twoStep) {
          // 2 step registration
          const cpId = cpr.cpId;
          for (let regCp of (participant.registeredCps || [])) {
            if (regCp.cpId == cpId) {
              // auto selected match is of the same CP as the current one
              // navigate to the participant overview page
              routerSvc.goto('ParticipantsListItemDetail.Overview', {cpId, cprId: regCp.cprId});
              return;
            }
          }
        }

        this._useSelectedMatch(matches[0]);
        this.ctx.history.unshift(action);
      } else {
        // multiple matches or a single match that cannot be auto selected
        this.ctx.step = 'choose_match';
        this._loadSelectMatchTabSchema();
        this.ctx.history.unshift(action);
      }
    },

    _useSelectedMatch: async function({participant}) {
      const {cpr} = this.dataCtx;

      // find if the participant is registered to the same CP
      let matchedCpr = undefined;
      for (let regCp of (participant.registeredCps || [])) {
        if (regCp.cpId == cpr.cpId) {
          matchedCpr = regCp.cprId;
          break;
        }
      }

      if (cpr.id > 0) {
        if (matchedCpr && participant.id != -1) {
          // matching participant registered to the same CP
          // ask for confirmation to merge
          const resp = await this.$refs.confirmMergeParticipant.open();
          if (resp != 'proceed') {
            return;
          }
        }

        this._updateUsingParticipant(participant);
        return;
      }

      if (!matchedCpr) {
        this._copyParticipantDetails(cpr, participant);
      } else if (matchedCpr > 0) {
        const dbCpr = await cprSvc.getCpr(matchedCpr);
        this.dataCtx.cpr = dbCpr;
        formUtil.createCustomFieldsMap(dbCpr.participant);
      }

      this.ctx.lockedFields = await this.cpViewCtx.getLockedParticipantFields(cpr.participant.source);
      this.ctx.step = 'register';
      setTimeout(() => this.$refs.cprForm.scrollToTop(), 500);
    },

    _updateUsingParticipant(participant) {
      this._copyParticipantDetails(this.dataCtx.cpr, participant);
      this._saveOrUpdate(this.dataCtx.cpr);
    },

    _isAutoSelect: function(matches) {
      if (this.dataCtx.cpr.id > 0 || matches.length > 1) {
        return false;
      }

      if (matches.length == 1 && matches[0].matchedAttrs.length == 1 && matches[0].matchedAttrs[0] == 'lnameAndDob') {
        return false;
      }

      return true;
    },

    _copyParticipantDetails(cpr, participant) {
      const customFields = cpr.participant.extensionDetail || {};
      Object.assign(cpr, {participant});
      Object.assign(cpr.participant, {extensionDetail: customFields});
    },

    _loadSelectMatchTabSchema: function() {
      if (this.ctx.selectMatchTs.columns.length > 0) {
        return;
      }

      this.cpViewCtx.getSelectMatchTabSchema().then(schema => this.ctx.selectMatchTs = schema);
    },

    _navToNextView: function(savedCpr) {
      if (savedCpr.activityStatus == 'Active') {
        if (this.ctx.gotoConsents) {
          if (this.$osSvc.edcSurveySvc) {
            this.$osSvc.edcSurveySvc.getStarterConsent(savedCpr.id).then(
              (survey) => {
                if (survey) {
                  this.ctx.savedCpr = savedCpr;
                  this.$refs.selectSurveyMode.gotoSurvey(survey).then(
                    resp => {
                      if (resp == 'cancel') {
                        this._navToConsents(savedCpr);
                      }
                    }
                  );
                } else {
                  this._navToConsents(savedCpr);
                }
              }
            );
          } else {
            this._navToConsents(savedCpr);
          }
        } else if (!this.ctx.selectedEvent || !this.ctx.selectedEvent.id) {
          routerSvc.goto('ParticipantsListItemDetail.Overview', {cpId: savedCpr.cpId, cprId: savedCpr.id})
        } else {
          this._navToCollectionWf(savedCpr);
        }
      } else {
        routerSvc.goto('ParticipantsList', {cpId: savedCpr.cpId, cprId: -1});
      }
    },

    _navToConsents: function({cpId, id: cprId}) {
      routerSvc.goto('ParticipantsListItemDetail.Consents', {cpId, cprId});
    },

    _navToCollectionWf: async function(cpr) {
      const wfInstanceSvc = this.$osSvc.tmWfInstanceSvc;
      if (wfInstanceSvc) {
        let wfName = await cpSvc.getWorkflowProperty(cpr.cpId, 'common', 'collectVisitsWf');
        if (!wfName) {
          wfName = 'sys-collect-visits';
        }

        const cpe = this.ctx.selectedEvent;
        let inputItem = {cpr, cpe};
        const opts = {
          params: {
            returnOnExit: JSON.stringify({
              name: 'ParticipantsListItemDetail.Overview',
              params: {cpId: cpr.cpId, cprId: cpr.id}
            }),
            cpId: cpr.cpId,
            'breadcrumb-1': JSON.stringify({
              label: cpr.cpShortTitle,
              route: {name: 'ParticipantsList', params: {cpId: cpr.cpId, cprId: -1}}
            }),
            'breadcrumb-2': JSON.stringify({
              label: cpr.ppid,
              route: {name: 'ParticipantsListItemDetail.Overview', params: {cpId: cpr.cpId, cprId: cpr.id}}
            }),
            batchTitle: cpSvc.getEventDescription(cpe),
            showOptions: false,
            collectVisits: true
          }
        }

        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
        wfInstanceSvc.gotoInstance(instance.id);
      } else {
        routerSvc.goto('ParticipantsListItemDetail.Overview', {cpId: cpr.cpId, cprId: cpr.id})
      }
    }
  }
}
</script>
