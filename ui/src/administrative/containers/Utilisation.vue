<template>
  <os-page-toolbar>
    <template #default>
      <os-button-link left-icon="map" :label="$t('containers.view_utilisation_heatmap')" :url="utilisationMapUrl"
        v-if="isSpecimensCountView" />

      <span v-else>
        <os-button-link left-icon="chart-pie" :label="$t('containers.specimens_by_type')"
          :url="specimensCountUrl" />

        <os-container-utilisation-legend v-if="!isDimensionless && hasChildContainers" />

        <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportUtilisationMap"
          v-if="!isDimensionless && hasChildContainers" />
      </span>

    </template>
  </os-page-toolbar>

  <div class="os-container-utilisation">
    <div class="specimens-count-view" v-if="isSpecimensCountView">
      <div class="specimens-count-chart" v-if="ctx.spmnCountsLoaded && ctx.storedSpmns > 0">
        <div class="chart-body">
          <os-chart type="doughnut" :data="ctx.spmnTypesStorage" :options="ctx.spmnTypesChartOptions"
            :plugins="ctx.spmnTypesChartPlugins" />
        </div>

        <div class="chart-title" v-t="'containers.top_5_specimens_by_type'">Top 5 Specimens by Type</div>
      </div>

      <os-message type="info" v-else-if="ctx.spmnCountsLoaded">
        <span v-t="'containers.no_specimens_by_type'">No specimens have been stored in this container.</span>
      </os-message>
    </div>

    <os-container-layout class="map" :container="ctx.container" :occupants="ctx.occupants"
      v-else-if="!isDimensionless && hasChildContainers">
      <template #occupant_container="slotProps">
        <os-container-utilisation-tile :container="slotProps.occupant" :fill="true" @click.stop />
      </template>

      <template #empty>
        <span />
      </template>
    </os-container-layout>

    <div v-else-if="isDimensionless">
      <os-message type="info">
        <span v-t="'containers.no_utilisation_map_for_dimless'">Utilisation map is not available for dimensionless containers.</span>
      </os-message>
    </div>

    <div v-else-if="ctx.occupantsLoaded">
      <os-message type="info">
        <span v-t="'containers.no_child_containers_for_utilisation'">This container does not store any child containers.</span>
      </os-message>
    </div>
  </div>
</template>

<script>
import { reactive } from 'vue';

import boxUtil      from '@/common/services/BoxUtil.js';
import containerSvc from '@/administrative/services/Container.js';
import alertsSvc    from '@/common/services/Alerts.js';
import routerSvc    from '@/common/services/Router.js';

const specimenPctPlugin = {
  id: 'specimenPctPlugin',

  afterDatasetsDraw: function(chart) {
    const dataset = chart.data.datasets[0] || {};
    const data = dataset.data || [];
    const total = dataset.totalSpecimens || data.reduce((sum, count) => sum + count, 0);
    if (total <= 0) {
      return;
    }

    const {ctx} = chart;
    const meta = chart.getDatasetMeta(0);
    ctx.save();
    ctx.font = '600 12px sans-serif';
    ctx.fillStyle = '#fff';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.shadowColor = 'rgba(0, 0, 0, 0.45)';
    ctx.shadowBlur = 3;

    meta.data.forEach((arc, idx) => {
      const value = data[idx];
      if (value <= 0) {
        return;
      }

      const {x, y} = arc.tooltipPosition();
      const pct = Math.round(value * 100 / total);
      ctx.fillText(pct + '%', x, y);
    });

    ctx.restore();
  }
};

export default {
  props: ['container', 'view'],

  setup() {
    const ctx = reactive({
      container: {},

      occupants: [],

      occupantsLoaded: false,

      spmnTypesStorage: {},

      spmnTypesChartOptions: {
        responsive: true,

        maintainAspectRatio: false,

        plugins: {
          legend: {
            position: 'right'
          }
        }
      },

      spmnTypesChartPlugins: [specimenPctPlugin],

      storedSpmns: null,

      spmnCountsLoaded: false
    });

    return { ctx };
  },

  created() {
    this._setupView();
  },

  watch: {
    'container.id': function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupView();
      }
    },

    view: function(newVal, oldVal) {
      if (newVal != oldVal && this.ctx.container.id) {
        this._loadCurrentView();
      }
    }
  },

  computed: {
    isDimensionless: function() {
      return !(this.ctx.container.noOfRows > 0 && this.ctx.container.noOfColumns > 0);
    },

    hasChildContainers: function() {
      return this.ctx.occupants.length > 0;
    },

    isSpecimensCountView: function() {
      return this._getView() == 'specimensCount';
    },

    utilisationMapUrl: function() {
      return this._viewUrl('heatmap');
    },

    specimensCountUrl: function() {
      return this._viewUrl('specimensCount');
    }
  },

  methods: {
    showUtilisationMap: async function(updateRoute) {
      if (updateRoute !== false) {
        this._updateViewInRoute('heatmap');
        return;
      }

      if (!this.isDimensionless) {
        await this._loadUtilisationMap();
      }
    },

    showSpecimensCount: async function(updateRoute) {
      if (updateRoute !== false) {
        this._updateViewInRoute('specimensCount');
        return;
      }

      if (this.ctx.spmnCountsLoaded) {
        return;
      }

      this.ctx.spmnTypesStorage = {};
      this.ctx.storedSpmns = null;
      this.ctx.spmnCountsLoaded = false;

      const containerId = this.ctx.container.id;
      const specimensByType = await containerSvc.getSpecimensCountByType(this.ctx.container);
      // Ignore stale responses if the user selected another container or switched views before the API returned.
      if (containerId != this.ctx.container.id || !this.isSpecimensCountView) {
        return;
      }

      const types = Object.keys(specimensByType || {})
        .sort((t1, t2) => specimensByType[t2] - specimensByType[t1]);

      let storedSpmns = 0, spmnTypes = [], spmnCounts = [];
      for (let type of types) {
        storedSpmns += specimensByType[type];
        if (spmnTypes.length < 5) {
          spmnTypes.push(type);
          spmnCounts.push(specimensByType[type]);
        }
      }

      this.ctx.spmnTypesStorage = {
        labels: spmnTypes,
        datasets: [
          {
            data: spmnCounts,
            totalSpecimens: storedSpmns,
            backgroundColor: containerSvc.getSpecimenTypeColors(spmnTypes)
          }
        ]
      };

      this.ctx.storedSpmns = storedSpmns;
      this.ctx.spmnCountsLoaded = true;
    },

    exportUtilisationMap: async function() {
      alertsSvc.info({code: 'containers.generating_utilisation_map_report'});
      const resp = await containerSvc.exportUtilisationMap(this.ctx.container);
      if (resp.fileId) {
        alertsSvc.info({code: 'containers.downloading_utilisation_map_report'});
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info({code: 'containers.utilisation_map_report_by_email'});
      }
    },

    _setupView: async function() {
      this.ctx.container = this.container;
      this.ctx.occupants = [];
      this.ctx.occupantsLoaded = false;
      this.ctx.spmnTypesStorage = {};
      this.ctx.storedSpmns = null;
      this.ctx.spmnCountsLoaded = false;

      await this._loadCurrentView();
    },

    _getView: function() {
      return this.view == 'specimensCount' ? 'specimensCount' : 'utilisation';
    },

    _loadCurrentView: async function() {
      if (this.isSpecimensCountView) {
        await this.showSpecimensCount(false);
        return;
      }

      await this.showUtilisationMap(false);
    },

    _loadUtilisationMap: async function() {
      if (this.ctx.occupantsLoaded || this.isDimensionless) {
        return;
      }

      const containerId = this.ctx.container.id;
      const occupants = await containerSvc.getUtilisationMap(this.ctx.container);
      // Ignore stale responses if the user selected another container or switched views before the API returned.
      if (containerId != this.ctx.container.id || this.isSpecimensCountView) {
        return;
      }

      this.ctx.occupants = (occupants || []).map(occupant => this._toOccupant(occupant));
      this.ctx.occupantsLoaded = true;
    },

    _toOccupant: function(occupant) {
      const location = occupant.storageLocation || {};
      const assigner = boxUtil.getPositionAssigner(this.ctx.container.positionAssignment);
      const {row, column} = assigner.fromPos({
        pos: location.position,
        nr: this.ctx.container.noOfRows,
        nc: this.ctx.container.noOfColumns
      });
      return {
        ...occupant,
        emptySlot: !occupant.id,
        occuypingEntity: 'container',
        occupyingEntityId: occupant.id,
        occupyingEntityName: occupant.name,
        posTwo: location.positionY,
        posOne: location.positionX,
        posTwoOrdinal: row,
        posOneOrdinal: column
      };
    },

    _updateViewInRoute: function(view) {
      const {name, params, query} = this.$route || {};
      this.$router.replace({name, params, query: {...query, view}});
    },

    _viewUrl: function(view) {
      const {name, params, query} = this.$route || {};
      return routerSvc.getUrl(name, params, {...query, view});
    }
  }
}
</script>

<style scoped>
.os-container-utilisation {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.os-container-utilisation .map {
  flex: 1;
}

.os-container-utilisation .specimens-count-view {
  width: min(100%, 40rem);
  height: min(30rem, calc(100vh - 14rem));
}

.os-container-utilisation .specimens-count-chart {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.os-container-utilisation .chart-body {
  flex: 1;
  min-height: 0;
}

.os-container-utilisation .chart-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #333;
  text-align: center;
  margin-top: 0.75rem;
}

.os-container-utilisation .chart-body :deep(.os-chart) {
  height: 100%;
}

.os-container-utilisation .map :deep(td.occupied) {
  background: transparent;
}

.os-container-utilisation .map :deep(td.occupied .coord) {
  color: #fff;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45);
}

.os-container-utilisation .map :deep(.occupant-wrapper) {
  position: static;
  padding: 0;
}

</style>
