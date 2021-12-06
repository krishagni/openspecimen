<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>{{ctx.institute.name}}</h3>
      </span>
    </os-page-head>
    <os-page-body>
      <os-side-menu>
        <ul>
          <li v-os-tooltip.right="'Overview'">
            <router-link :to="{name: 'InstituteOverview'}">
              <os-icon name="eye" />
            </router-link>
          </li>

          <os-plugin-views page="institute-detail" view="side-menu" />
        </ul>
      </os-side-menu>

      <router-view :institute="ctx.institute" v-if="ctx.institute.id" />
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, watchEffect } from 'vue';

import routerSvc    from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  props: ['instituteId'],

  setup(props) {
    let ctx = reactive({
      institute: {},

      bcrumb: [
        {url: routerSvc.getUrl('InstitutesList'), label: 'Institutes'}
      ]
    });

    watchEffect(
      () => {
        instituteSvc.getInstitute(+props.instituteId).then(institute => ctx.institute = institute);
      }
    );

    return { ctx };
  }
}
</script>
