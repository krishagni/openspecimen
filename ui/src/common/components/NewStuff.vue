
<template>
  <div class="os-new-stuff">
    <os-button :label="$t('common.new_stuff.whats_new_q')" @click="toggleOverlay($event)"
      v-if="uiState.notesRead != $ui.global.appProps.build_commit_revision" />

    <os-overlay ref="overlay" :dismissable="false" @hide="overlayClosed">
      <div class="os-new-stuff-overlay">
        <div class="header">
          <span v-t="'common.new_stuff.announcements'">Announcements</span>
        </div>
        <div class="content">
          <span v-t="{path: 'common.new_stuff.congrats', args: $ui.global.appProps}"></span>
          <p>
            <a :href="releaseNotesLink" target="_blank" rel="noopener"><span v-t="'common.new_stuff.click_here'">Click here</span></a> <span v-t="'common.new_stuff.read_more'">to read more about the new features and enhancements in this release.</span>
          </p>
        </div>
      </div>
    </os-overlay>
  </div>
</template>

<script>

import commonSvc   from '@/common/services/Common.js';
import settingsSvc from '@/common/services/Setting.js';

export default {
  data() {
    return {
      uiState: this.$ui.global.state,

      releaseNotesLink: undefined
    }
  },

  methods: {
    toggleOverlay: async function(event) {
      let tgt = event.currentTarget;

      if (this.releaseNotesLink == undefined) {
        const link    = await settingsSvc.getSetting('training', 'release_notes');
        this.releaseNotesLink = link[0].value;
      }

      this.$refs.overlay.toggle({currentTarget: tgt});
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
  border-radius: 0.25rem;
  cursor: pointer;
}

.os-new-stuff-overlay {
  max-width: 25rem;
  margin-bottom: -1.25rem;
}

.os-new-stuff-overlay .header {
  border-bottom: 1px solid #ddd;
  padding-bottom: 0.5rem;
  font-weight: bold;
}

.os-new-stuff-overlay .content {
  padding: 0.5rem 0rem;
}
</style>
