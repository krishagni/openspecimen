<template>
  <PageToolbar>
    <template #default>
      <Button left-icon="edit" label="Edit" @click="$goto('SiteAddEdit', {siteId: ctx.site.id})" />

      <Button left-icon="trash" label="Delete" @click="deleteSite" />
    </template>
  </PageToolbar>

  <Grid>
    <GridColumn width="8">
      <Overview :schema="siteSchema.fields" :object="ctx"></Overview>
    </GridColumn>

    <GridColumn width="4">
      <AuditOverview :objects="ctx.siteObjs" v-if="ctx.site.id"></AuditOverview>
    </GridColumn>
  </Grid>

  <DeleteObject ref="deleteObj" :input="ctx.deleteOpts" />
</template>

<script>
import { reactive, watchEffect } from 'vue';

import PageToolbar from '@/common/components/PageToolbar.vue';
import Grid from '@/common/components/Grid.vue';
import GridColumn from '@/common/components/GridColumn.vue';
import Overview from '@/common/components/Overview.vue';
import Button from '@/common/components/Button.vue';
import AuditOverview from '@/common/components/AuditOverview.vue';
import DeleteObject from '@/common/components/DeleteObject.vue';

import siteSchema from '@/administrative/sites/schemas/site.js';

import routerSvc from '@/common/services/Router.js';
import siteSvc from '@/administrative/services/Site.js';

export default {
  props: ['site'],

  inject: ['ui'],

  components: {
    PageToolbar,
    Overview,
    Button,
    AuditOverview,
    Grid,
    GridColumn,
    DeleteObject
  },

  setup(props) {
    let ctx = reactive({
      site: {},

      deleteOpts: {},

      siteObjs: []
    });

    
    watchEffect(
      () => {
        ctx.site = props.site;
        ctx.deleteOpts = {
          type: 'Site',
          title: props.site.name,
          dependents: () => siteSvc.getDependents(props.site),
          deleteObj: () => siteSvc.delete(props.site)
        };
        ctx.siteObjs = [{objectName: 'site', objectId: props.site.id}];
      }
    );

    return { ctx, siteSchema };
  },

  computed: {
  },

  methods: {
    deleteSite: function() {
      this.$refs.deleteObj.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('SitesList');
          }
        }
      );
    }
  }
}
</script>
