<template>
  <div>
    <os-form ref="regForm" :schema="schema" :data="ctx" @input="handleInput($event)">
      <div>
        <os-button primary :label="$t('participants.add_participant')" @click="register" />

        <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
      </div>
    </os-form>
  </div>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import cprSvc from '@/biospecimen/services/Cpr.js';

import routerSvc from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';

export default {
  props: ['cpr'],

  data() {
    return {
      ctx: {
        cpr: {
          registrationDate: Date.now()
        },

        showPpid: false
      },

      schema: {
        rows: []
      }
    }
  },

  created() {
    this._loadSchema();
  },

  methods: {
    handleInput: function({field, data}) {
      if (field.name == 'cpr.cp') {
        const cp = data.cpr.cp;
        this.ctx.showPpid = cp && (!cp.ppidFmt || cp.manualPpidEnabled);
        data.cpr.cpId = cp && cp.id;
        data.cpr.cpShortTitle = cp && cp.shortTitle;
      }
    },

    register: function() {
      if (!this.$refs.regForm.validate()) {
        return;
      }

      const cprToSave = {...this.ctx.cpr};
      cprToSave.participant = {
        id: this.cpr.participant.id,
        pmis: (this.cpr.participant.pmis || []).filter(pmi => !!pmi.siteName)
      };

      delete cprToSave.cp;
      cprSvc.saveOrUpdate(cprToSave).then(
        (savedCpr) => {
          routerSvc.goto('ParticipantsListItemDetail.Overview', {cpId: savedCpr.cpId, cprId: savedCpr.id});
        }
      );
    },

    cancel: function() {
      const route = this.$route.matched[this.$route.matched.length - 1];
      const detailRouteName = route.name.split('.')[0];
      routerSvc.goto(detailRouteName + '.Overview');
    },

    _loadSchema: async function() {
      const regCps = (this.cpr.participant.registeredCps || []).map(cp => cp.cpShortTitle);
      regCps.push(this.cpr.cpShortTitle);

      const setting = await settingSvc.getSetting('biospecimen', 'mrn_restriction_enabled');
      const mrnAccessRestriction = setting[0].value == 'true';
      let sites = [];
      if (mrnAccessRestriction) {
        sites = ((this.cpr.participant && this.cpr.participant.pmis) || []).map(pmi => pmi.siteName);
      }

      const staticSchema = {
        rows: [
          {
            fields: [
              {
                type: 'dropdown',
                name: 'cpr.cp',
                labelCode: 'participants.cp',
                listSource: {
                  loadFn: ({query}) =>
                    cpSvc.getCpsForRegistrations(sites, query).then(
                      cps => cps.filter(cp => !cp.specimenCentric && regCps.indexOf(cp.shortTitle) == -1)
                    ),
                  displayProp: 'shortTitle'
                },
                validations: {
                  required: {
                    messageCode: "participants.cp_req"
                  }
                }
              }
            ]
          },

          {
            fields: [
              {
                type: 'datePicker',
                name: 'cpr.registrationDate',
                labelCode: 'participants.registration_date',
                validations: {
                  required: {
                    messageCode: 'participants.registration_date_req'
                  }
                }
              }
            ]
          },

          {
            fields: [
              {
                type: 'text',
                name: 'cpr.ppid',
                labelCode: 'participants.ppid',
                validations: {
                  required: {
                    messageCode: 'participants.ppid_req'
                  }
                },
                showWhen: 'showPpid'
              }
            ]
          }
        ]
      }

      this.schema = staticSchema;
    }
  }
}
</script>

<style>
// .os-tab-menu {
//  display: none;
// }
</style>
