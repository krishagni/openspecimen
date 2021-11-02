
<template>
  <div class="os-navbar">
    <div class="items">
      <div class="logo">
        <a href="http://www.openspecimen.org" target="_blank" relopener="noopener">
          <img :src="osLogo">
        </a>
        <a class="deploy-logo" :href="deploySiteUrl" v-if="deploySiteLogo" target="_blank" rel="noopener">
          <img :src="deploySiteLogo">
        </a>
        <div class="deploy-env">
          <span>{{deployEnv}}</span>
        </div>
      </div>
      <div class="search">
        <span> </span>
      </div>
      <div class="buttons">
        <div class="new-stuff">
          <os-button label="What's New?" @click="toggleNewStuffOverlay"
            v-if="uiState.notesRead != $ui.global.appProps.build_commit_revision" />

          <os-overlay ref="newStuffOverlay" :dismissable="false" @hide="newStuffOverlayClosed">
            <div class="new-stuff-container">
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

        <div class="feedback" v-if="$ui.global.appProps.feedback_enabled">
          <button @click="showFeedbackForm">
            <os-icon name="bullhorn" />
          </button>

          <os-dialog ref="feedbackDialog">
            <template #header>
              <span>Your feedback counts!</span>
            </template>
            <template #content>
              <os-message type="info">
                <span>Please let us know what you think of OpenSpecimen. </span>
                <span>Your feedback to improve the product is most welcome.</span>
              </os-message>

              <os-form ref="feedbackForm" :schema="feedbackFormSchema" @input="handleFeedbackChange($event)">
                <div>
                  <os-button label="Submit" @click="submitFeedback()" />
                  <os-button label="Cancel" @click="cancelFeedback()" />
                </div>
              </os-form>
            </template>
          </os-dialog>
        </div>

        <div class="help">
          <button @click="toggleHelpMenu">
            <os-icon name="question-circle" />
          </button>

          <os-overlay ref="helpMenu" @click="toggleHelpMenu">
            <ul class="help-options">
              <li>
                <a href="https://help.openspecimen.org" target="_blank" rel="noopener">
                  <span>Online Help</span>
                </a>
              </li>
              <li>
                <a href="https://forums.openspecimen.org" target="_blank" rel="noopener">
                  <span>Q & A Forums</span>
                </a>
              </li>
              <li class="divider">
                <os-divider />
              </li>
              <li>
                <a @click="showOpenSpecimenInfo()">
                  <span>About OpenSpecimen</span>
                </a>
              </li>
            </ul>
          </os-overlay>

          <os-dialog ref="aboutDialog">
            <template #header>
              <span>About OpenSpecimen</span>
            </template>
            <template #content>
              <os-tabs>
                <os-tab>
                  <template #header>
                    <os-icon-title icon="eye" title="Overview" />
                  </template>

                  <ul class="os-key-values">
                    <li class="item">
                      <strong class="key key-sm">Version</strong>
                      <span class="value">{{$ui.global.appProps.build_version}}</span>
                    </li>
                    <li class="item">
                      <strong class="key key-sm">Build Date</strong>
                      <span class="value">{{$filters.date(+$ui.global.appProps.build_date)}}</span>
                    </li>
                    <li class="item">
                      <strong class="key key-sm">Revision</strong>
                      <span class="value">{{$ui.global.appProps.build_commit_revision}}</span>
                    </li>
                  </ul>
                </os-tab>

                <os-tab>
                  <template #header>
                    <os-icon-title icon="list" title="Plugins" />
                  </template>

                  <table class="os-table">
                    <thead>
                      <tr>
                        <th>Plugin</th>
                        <th>Version</th>
                        <th>Build Date</th>
                        <th>Revision</th>
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

        <div class="notifs">
          <button @click="toggleNotifOverlay">
            <os-icon name="bell" v-os-badge.danger="unreadNotifCount" v-if="unreadNotifCount > 0" />
            <os-icon name="bell" v-else />
          </button>

          <os-overlay class="os-notifs-overlay" ref="notifsOverlay"
            @click="toggleNotifOverlay" @show="notifsDisplayed" @hide="notifsHidden">
            <div class="content">
              <os-user-notifs />
            </div>
          </os-overlay>
        </div>

        <div class="user-profile">
          <button @click="toggleProfileMenu">
            <os-username-avatar :name="username" />
          </button>

          <os-overlay ref="userProfileMenu" @click="toggleProfileMenu">
            <ul class="user-profile-options">
              <li>
                <router-link :to="{name: 'UserOverview', params: {userId: $ui.currentUser.id}}">
                  <span>{{username}}</span>
                </router-link>
              </li>
              <li class="divider">
                <os-divider />
              </li>
              <li>
                <a v-if="ssoLogout" href="saml/logout">Log Out</a>
                <a v-else :href="$ui.ngServer + '#/?logout=true'">Log Out</a>
              </li>
            </ul>
          </os-overlay>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import osLogo from '@/assets/images/os_logo.png';
import alertsSvc from '@/common/services/Alerts.js';
import http from '@/common/services/HttpClient.js';
import settingsSvc from '@/common/services/Setting.js';
import notifSvc from '@/common/services/Notif.js';
import userSvc from '@/administrative/services/User.js';

import NotificationsList from '@/common/components/NotificationsList';

export default {
  components: {
    'os-user-notifs': NotificationsList
  },

  data() {
    return {
      osLogo: osLogo,

      ssoLogout: false,

      unreadNotifCount: 0,

      uiState: this.$ui.global.state,

      releaseNotes: undefined,

      releaseNotesLink: undefined,

      feedback: {},

      feedbackFormSchema: {
        "rows": [
          {
            "fields": [
              {
                "type": "text",
                "label": "Subject",
                "name": "subject",
                "validations": {
                  "required": {
                    "message": "Subject is mandatory"
                  }
                }
              }
            ]
          },
          {
            "fields": [
              {
                "type": "textarea",
                "label": "Feedback",
                "name": "feedback",
                "rows": 5,
                "validations": {
                  "required": {
                    "message": "Feedback is mandatory"
                  }
                }
              }
            ]
          }
        ]
      }
    }
  },

  mounted() {
    let samlEnabled = settingsSvc.getSetting('auth', 'saml_enable');
    let sloEnabled  = settingsSvc.getSetting('auth', 'single_logout');
    Promise.all([samlEnabled, sloEnabled]).then(
      (resp) => {
        this.ssoLogout = (resp[0][0].value == true || resp[0][0].value == 'true') &&
          (resp[1][0].value == true || resp[1][0].value == 'true');
      }
    );

    settingsSvc.getSetting('training', 'release_notes').then(setting => this.releaseNotesLink = setting[0].value);

    let self = this;
    let timeout;
    function getUnreadNotifCount() {
      if (timeout) {
        clearTimeout(timeout);
      }

      if (self.notifsOpen) {
        timeout = setTimeout(getUnreadNotifCount, 10000);
        return;
      }

      notifSvc.getUnreadCount().then(
        (result) => {
          self.unreadNotifCount = result.count;
          timeout = setTimeout(getUnreadNotifCount, 10000);
        }
      );
    }

    getUnreadNotifCount();
  },

  computed: {
    siteAssets: function() {
      return (this.$ui && this.$ui.global && this.$ui.global.siteAssets) || {};
    },

    deployEnv: function() {
      if (this.$ui && this.$ui.global && this.$ui.global.appProps.deploy_env) {
        return this.$ui.global.appProps.deploy_env.toUpperCase();
      }
      return 'UNKNOWN';
    },

    deploySiteUrl: function() {
      return this.siteAssets.siteUrl || '';
    },

    deploySiteLogo: function() {
      let logoUrl = this.siteAssets.siteLogo || '';
      if (logoUrl && logoUrl.length > 9) {
        logoUrl = http.getUrl(logoUrl.substring(9));
      }

      return logoUrl;
    },

    username: function() {
      return this.$filters.username(this.$ui.currentUser);
    }
  },

  methods: {
    toggleNewStuffOverlay: async function(event) {
      this.$refs.newStuffOverlay.toggle(event);
      if (this.releaseNotes == undefined) {
        let summary = await http.get('release-notes/latest-summary');
        this.releaseNotes = summary.notes;
      }
    },

    newStuffOverlayClosed: function() {
      userSvc.saveUiState({notesRead: this.$ui.global.appProps.build_commit_revision}).then(
        uiState => this.uiState = this.$ui.global.state = uiState
      );
    },

    showFeedbackForm: function() {
      this.feedback = {};
      this.$refs.feedbackDialog.open();
    },

    handleFeedbackChange: function({data}) {
      Object.assign(this.feedback, data);
    },

    submitFeedback: function() {
      if (!this.$refs.feedbackForm.validate()) {
        return;
      }

      http.post('support/user-feedback', this.feedback).then(
        (resp) => {
          if (resp == false || resp == 'false') {
            alertsSvc.error('Failed to send the feedback. Contact system administrator for further help!');
          } else {
            alertsSvc.success('Feedback sent successfully!');
            this.$refs.feedbackDialog.close();
          }
        }
      );
    },

    cancelFeedback: function() {
      this.$refs.feedbackDialog.close();
    },

    toggleHelpMenu: function(event) {
      this.$refs.helpMenu.toggle(event);
    },

    showOpenSpecimenInfo: function() {
      this.$refs.aboutDialog.open();
    },

    toggleProfileMenu: function(event) {
      this.$refs.userProfileMenu.toggle(event);
    },

    toggleNotifOverlay: function(event) {
      this.$refs.notifsOverlay.toggle(event);
    },

    notifsDisplayed: function() {
      this.notifsOpen = true;
      if (this.unreadNotifCount > 0) {
        this.notifOpenTime = new Date();
      }
    },

    notifsHidden: function() {
      this.notifsOpen = false;
      if (this.notifOpenTime) {
        notifSvc.markAsRead(this.notifOpenTime).then(() => this.unreadNotifCount = 0);
        this.notifOpenTime = null;
      }
    }
  }
}
</script>

<style scoped>

.os-navbar {
  display: block;
  height: 40px;
  width: 100%;
  background: #205081;
  border-bottom: 1px solid #2e3d54;
  color: #fff;
  font-weight: bold;
}

.os-navbar .items {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: row;
}

.os-navbar .items .logo {
  display: flex;
  flex: 1;
  flex-direction: row;
  padding: 0.15rem;
}

.os-navbar .items .logo img {
  height: 2rem;
}

.os-navbar .items .logo .deploy-logo {
  margin-left: 0.4rem;
}

.os-navbar .items .logo .deploy-env {
  font-size: 0.6rem;
  color: #fff;
  background-color: #c33;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  display: inline-block;
  height: 1.125rem;
  margin-left: 0.4rem;
  margin-top: 0.2rem;
}

.os-navbar .items .search {
  display: flex;
  flex: 2;
}

.os-navbar .items .buttons {
  display: flex;
  flex: 1;
  justify-content: right;
  padding: 0.2rem 1rem;
}

.buttons button {
  background: transparent;
  border: none;
  color: #fff;
  font-weight: bold;
  font-size: 1.2rem;
  padding: 0.5rem 1.25rem 0rem 1.25rem;
  cursor: pointer;
}

.new-stuff button {
  padding: 0.25rem;
  font-size: 0.8rem;
  width: 7rem;
  background: #5cb85c;
  margin-top: 0.25rem;
  border-radius: 0.25rem;
}

.new-stuff-container .header {
  border-bottom: 1px solid #ddd;
  padding-bottom: 0.5rem;
  font-weight: bold;
}

.new-stuff-container .footer {
  border-top: 1px solid #ddd;
  padding-top: 0.5rem;
}

.help-footer {
  padding: 0.75rem 1rem;
  text-align: center;
}

.user-profile {
  display: inline-block;
}

.user-profile button {
  padding-top: 0rem;
}

.help-options,
.user-profile-options {
  margin: -1.25rem;
  list-style: none;
  padding: 0.5rem 0rem;
}

.help-options li a,
.user-profile-options li a {
  display: inline-block;
  padding: 0.75rem 1rem;
  transition: box-shadow 0.15s;
  text-decoration: none;
  color: inherit;
  width: 100%;
}

.help-options li:not(.divider):hover,
.user-profile-options li:not(.divider):hover {
  background: #e9ecef;
}

.help-options li.divider,
.user-profile-options li.divider {
  padding: 0.25rem 0rem;
  margin: -1rem 0rem;
}

</style>

<style>
.os-notifs-overlay {
  max-height: calc(100% - 100px);
  width: 35%;
  overflow: auto;
}
</style>
