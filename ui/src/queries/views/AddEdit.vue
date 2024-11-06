<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>
          <span v-if="query.title">{{query.title}}</span>
          <span v-else v-t="'queries.unsaved_query'">Unsaved Query</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <span>actions</span>
        </template>
      </os-page-toolbar>

      <os-grid>
        <os-grid-column :width="3">
          <span>forms and filters</span>
        </os-grid-column>

        <os-grid-column :width="9">
          <os-panel class="expressions_n_filters">
            <template #header>
              <span v-t="'queries.expressions_n_filters'">Expressions and Filters</span>
            </template>
            <template #default>
              <div class="body">
                <ExpressionEditor :query="ctx.query" v-model="ctx.query.queryExpression" />

                <div style="flex: 1; overflow-y: auto; border: 1px solid red; margin: 1rem 0rem;">
                  <pre>{{query}}</pre>
                </div>
              </div>
            </template>
          </os-panel>
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';

import ExpressionEditor from '@/queries/views/ExpressionEditor.vue';

export default {
  props: ['query'],

  components: {
    ExpressionEditor
  },

  data() {
    return {
      ctx: {
        bcrumb: [
          {url: routerSvc.getUrl('QueriesList', {}), label: i18n.msg('queries.list')}
        ]
      }
    }
  },

  created() {
    this.ctx.query = this.query;
  }
}
</script>

<style scoped>
.expressions_n_filters {
  height: 100%;
}

.expressions_n_filters .body {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
