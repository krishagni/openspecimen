<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="map" :label="$t('containers.view_utilisation_heatmap')" @click="showUtilisationMap"
        v-if="isSpecimensCountView" />

      <span v-else>
        <os-button left-icon="chart-pie" :label="$t('containers.specimens_by_type')" @click="showSpecimensCount" />

        <os-button left-icon="palette" :label="$t('containers.view_utilisation_legend')" @click="toggleLegend"
          v-if="!isDimensionless && hasChildContainers" />

        <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportUtilisationMap"
          v-if="!isDimensionless && hasChildContainers" />
      </span>

      <os-overlay ref="legendOverlay">
        <ul class="os-utilisation-legend">
          <li class="legend-item" v-for="item of legend" :key="item.key">
            <span :class="['legend-swatch', item.key]" />
            <span class="legend-label">{{item.range}}, {{item.label}}</span>
          </li>
        </ul>
      </os-overlay>
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

    <Layout class="map" :container="ctx.container" :occupants="ctx.occupants" v-else-if="!isDimensionless && hasChildContainers">
      <template #occupant_container="slotProps">
        <a :class="['utilisation-cell', getUtilisationClass(slotProps.occupant)]"
          v-os-tooltip="getTooltip(slotProps.occupant)"
          @click.prevent="showUtilisation(slotProps.occupant)">
          <span class="container-name">{{getDisplayName(slotProps.occupant)}}</span>
          <span class="utilisation-value">{{slotProps.occupant.utilisation}}%</span>
          <span class="utilisation-counts">
            {{getOccupiedSlots(slotProps.occupant)}} / {{slotProps.occupant.totalSlots}}
          </span>
        </a>
      </template>

      <template #empty>
        <span />
      </template>
    </Layout>

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

import Layout from './Layout.vue';

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

  components: {
    Layout
  },

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

    legend: function() {
      return [
        {key: 'full',   range: '100%',  label: this.$t('containers.utilisation_full')},
        {key: 'high',   range: '> 80%', label: this.$t('containers.utilisation_high_label')},
        {key: 'medium', range: '51-80%', label: this.$t('containers.utilisation_medium_label')},
        {key: 'low',    range: '1-50%', label: this.$t('containers.utilisation_low_label')},
        {key: 'empty',  range: '0%',    label: this.$t('containers.utilisation_empty_label')}
      ];
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

    toggleLegend: function(event) {
      this.$refs.legendOverlay.toggle(event);
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

    getUtilisationClass: function(occupant) {
      const utilisation = occupant.utilisation || 0;
      if (utilisation >= 100) {
        return 'full';
      } else if (utilisation > 80) {
        return 'high';
      } else if (utilisation > 50) {
        return 'medium';
      } else if (utilisation > 0) {
        return 'low';
      }

      return 'empty';
    },

    getTooltip: function(occupant) {
      return this.$t(
        'containers.utilisation_cell_tooltip',
        {
          name: this.getDisplayName(occupant),
          utilisation: occupant.utilisation,
          occupied: this.getOccupiedSlots(occupant),
          total: occupant.totalSlots
        }
      );
    },

    showUtilisation: function(occupant) {
      const containerId = occupant.id;
      if (containerId) {
        routerSvc.goto('ContainerDetail.Utilisation', {containerId});
      }
    },

    getDisplayName: function(occupant) {
      if (occupant.emptySlot) {
        return this.$t('containers.empty_container_slot', {type: occupant.typeName});
      }

      return occupant.displayName || occupant.occupyingEntityName || occupant.name;
    },

    getOccupiedSlots: function(occupant) {
      return (occupant.totalSlots || 0) - (occupant.freeSlots || 0);
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
      const totalSlots = occupant.totalPositions || 0;
      const usedSlots  = occupant.usedPositions || 0;
      return {
        ...occupant,
        emptySlot: !occupant.id,
        occuypingEntity: 'container',
        occupyingEntityId: occupant.id,
        occupyingEntityName: occupant.name,
        posTwo: location.positionY,
        posOne: location.positionX,
        posTwoOrdinal: row,
        posOneOrdinal: column,
        freeSlots: occupant.freePositions || 0,
        totalSlots: totalSlots,
        utilisation: totalSlots > 0 ? Math.round(usedSlots * 100 / totalSlots) : 0
      };
    },

    _updateViewInRoute: function(view) {
      const {name, params, query} = this.$route || {};
      this.$router.replace({name, params, query: {...query, view}});
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

.os-utilisation-legend {
  margin: -0.75rem -1rem;
  padding: 0.5rem 0;
  min-width: 12rem;
  list-style: none;
}

.os-utilisation-legend .legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  white-space: nowrap;
}

.os-utilisation-legend .legend-swatch {
  display: inline-block;
  width: 0.9rem;
  height: 0.9rem;
  border-radius: 2px;
  border: 1px solid rgba(0, 0, 0, 0.16);
}

.os-utilisation-legend .legend-label {
  font-size: 0.875rem;
}

.utilisation-cell {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  position: absolute;
  inset: 2px;
  padding: 1.5rem 0.45rem 0.35rem;
  color: #fff;
  text-align: center;
  text-decoration: none;
  border-radius: 4px;
  overflow: hidden;
}

.utilisation-cell:hover {
  color: #fff;
  filter: brightness(0.96);
}

.utilisation-cell .container-name,
.utilisation-cell .utilisation-counts {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.utilisation-cell .container-name {
  font-weight: 600;
  min-height: 1.2rem;
}

.utilisation-cell .utilisation-value {
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.2;
  margin-top: auto;
}

.utilisation-cell .utilisation-counts {
  font-size: 0.8rem;
}

.full,
.legend-swatch.full {
  background: #b91c1c;
}

.high,
.legend-swatch.high {
  background: #dc5f57;
}

.medium,
.legend-swatch.medium {
  background: #d97706;
}

.low,
.legend-swatch.low {
  background: #2e7d32;
}

.empty,
.legend-swatch.empty {
  background: #2f80a7;
}
</style>
