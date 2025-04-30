<template>
  <os-grid>
    <os-grid-column width="12">
    <os-accordion>
      <os-accordion-tab v-for="level of levels" :key="level.entityType">
        <template #header>
          <span class="title">{{level.title}}</span>
        </template>
        <template #content>
          <div>
            <ul class="os-key-values">
              <li class="item">
                <strong class="key" v-t="'cpgs.custom_fields'">Custom Fields</strong>
                <span class="value" v-if="ctx.forms[level.customFields] && ctx.forms[level.customFields].length == 1">
                  <router-link :to="{name: 'FormPreview', params: {formId: ctx.forms[level.customFields][0].formId}}"
                    target="_blank">
                    <span>{{ctx.forms[level.customFields][0].caption}}</span>
                  </router-link>
                  <os-button left-icon="times" size="small" v-if="updateAllowed" />
                </span>
                <span class="value" v-else>
                  <span v-t="'common.none'">-</span>
                  <os-button left-icon="plus" size="small" v-if="updateAllowed" />
                </span>
              </li>
            </ul>
            <os-section>
              <template #title>
                <span v-t="'cpgs.forms'">Forms</span>
              </template>
              <template #content>
                <div>
                  <div class="toolbar" v-if="updateAllowed">
                    <os-button left-icon="plus"  :label="$t('common.buttons.add')" v-if="ctx.selectedForms.length == 0" />
                    <os-button left-icon="times" :label="$t('common.buttons.remove')" v-else />
                  </div>

                  <os-list-view :data="ctx.forms[level.entityType]" :schema="formsListSchema"
                    :allow-selection="updateAllowed" @selected-rows="onFormsSelection" />
                </div>
              </template>
            </os-section>
          </div>
        </template>
      </os-accordion-tab>
    </os-accordion>
    </os-grid-column>
  </os-grid>
</template>

<script>
import cpgSvc from '@/biospecimen/services/CollectionProtocolGroup.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cpg', 'permOpts'],

  data() {
    return {
      ctx: {
        level: ['Participant', 'SpecimenCollectionGroup', 'Specimen'],

        forms: {},

        selectedForms: []
      }
    }
  },

  mounted: function() {
    this._loadForms();
  },

  computed: {
    eximAllowed: function() {
      return this.permOpts && this.permOpts.eximAllowed;
    },

    updateAllowed: function() {
      return this.permOpts && this.permOpts.updateAllowed;
    },

    levels: function() {
      return [
        {
          customFields: 'ParticipantExtension',
          entityType: 'Participant',
          title: this.$t('cpgs.participant')
        },
        {
          customFields: 'VisitExtension',
          entityType: 'SpecimenCollectionGroup',
          title: this.$t('cpgs.visit')
        },
        {
          customFields: 'SpecimenExtension',
          entityType: 'Specimen',
          title: this.$t('cpgs.specimen')
        }
      ];
    },

    formsListSchema: function() {
      return {
        columns: [
          {
            "name": "caption",
            "labelCode": "cpgs.name",
            "href": ({rowObject}) => routerSvc.getUrl('FormPreview', {formId: rowObject.formId}),
            "hrefTarget": "_blank"
          },
          {
            "name": "multipleRecords",
            "labelCode": "cpgs.multiple_records",
            "value": ({multipleRecords}) => this.$t(multipleRecords ? 'common.yes' : 'common.no')
          },
          {
            "name": "notifEnabled",
            "labelCode": "cpgs.form_notifs_enabled",
            "value": ({notifEnabled}) => this.$t(notifEnabled ? 'common.yes' : 'common.no')
          },
          {
            "name": "dataInNotif",
            "labelCode": "cpgs.form_data_in_notif",
            "value": ({dataInNotif}) => this.$t(dataInNotif ? 'common.yes' : 'common.no')
          },
          {
            "name": "notifUserGroups",
            "labelCode": "cpgs.notif_rcpts",
            "value": ({notifUserGroups}) => (notifUserGroups || []).map(({name}) => name).join(', ')
          }
        ]
      }
    }
  },

  methods: {
    onFormsSelection: function(selectedForms) {
      this.ctx.selectedForms = selectedForms.map(({rowObject}) => rowObject);
    },

    _loadForms: async function() {
      const forms = await cpgSvc.getForms(this.cpg.id);
      this.ctx.forms = forms.reduce(
        (map, groupForms) => {
          map[groupForms.level] = groupForms.forms;
          return map;
        },
        {}
      );
      this.ctx.selectedForms = [];
    }
  }
}
</script>

<style scoped>
.value {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.value :deep(.btn) {
  margin-left: 1rem;
}

.toolbar {
  margin-bottom: 1rem;
}
</style>
