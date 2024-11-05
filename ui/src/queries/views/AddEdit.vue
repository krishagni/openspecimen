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
          <span>expressions and filters</span>
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['query'],

  data() {
    return {
      ctx: {
        bcrumb: [
          {url: routerSvc.getUrl('QueriesList', {}), label: i18n.msg('queries.list')}
        ]
      }
    };
  }
}
</script>
