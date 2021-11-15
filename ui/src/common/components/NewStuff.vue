
<template>
  <div class="os-new-stuff">
    <os-button label="What's New?" @click="toggleOverlay"
      v-if="uiState.notesRead != $ui.global.appProps.build_commit_revision" />

    <os-overlay ref="overlay" :dismissable="false" @hide="overlayClosed">
      <div class="os-new-stuff-overlay">
        <div class="header">
          <span>Announcements</span>
        </div>
        <div class="content">
          <span v-html="releaseNotes"></span>
        </div>
        <div class="footer">
          <a :href="releaseNotesLink" target="_blank" rel="noopener">Read more...</a>
        </div>
      </div>
    </os-overlay>
  </div>
</template>

<script>

import commonSvc   from '@/common/services/Common.js';
import http        from '@/common/services/HttpClient.js';
import settingsSvc from '@/common/services/Setting.js';

export default {
  data() {
    return {
      uiState: this.$ui.global.state,

      releaseNotes: undefined,

      releaseNotesLink: undefined
    }
  },

  methods: {
    toggleOverlay: async function() {
      this.$refs.overlay.toggle(event);
      if (this.releaseNotes == undefined) {
        let summary = await http.get('release-notes/latest-summary');
        let linkSetting = await settingsSvc.getSetting('training', 'release_notes');

        this.releaseNotes = summary.notes;
        this.releaseNotesLink = linkSetting[0].value;
      }
    },

    overlayClosed: async function() {
      let uiState = await commonSvc.saveUiState({notesRead: this.$ui.global.appProps.build_commit_revision});
      this.uiState = this.$ui.global.state = uiState;
    }
  }
}
</script>

<style scoped>

.os-new-stuff .btn {
  background: #5cb85c;
  border: none;
  color: #fff;
  padding: 0.25rem;
  font-weight: bold;
  font-size: 0.8rem;
  width: 7rem;
  margin-top: 0.25rem;
  border-radius: 0.25rem;
  cursor: pointer;
}

.os-new-stuff-overlay .header {
  border-bottom: 1px solid #ddd;
  padding-bottom: 0.5rem;
  font-weight: bold;
}

.os-new-stuff-overlay .footer {
  border-top: 1px solid #ddd;
  padding-top: 0.5rem;
}

</style>
