<template>
  <os-page>
    <os-page-head>
      <span>
        <h3 v-t="'settings.list'">Settings</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-input-text v-model="ctx.search" :placeholder="$t('common.buttons.search')"
            @update:model-value="searchSetting" />
        </template>
      </os-page-toolbar>

      <os-grid>
        <os-grid-column :width="3">
          <os-panel>
            <template #header>
              <span v-t="'settings.modules'">Modules</span>
            </template>
            <template #default>
              <div class="modules-list">
                <div class="module" :class="ctx.selectedModule == module ? 'selected' : ''"
                  v-for="(module, idx) in ctx.modules" :key="idx" @click="selectModule(module)">
                  <span v-t="'settings.' + module + '.title'">{{module}}</span>
                </div>
              </div>
            </template>
          </os-panel>
        </os-grid-column>

        <os-grid-column :width="9">
          <os-panel>
            <template #header>
              <span v-t="'settings.' + ctx.selectedModule + '.title'">{{ctx.selectedModule}}</span>
            </template>
            <template #default>
              <table class="settings" v-if="!property">
                <thead>
                  <tr>
                    <th style="width: 25%;">
                      <span v-t="'settings.property'">Property</span>
                    </th>
                    <th style="width: 45%;">
                      <span v-t="'settings.description'">Description</span>
                    </th>
                    <th style="width: 30%;">
                      <span v-t="'settings.value'">Value</span>
                    </th>
                    <th>&nbsp;</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(setting, idx) in ctx.settings[ctx.selectedModule]"
                    :key="idx" @click="selectSetting(setting)">
                    <td>
                      <span>{{setting.uiName}}</span>
                    </td>
                    <td>
                      <span>{{setting.uiDesc}}</span>
                    </td>
                    <td>
                      <ValueSpan :setting="setting" />
                    </td>
                    <td>
                      <os-button left-icon="history" size="small"
                        @click="showHistory($event, setting.module, setting.name)" />
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-else>
                <os-form :schema="{rows: []}" v-if="ctx.selectedSetting && ctx.selectedSetting.name">
                  <template v-slot:[`static-fields`]>
                    <div class="row">
                      <div class="field">
                        <os-label>
                          <span v-t="'settings.property'">Property</span>
                        </os-label>
                        <span>{{ctx.selectedSetting.uiName}}</span>
                      </div>
                    </div>
                    <div class="row">
                      <div class="field">
                        <os-label>
                          <span v-t="'settings.description'">Description</span>
                        </os-label>
                        <span>{{ctx.selectedSetting.uiDesc}}</span>
                      </div>
                    </div>
                    <div class="row">
                      <div class="field">
                        <os-label>
                          <span v-t="'settings.existing_value'">Existing Value</span>
                        </os-label>
                        <div>
                          <ValueSpan :setting="ctx.selectedSetting" />
                        </div>
                      </div>
                    </div>
                    <div class="row">
                      <div class="field">
                        <os-label>
                          <span v-t="'settings.last_updated'">Last Updated</span>
                        </os-label>
                        <span>{{$filters.dateTime(ctx.selectedSetting.activationDate)}}</span>
                      </div>
                    </div>
                    <div class="row">
                      <div class="field">
                        <os-label>
                          <span v-t="'settings.new_value'">New Value</span>
                        </os-label>
                        <os-input-number v-model="ctx.newValue" :maxFractionDigits="0"
                          v-if="ctx.selectedSetting.type == 'INT'" />
                        <os-input-number v-model="ctx.newValue" :maxFractionDigits="6"
                          v-else-if="ctx.selectedSetting.type == 'FLOAT'" />
                        <os-select-button v-model="ctx.newValue"
                          :options="[
                            {label: $t('common.enabled'), value: true},
                            {label: $t('common.disabled'), value: false}
                          ]" option-label="label" option-value="value"
                          v-else-if="ctx.selectedSetting.type == 'BOOLEAN'" />
                        <os-file-upload ref="fileUploader" v-model="ctx.newValue"
                          :headers="reqHeaders" :auto="false" :url="ctx.selectedSetting.fileUrl"
                          v-else-if="ctx.selectedSetting.type == 'FILE'" />
                        <os-input-text v-model="ctx.newValue" v-else />
                      </div>
                    </div>
                  </template>

                  <div>
                    <os-button primary :label="$t('common.buttons.update')" @click="updateSetting" />
                    <os-button text :label="$t('common.buttons.cancel')" @click="cancelUpdateSetting" />
                  </div>
                </os-form>
              </div>
            </template>
          </os-panel>
        </os-grid-column>
      </os-grid>
    </os-page-body>

    <os-dialog ref="historyDialog">
      <template #header>
        <span>{{$t('settings.' + ctx.history[0].module + '.title')}}: {{$t('settings.' + ctx.history[0].module + '.' + ctx.history[0].name)}}</span>
      </template>
      <template #content>
        <table class="settings">
          <thead>
            <tr>
              <th style="min-width: 12rem;" v-t="'settings.updated_on'">Updated On</th>
              <th style="min-width: 12rem;" v-t="'settings.updated_by'">Updated By</th>
              <th style="max-width: 20rem;" v-t="'settings.value'">Value</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(rev, idx) of ctx.history" :key="idx">
              <td>{{$filters.dateTime(rev.activationDate)}}</td>
              <td>{{$filters.username(rev.activatedBy)}}</td>
              <td><ValueSpan :setting="rev" :show-link="idx == 0" /></td>
            </tr>
          </tbody>
        </table>
      </template>
      <template #footer>
        <os-button primary :label="$t('common.buttons.done')" @click="closeHistory" />
      </template>
    </os-dialog>
  </os-page>
</template>

<script>

import http from '@/common/services/HttpClient.js';
import i18n from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';

import ValueSpan from './Value.vue';

export default {
  props: ['module', 'property', 'search'],

  components: {
    ValueSpan
  },

  data() {
    return {
      ctx: {
        ui: this.$ui,

        modules: [],

        settings: {},

        selectedModule: '',

        search: ''
      }
    };
  },

  created() {
    this.loadSettings().then(() => this._showDetails());
  },

  watch: {
    module: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._showDetails();
      }
    },

    property: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._showDetails();
      }
    },
  },

  computed: {
    reqHeaders: function() {
      return http.headers;
    }
  },

  methods: {
    loadSettings: function() {
      return settingSvc.getSettings().then(
        settings => {
          const settingsMap = {};
          for (let setting of settings) {
            settingsMap[setting.module] = settingsMap[setting.module] || [];
            settingsMap[setting.module].push(setting);

            setting.uiName = i18n.msg('settings.' + setting.module + '.' + setting.name);
            setting.uiDesc = i18n.msg('settings.' + setting.module + '.' + setting.name + '_desc');
            if (setting.type == 'FILE') {
              setting.fileUrl = this.downloadUrl(setting);
              setting.filename = setting.value;
              if (setting.value && setting.value.indexOf('classpath:') != 0) {
                setting.filename = setting.value.substring(setting.value.indexOf('_') + 1);
              }
            }
          }

          this.ctx.modules = Object.keys(settingsMap);
          this.ctx.settings = this.ctx.pristine = settingsMap;
          if (!this.module) {
            this.selectModule(this.ctx.modules[0]);
          }

          return this.ctx.settings;
        }
      );
    },

    selectModule: function(module) {
      routerSvc.goto('SettingsList', {}, {module, property: undefined});
    },

    selectSetting: function(setting) {
      const {module, name: property} = setting;
      routerSvc.goto('SettingsList', {}, {module, property});
    },

    updateSetting: function() {
      const {module, name, type} = this.ctx.selectedSetting;
      const payload = {module, name, value: this.ctx.newValue};
      if (type == 'FILE' && this.$refs.fileUploader.hasFiles()) {
        this.$refs.fileUploader.upload().then(
          fileId => {
            payload.value = fileId;
            this._saveSetting(payload);
          }
        );
      } else {
        this._saveSetting(payload);
      }
    },

    cancelUpdateSetting: function() {
      routerSvc.goto('SettingsList', {}, {module: this.module});
    },

    showHistory: function(event, module, property) {
      event.stopPropagation();
      settingSvc.getHistory(module, property).then(
        (history) => {
          this.ctx.history = history;
          let isFile = false;
          if (history.length > 0) {
            isFile = history[0].type == 'FILE';
          }
          for (let point of history) {
            if (isFile) {
              point.fileUrl = this.downloadUrl(point);
              point.filename = point.value;
              if (point.value && point.value.indexOf('classpath:') != 0) {
                point.filename = point.value.substring(point.value.indexOf('_') + 1);
              }
            }
          }   

          this.$refs.historyDialog.open();
        }
      );
    },

    closeHistory: function() {
      this.ctx.history = [];
      this.$refs.historyDialog.close();
    },

    searchSetting: function() {
      if (this.ctx.searchTimer) {
        clearTimeout(this.ctx.searchTimer);
      }

      if (!this.ctx.search) {
        this.ctx.settings = this.ctx.pristine;
        this.ctx.modules = Object.keys(this.ctx.pristine);
        routerSvc.goto('SettingsList', {}, {module: this.ctx.modules[0]});
        return;
      }

      const search = this.ctx.search.toLowerCase();
      this.ctx.searchTimer = setTimeout(
        () => {
          const result = {};
          for (let module of Object.keys(this.ctx.pristine)) {
            for (let setting of this.ctx.pristine[module]) {
              if (setting.uiName.toLowerCase().indexOf(search) != -1) {
                result['all'] = result['all'] || [];
                result[module] = result[module] || [];
                result[module].push(setting);
                result['all'].push(setting);
              }
            }
          }

          this.ctx.settings = result;
          this.ctx.modules = Object.keys(result);
          routerSvc.goto('SettingsList', {}, {module: 'all'});
        },
        1000
      );
    },

    downloadUrl: function(setting) {
      return settingSvc.getDownloadUrl(setting);
    },

    _showDetails: function() {
      if (this.module && this.property) {
        const moduleSettings = this.ctx.settings[this.module];
        if (!moduleSettings) {
          alert('Invalid module: ' + this.module);
          routerSvc.goto('SettingsList', {}, {module: undefined, property: undefined});
          return;
        }

        const setting = moduleSettings.find(({name}) => name == this.property);
        if (!setting) {
          alert('Invalid setting: ' + this.property);
          routerSvc.goto('SettingsList', {}, {module: this.module, property: undefined});
          return;
        }

        this.ctx.selectedModule = this.module;
        this.ctx.selectedSetting = setting;
        this.ctx.newValue = null;
      } else if (this.module) {
        const moduleSettings = this.ctx.settings[this.module];
        if (!moduleSettings) {
          alert('Invalid module: ' + this.module);
          routerSvc.goto('SettingsList', {}, {module: undefined, property: undefined});
          return;
        }

        this.ctx.selectedModule = this.module;
        this.ctx.selectedSetting = null;
      }
    },

    _saveSetting: function(setting) {
      settingSvc.saveSetting(setting).then(
        savedSetting => {
          savedSetting.uiName = i18n.msg('settings.' + savedSetting.module + '.' + savedSetting.name);
          savedSetting.uiDesc = i18n.msg('settings.' + savedSetting.module + '.' + savedSetting.name + '_desc');
          if (savedSetting.type == 'FILE') {
            savedSetting.fileUrl = this.downloadUrl(savedSetting);
            savedSetting.filename = savedSetting.value;
            if (savedSetting.value && savedSetting.value.indexOf('classpath:') != 0) {
              savedSetting.filename = savedSetting.value.substring(savedSetting.value.indexOf('_') + 1); 
            }   
          }

          Object.assign(this.ctx.selectedSetting, savedSetting);
          routerSvc.goto('SettingsList', {}, {module: savedSetting.module});
        }
      );
    }
  }
}
</script>

<style scoped>
.modules-list {
  margin: -1rem;
}

.module {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #ddd;
  cursor: pointer;
}

.module:last-child {
  border-bottom: 0px;
}

.module.selected {
  background: #337ab7;
  border-color: #337ab7;
  color: #fff;
}

.module:hover:not(.selected) {
  background: #f5f5f5;
}

table.settings {
  border-collapse: collapse;
  width: 100%;
}

table.settings thead tr th {
  vertical-align: bottom;
  border-bottom: 1px solid #ddd;
  font-weight: bold;
  padding: 0.5rem;
  text-align: left;
}

table.settings tbody tr td {
  vertical-align: top;
  border-top: 1px solid #ddd;
  padding: 0.5rem;
  word-break: break-word;
}

table.settings tbody tr:hover {
  background: #f5f5f5;
}

table.settings tbody tr {
  cursor: pointer;
}
</style>
