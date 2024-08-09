<template>
  <os-page>
    <os-page-head>
      <span>
        <h3 v-t="'common.home.home'">Home</h3>
      </span>

      <template #right>
        <os-button style="margin-top: -0.5rem;" left-icon="cog"
          :label="$t('common.home.widgets_button')" @click="showWidgetsDialog" />
      </template>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="9" style="overflow-y: auto;">
          <os-card class="os-widgets-info" v-if="ctx.widgets.length == 0">
            <template #body>
              <span class="message">
                <span class="question" v-t="'common.home.did_you_know'">Did you know you can configure the homepage to display the modules you frequently use?</span>
                <span class="help" v-t="'common.home.click_widgets_button'">Click on the "Widgets" button on the top right corner of this page</span>
              </span>
            </template>
          </os-card>
          <div class="os-widgets" v-else>
            <div :class="['widget', 'widget-' + (widget.width || 2)]" v-for="widget of widgets" :key="widget.name">
              <component :is="'os-home-' + widget.name" :widget="widget" />
            </div>
          </div>
        </os-grid-column>

        <os-grid-column :width="3" style="overflow-y: auto;">
          <div class="os-widgets">
            <div class="widget widget-6" v-if="ctx.favorites && ctx.favorites.length > 0">
              <os-home-list-card class="os-favorite-links" :icon="'heart'" :title="$t('common.home.favorites')"
                :show-star="false" :list="ctx.favorites" :hide-search="true">
                <template #actions="slotProps">
                  <os-button size="small" left-icon="trash" @click="confirmRemoveFavorite(slotProps.item)" />
                </template>
              </os-home-list-card>
            </div>

            <div class="widget widget-6" style="padding-bottom: 0px;" v-else>
              <os-card class="os-quick-links">
                <template #body>
                  <span v-t="'common.home.no_favorites'">No favorite links</span>
                </template>
              </os-card>
            </div>

            <div class="widget widget-6" style="padding-bottom: 0px;">
              <os-card class="os-quick-links">
                <template #header>
                  <span class="title" v-t="'common.home.useful_links'">Useful Links</span>
                </template>
                <template #body>
                  <ul>
                    <li>
                      <a href="https://openspecimen.atlassian.net/l/cp/ExVdshgT" target="_blank">
                        <span v-t="'common.home.user_manual'">User Manual</span>
                      </a>
                    </li>
                    <li>
                      <a href="https://forums.openspecimen.org" target="_blank">
                        <span v-t="'common.home.online_forums'">Online Forums</span>
                      </a>
                    </li>
                    <li>
                      <a href="https://www.youtube.com/channel/UCWRN3KN0G5k9WmTiQIwuA8g" target="_blank">
                        <span v-t="'common.home.youtube'">YouTube</span>
                      </a>
                    </li>
                    <li>
                      <a href="https://www.openspecimen.org/webinars/" target="_blank">
                        <span v-t="'common.home.past_webinars'">Past Webinars</span>
                      </a>
                    </li>
                  </ul>
                </template>
              </os-card>
            </div>
          </div>
        </os-grid-column>
      </os-grid>
    </os-page-body>

    <os-dialog ref="widgetsDialog">
      <template #header>
        <span v-t="'common.home.select_widgets'">Select Widgets</span>
      </template>
      <template #content>
        <table class="os-table">
          <thead class="os-table-head">
            <tr>
              <th>&nbsp;</th>
              <th v-t="'common.home.widget'">Widget</th>
              <th v-t="'common.home.widget_width'">Width</th>
            </tr>
          </thead>
            <draggable v-model="ctx.availableWidgets" tag="tbody" handle=".os-widget-row-grip" class="os-table-body">
              <tr class="row os-widget-row-grip" v-for="widget of ctx.availableWidgets" :key="widget.name">
                <td class="col os-widget-selector">
                  <os-boolean-checkbox v-model="widget.selected" />
                </td>
                <td class="col">
                  <span>{{widget.displayLabel}}</span>
                </td>
                <td class="col">
                  <os-dropdown v-model="widget.width" :md-type="true" :optional="false"
                    :list-source="{
                      'displayProp': 'value',
                      'selectProp': 'value',
                      'options': [{'value': 1}, {'value': 2}, {'value': 3}, {'value': 4}, {'value': 5}, {'value': 6}]
                    }"
                  />
                </td>
              </tr>
            </draggable>
        </table>
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="closeWidgetsDialog" />
        <os-button primary :label="$t('common.buttons.save')" @click="saveWidgets" />
      </template>
    </os-dialog>

    <os-confirm-delete ref="deleteFavoriteConfirm" :captcha="false" :collect-reason="false">
      <template #message>
        <span v-t="'common.home.rm_from_favorites'"></span>
      </template>
    </os-confirm-delete>
  </os-page>
</template>

<script>

import { VueDraggableNext } from "vue-draggable-next";

import homePageSvc from '@/common/services/HomePageService.js';
import i18n from '@/common/services/I18n.js';

export default {
  name: 'HomePage',

  components: {
    draggable: VueDraggableNext
  },

  data() {
    return {
      ctx: {
        widgets: [],

        favorites: []
      }
    }
  },

  created() {
    const {widgets} = this.$ui.global.state || {};
    this.ctx.widgets = widgets;
    this._loadFavorites();
    homePageSvc.registerFavoritesListener(() => this._loadFavorites());
  },

  watch: {
  },

  computed: {
    widgets: function() {
      const accessible = homePageSvc.getWidgets();
      return this.ctx.widgets.filter(({name}) => accessible.indexOf(name) != -1);
    }
  },

  methods: {
    showWidgetsDialog: function() {
      this.$refs.widgetsDialog.open();

      const selectedList = [];
      const allWidgets = homePageSvc.getAllWidgets()
        .map(name => ({name, displayLabel: i18n.msg('common.home.widgets.' + name), width: 2}));
      for (let selectedWidget of (this.ctx.widgets || [])) {
        for (let widget of allWidgets) {
          if (widget.name == selectedWidget.name) {
            widget.selected = true;
            widget.width = selectedWidget.width;
            selectedList.push(widget);
            allWidgets.splice(allWidgets.indexOf(widget), 1);
            break;
          }
        }
      }

      this.ctx.availableWidgets = selectedList.concat(allWidgets);
    },

    closeWidgetsDialog: function() {
      this.$refs.widgetsDialog.close();
    },

    saveWidgets: function() {
      const selectedWidgets = this.ctx.availableWidgets.filter(widget => widget.selected);
      const toSave = selectedWidgets.map(({name, width}) => ({name, width}));
      homePageSvc.saveUserWidgets(toSave).then(
        (state) => {
          this.$ui.global.state = state;
          this.ctx.widgets = state.widgets || [];
          this.closeWidgetsDialog();
        }
      );
    },

    confirmRemoveFavorite: function(favorite) {
      this.$refs.deleteFavoriteConfirm.open().then(
        async (resp) => {
          if (resp != 'proceed') {
            return;
          }

          homePageSvc.deleteFavorite(favorite.id);
        }
      );
    },

    _loadFavorites: function() {
      homePageSvc.getFavorites().then(
        favorites => {
          this.ctx.favorites = favorites.map(
            favorite => (
              {
                id: favorite.id,
                displayName: favorite.title,
                url: favorite.viewUrl,
                title: favorite.title
              }
            )
          );
        }
      );
    }
  }
}
</script>

<style scoped>
.os-widgets-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.os-widgets-info .message {
  padding: 2.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.os-widgets-info .message .question {
  font-size: 1.25rem;
  margin-bottom: 1rem;
}

.os-widgets-info .message .help {
  font-size: 1rem;
  font-style: italic;
}

.os-quick-links {
  min-width: 200px;
  margin-bottom: 1.25rem;
}

.os-quick-links ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.os-quick-links ul li {
  padding: 0.25rem 0;
}

.os-widgets {
  display: flex;
  flex-wrap: wrap;
  width: 100%;
}

.os-widgets .widget {
  flex-shrink: 0;
  padding-right: 1rem;
  padding-bottom: 1rem;
  min-width: 350px;
}

.os-widgets .widget :deep(.os-card .body) {
  height: 340px;
  overflow-y: auto;
}

.os-widgets .widget .os-quick-links :deep(.body) {
  height: auto;
}

.os-widgets .widget .os-favorite-links :deep(.body) {
  height: auto;
  max-height: 340px;
}

.os-favorite-links :deep(.title .os-icon-wrapper) {
  color: orangered;
}

.os-favorite-links :deep(tr button.btn.btn-xs) {
  display: none;
  height: 1rem;
  width: 1rem;
  font-size: 0.75rem;
  padding: 0;
  border: 0;
}

.os-favorite-links :deep(tr:hover button.btn.btn-xs) {
  display: inline-block;
}

.os-widgets .widget-1 {
  width: 16.66%;
}

.os-widgets .widget-2 {
  width: 33.33%;
}

.os-widgets .widget-3 {
  width: 50%;
}

.os-widgets .widget-4 {
  width: 66.66%;
}

.os-widgets .widget-5 {
  width: 83.33%;
}

.os-widgets .widget-6 {
  width: 100%;
}

td.os-widget-selector {
  width: 50px;
}

.os-widget-row-grip {
  position: relative;
}

.os-widget-row-grip:after {
  content: '::::';
  position: absolute;
  left: 50%;
  top: 0;
  cursor: grab;
  font-weight: 700;
  display: none;
}

.os-widget-row-grip:hover:after {
  display: initial;
}
</style>
