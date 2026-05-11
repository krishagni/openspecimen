<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>
          <span v-if="!dataCtx.cpr.id" v-t="'participants.register_participant'">Register Participant</span>
          <span v-else-if="!ctx.header.leftTitle" v-t="{path: 'common.update', args: {name}}"></span>
          <span class="custom-title" v-else>
            <span v-t="{path: 'common.buttons.update'}">Update</span>
            <os-dynamic-template v-if="ctx.header.leftTitle" :template="ctx.header.leftTitle"
              :cp="dataCtx.cp" :cpr="cpr" :hasPhiAccess="true" />
          </span>
        </h3>
      </span>

      <template #right v-if="ctx.header.rightTitle">
        <h3>
          <os-dynamic-template :template="ctx.header.rightTitle" :cp="dataCtx.cp" :cpr="cpr" :hasPhiAccess="true" />
        </h3>
      </template>
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

      <div class="participant-matches" v-else-if="ctx.step == 'choose_match'">
        <div class="message" v-if="!ctx.detailView">
          <os-message type="info">
            <span v-t="'participants.following_matches_found'">Following matching participant/s were found: </span>
          </os-message>
        </div>

        <os-list-view class="os-list-shadowed-rows"
          :data="ctx.matches"
          :schema="ctx.selectMatchTs"
          :showRowActions="!ctx.detailView"
          :split-view="true"
          :detail-view="ctx.detailView"
          :split-view-title="$t('participants.matching_participants')"
          :selected="ctx.selectedMatch"
          @rowClicked="viewMatchingParticipantDetails"
          @detailViewClosed="showMatchingParticipantsTable"
          ref="listView">
          <template #detail="{rowObject: match}">
            <div class="match-detail-card">
              <os-overview :schema="ctx.cprDict" :object="ctx" :columns="2" v-if="ctx.cprDict && ctx.cprDict.length > 0" />

              <os-section v-if="getRegisteredCps(match).length > 0">
                <template #title>
                  <span v-t="'participants.registrations'">Participant Registrations</span>
                </template>

                <template #content>
                  <os-list-view class="os-list-shadowed-rows"
                    :data="getRegisteredCps(match)" :schema="ctx.registrationsTs"
                    :expanded="ctx.expandedRegs" @rowClicked="viewRegistrationDetails">
                    <template #expansionRow="{rowObject: reg}">
                      <template v-for="(widget, idx) of getMatchingPartWidgets(reg)" :key="idx">
                        <component :is="widget.name" :context="getRegistrationCtx(reg)" v-bind="widget.params" />
                      </template>
                    </template>
                  </os-list-view>
                </template>
              </os-section>
            </div>

            <div>
              <os-button primary  @click="useSelectedMatch(match)"
                :label="$t(!dataCtx.cpr.id || !isOsParticipant(match) ?
                'participants.use_selected_match' : 'participants.merge_with_selected_part')" />
            </div>
          </template>
        </os-list-view>

        <div class="participant-match-actions">
          <os-divider />

          <div class="buttons">
            <os-button primary v-if="ctx.allowIgnoreMatches" :label="$t('participants.ignore_matches_proceed')"
              @click="ignoreMatches" />

            <os-button secondary :label="$t('common.buttons.back')" @click="back" v-if="ctx.history.length > 0" />

            <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
          </div>
        </div>
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
          <span v-t="{path: 'participants.merge_participant_warning', args: {name: pname}}">
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

const DEFAULT_REGISTRATION_FIELDS = ['cpShortTitle', 'ppid', 'registrationDate', 'site'];

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

        registrationsTs: {columns: []},

        step: 'lookup',

        history: [],

        matches: [],

        expandedRegs: [],

        detailView: false,

        selectedMatch: null,

        lockedFields: [],

        bcrumb: [{url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: cp.shortTitle}],

        showProceedToConsent: false,

        header: {}
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

    cpSvc.getWorkflowProperty(this.dataCtx.cp.id, 'common', 'participantHeader').then(
      header => {
        if (header) {
          this.ctx.header = header;
        }
      }
    );
  },

  computed: {
    name: function() {
      return cprSvc.getFormattedTitle(this.dataCtx.cpr);
    },

    pname: function() {
      const {participant: {firstName, lastName}} = this.dataCtx.cpr;
      let result = '';
      if (firstName) {
        result += firstName;
      }

      if (lastName) {
        if (result) {
          result += ' ';
        }

        result += lastName;
      }

      return result;
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

    useSelectedMatch: function(match) {
      this.ctx.history.unshift({
        step: 'choose_match',
        data: {cpr: util.clone(this.dataCtx.cpr), matches: util.clone(this.ctx.matches)}
      });

      this._useSelectedMatch(match);
    },

    viewMatchingParticipantDetails: async function(match) {
      if (!this.ctx.participantWidgets) {
        const {cp: {id: cpId}} = this.dataCtx;
        cpSvc.getWorkflowProperty(cpId, 'matching-participants', 'detail-widgets')
          .then(widgets => this.ctx.participantWidgets = widgets || []);
      }

      await this._loadRegistrationsTableSchema();

      this.ctx.cpr = {participant: match.participant};
      this.ctx.expandedRegs = [];
      if (!this.ctx.cprDict) {
        this.ctx.cprDict = await this.getCprDict();
      }

      await this._loadRegistrationDetailsIfNeeded(match);
      this.ctx.selectedMatch = match; // set it only after the required details are loaded
      this.ctx.detailView = true;
    },

    showMatchingParticipantsTable: function() {
      this._resetMatchingParticipantDetailView();
    },

    getRegisteredCps: function(match) {
      const {participant} = match || {};
      if (!participant) {
        return [];
      }

      if (!participant.registrations) {
        participant.registrations = (participant.registeredCps || []).map(this._toRegistrationRow);
      }

      return participant.registrations;
    },

    isOsParticipant: function(match) {
      return match && match.participant && match.participant.id > 0;
    },

    viewRegistrationDetails: function(registration) {
      const {expandedRegs} = this.ctx;
      if (expandedRegs && expandedRegs.length == 1 && expandedRegs[0] == registration) {
        expandedRegs.length = 0;
      } else {
        this.ctx.expandedRegs = [registration];
      }
    },

    getRegistrationCtx: function(registration) {
      const regCpr = {
        ...(registration || {}),
        id: registration && (registration.cprId || registration.id)
      };

      return {...this.ctx, cpr: regCpr};
    },

    getMatchingPartWidgets(registration) {
      const widgetCtx = this.getRegistrationCtx(registration);
      const widgets = this._getMatchingParticipantWidgets();
      const result = [];
      for (const widget of widgets) {
        const {name, params} = widget;
        const item = {name};
        if (item.name.indexOf('os-') == -1) {
          item.name = 'os-' + name;
        }

        item.params = util.getInstantiatedParams(widgetCtx, params);
        result.push(item);
      }

      return result;
    },

    getCprDict: async function() {
      if (this.matchingPartDict) {
        return this.matchingPartDict;
      }

      const fields = await this.cpViewCtx.getCprDict();
      return this.matchingPartDict = (fields || []).filter(field => field.name.indexOf('cpr.participant.') == 0);
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

      if (step == 'register' || step == 'lookup') {
        this.ctx.autoSelectedMatch = null;
        this.dataCtx.cpr = data;
      }

      this.ctx.step = step;
    },

    cancel: function() {
      this.ctx.selectedEvent = null;
      this._navToNextView(this.dataCtx.cpr);
    },

    _resetMatchingParticipantDetailView: function() {
      this.ctx.detailView = false;
      this.ctx.selectedMatch = null;
      this.ctx.expandedRegs = [];
    },

    _saveOrUpdate: function(cpr) {
      const toSave = util.clone(cpr);
      toSave.participant.source = 'OpenSpecimen';
      cprSvc.saveOrUpdate(toSave).then(savedCpr => this._navToNextView(savedCpr));
    },

    _handleParticipantMatches(matches, action) {
      const ctx = this.ctx;

      ctx.matches = matches;
      this._resetMatchingParticipantDetailView();
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
      } else {
        // one or more matches
        this.ctx.step = 'choose_match';
        this._loadSelectMatchTabSchema();
        if (matches.length == 1) {
          this.viewMatchingParticipantDetails(matches[0]);
        }

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

    _copyParticipantDetails(cpr, participant) {
      const customFields = cpr.participant.extensionDetail || {};
      Object.assign(cpr, {participant});
      Object.assign(cpr.participant, {extensionDetail: customFields});
    },

    _loadSelectMatchTabSchema: function() {
      const {selectMatchTs: {columns}} = this.ctx;
      if (columns.length > 0) {
        return;
      }

      this.cpViewCtx.getSelectMatchTabSchema().then(schema => this.ctx.selectMatchTs = schema);
    },

    _loadRegistrationsTableSchema: async function() {
      const {registrationsTs: {columns}} = this.ctx;
      if (columns.length > 0) {
        return this.ctx.registrationsTs;
      }

      return this.ctx.registrationsTs = await this.cpViewCtx.getMatchingRegsTs();
    },

    _loadRegistrationDetailsIfNeeded: async function(match) {
      const registrations = this.getRegisteredCps(match);
      if (!this._isFullRegistrationDetailsNeeded()) {
        return;
      }

      const cprIds = registrations.map(({cprId, id}) => cprId || id).filter(id => id > 0);
      match.participant.registrations = await this._getFullRegistrations(cprIds);
    },

    _isFullRegistrationDetailsNeeded: function() {
      const {columns} = this.ctx.registrationsTs || {};
      if (!columns || columns.length == 0) {
        return false;
      }

      return columns.some(column => DEFAULT_REGISTRATION_FIELDS.indexOf(this._getRegistrationFieldName(column.name)) == -1);
    },

    _getRegistrationFieldName: function(name) {
      if (name && name.indexOf('cpr.') == 0) {
        name = name.substring(4);
      }

      return name;
    },

    _getFullRegistrations: async function(cprIds) {
      this.cprs = this.cprs || {};
      const pendingIds = cprIds.filter(cprId => !this.cprs[cprId]);
      if (pendingIds.length > 0) {
        const cprs = await cprSvc.getCprs(pendingIds);
        cprs.forEach(cpr => this.cprs[cpr.id] = this._toRegistrationRow(cpr));
      }

      return cprIds.map(cprId => this.cprs[cprId]).filter(cpr => !!cpr);
    },

    _toRegistrationRow: function(cpr) {
      if (cpr.participant) {
        formUtil.createCustomFieldsMap(cpr.participant, true);
      }

      const cprRow = {...cpr, cprId: cpr.cprId || cpr.id};
      cprRow.cpr = {...cprRow}; // Keep workflow columns configured with cpr.* paths working, e.g. cpr.ppid.
      return cprRow;
    },

    _getMatchingParticipantWidgets() {
      const {participantWidgets} = this.ctx;
      if (participantWidgets && participantWidgets.length > 0) {
        return participantWidgets;
      }

      return [this._getPrimarySpecimensTable(), this._getAliquotCountsTable()];
    },

    _getPrimarySpecimensTable() {
      return {
        name: 'aql-table',
        params: {
          static: {
            title: this.$t('participants.primary_specimens'),
            aql: `select
                    Specimen.type, Specimen.tissueSite, Specimen.spmnCollRecvDetails.collContainer, Specimen.availableQty
                  where
                    Specimen.lineage = "New" and
                    Specimen.collectionStatus = "Collected" and
                    Participant.id = :cprId`,
            params: {
              dynamic: {
                cprId: 'cpr.id'
              }
            }
          }
        }
      }
    },

    _getAliquotCountsTable() {
      return {
        name: 'aql-table',
        params: {
          static: {
            title: this.$t('participants.aliquots'),
            aql: `select
                    Specimen.type, count(distinct Specimen.id) as "Aliquots"
                  where
                    Specimen.lineage = "Aliquot" and
                    Specimen.availabilityStatus = "Available" and
                    Participant.id = :cprId`,
            params: {
              dynamic: {
                cprId: 'cpr.id'
              }
            }
          }
        }
      }
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
            cprId: cpr.id,
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

<style scoped>
.participant-matches {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.participant-matches > .message {
  align-items: flex-start;
  display: flex;
  gap: 0.5rem;
  justify-content: space-between;
}

.participant-matches > .message :deep(.p-message) {
  flex: 1;
}

.participant-matches :deep(> .os-list) {
  flex: 1;
  min-height: 0;
}

.participant-matches .participant-match-actions {
  flex: 0 0 auto;
}

.participant-matches .match-detail-card {
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
  margin-bottom: 1rem;
  padding: 1rem;
}

.buttons :deep(button) {
  margin-right: 0.5rem;
}
</style>
