<template>
  <div class="os-help" v-os-tooltip.bottom="$t('common.about.online_help')">
    <button @click="toggleHelpMenu">
      <os-icon name="question-circle" />
    </button>

    <os-overlay ref="helpMenu" @click="toggleHelpMenu">
      <ul class="help-options">
        <li>
          <a href="https://help.openspecimen.org" target="_blank" rel="noopener">
            <span v-t="'common.about.online_help'">Online Help</span>
          </a>
        </li>
        <li>
          <a href="https://forums.openspecimen.org" target="_blank" rel="noopener">
            <span v-t="'common.about.qna_forums'">Q & A Forums</span>
          </a>
        </li>
        <li class="divider">
          <os-divider />
        </li>
        <li>
          <a @click="showAboutDialog()">
            <span v-t="'common.about.openspecimen'">About OpenSpecimen</span>
          </a>
        </li>
      </ul>
    </os-overlay>

    <os-dialog ref="aboutDialog">
      <template #header>
        <span v-t="'common.about.openspecimen'">About OpenSpecimen</span>
      </template>
      <template #content>
        <os-tabs>
          <os-tab>
            <template #header>
              <os-icon-title icon="eye" :title="$t('common.overview')" />
            </template>

            <ul class="os-key-values os-one-col">
              <li class="item">
                <strong class="key key-sm" v-t="'common.about.version'">Version</strong>
                <span class="value">{{$ui.global.appProps.build_version}}</span>
              </li>
              <li class="item">
                <strong class="key key-sm" v-t="'common.about.build_date'">Build Date</strong>
                <span class="value">{{$filters.date(+$ui.global.appProps.build_date)}}</span>
              </li>
              <li class="item">
                <strong class="key key-sm" v-t="'common.about.revision'">Revision</strong>
                <span class="value">{{$ui.global.appProps.build_commit_revision}}</span>
              </li>
            </ul>
          </os-tab>

          <os-tab>
            <template #header>
              <os-icon-title icon="list" :title="$t('common.about.plugins')" />
            </template>

            <table class="os-table">
              <thead>
                <tr>
                  <th v-t="'common.about.plugin'">Plugin</th>
                  <th v-t="'common.about.version'">Version</th>
                  <th v-t="'common.about.build_date'">Build Date</th>
                  <th v-t="'common.about.revision'">Revision</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="plugin in $ui.global.appProps.pluginsDetail" :key="plugin['os-plugin-name']">
                  <td>{{plugin['os-plugin-name']}}</td>
                  <td>{{plugin['version']}}</td>
                  <td>{{$filters.date(+plugin['built-on'])}}</td>
                  <td>{{plugin['commit']}}</td>
                </tr>
              </tbody>
            </table>
          </os-tab>
        </os-tabs>
      </template>
      <template #footer>
        <div class="help-footer">
          <span>
            <a href="https://www.openspecimen.org" target="_blank" rel="noopener">OpenSpecimen</a>
          </span>
          <span>
            <span> - powered by </span>
            <a href="https://www.krishagni.com" target="_blank" rel="noopener">Krishagni</a>
          </span>
        </div>
      </template>
    </os-dialog>
  </div>
</template>

<script>

export default {
  methods: {
    toggleHelpMenu: function(event) {
      this.$refs.helpMenu.toggle(event);
    },

    showAboutDialog: function() {
      this.$refs.aboutDialog.open();
    },
  }
}

</script>

<style scoped>

.help-options {
  margin: -1.25rem;
  list-style: none;
  padding: 0.5rem 0rem;
}

.help-options li a {
  display: inline-block;
  padding: 0.75rem 1rem;
  transition: box-shadow 0.15s;
  text-decoration: none;
  color: inherit;
  width: 100%;
}

.help-options li:not(.divider):hover {
  background: #e9ecef;
}

.help-options li.divider {
  padding: 0.25rem 0rem;
  margin: -1rem 0rem;
}

.help-footer {
  padding: 0.75rem 1rem;
  text-align: center;
}
</style>
