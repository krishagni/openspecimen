<template>
  <component :is="tag" :href="url" :target="url ? '_blank' : null" :rel="url ? 'noopener noreferrer' : null"
    :class="['os-container-utilisation-tile', utilisationClass, {fill}]"
    v-os-tooltip="tooltip">
    <span class="container-name">{{displayName}}</span>

    <span class="utilisation-value" v-if="hasUtilisation">{{utilisation}}%</span>
    <span class="utilisation-value" v-else v-t="'containers.utilisation_not_available'">Not Available</span>

    <span class="utilisation-counts" v-if="hasUtilisation">
      {{usedPositions}} / {{totalPositions}}
    </span>
  </component>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['container', 'fill'],

  computed: {
    tag: function() {
      return this.url ? 'a' : 'div';
    },

    url: function() {
      if (!this.container.id) {
        return null;
      }

      return routerSvc.getUrl('ContainerDetail.Utilisation', {containerId: this.container.id});
    },

    displayName: function() {
      if (this.container.emptySlot) {
        return this.$t('containers.empty_container_slot', {type: this.container.typeName});
      }

      return this.container.displayName || this.container.occupyingEntityName || this.container.name;
    },

    isDimensionless: function() {
      return !!this.container.id && !(this.container.noOfRows > 0 && this.container.noOfColumns > 0);
    },

    hasUtilisation: function() {
      return !this.isDimensionless;
    },

    usedPositions: function() {
      return this.container.usedPositions || 0;
    },

    totalPositions: function() {
      return this.container.totalPositions || 0;
    },

    utilisation: function() {
      return this.totalPositions > 0 ? Math.round(this.usedPositions * 100 / this.totalPositions) : 0;
    },

    utilisationClass: function() {
      if (!this.hasUtilisation) {
        return 'not-available';
      } else if (this.utilisation >= 100) {
        return 'full';
      } else if (this.utilisation > 80) {
        return 'high';
      } else if (this.utilisation > 50) {
        return 'medium';
      } else if (this.utilisation > 0) {
        return 'low';
      }

      return 'empty';
    },

    tooltip: function() {
      if (!this.hasUtilisation) {
        return this.$t('containers.utilisation_not_available_tooltip', {name: this.displayName});
      }

      return this.$t(
        'containers.utilisation_cell_tooltip',
        {
          name: this.displayName,
          utilisation: this.utilisation,
          occupied: this.usedPositions,
          total: this.totalPositions
        }
      );
    }
  }
}
</script>

<style scoped>
.os-container-utilisation-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
  min-height: 7rem;
  padding: 0.85rem;
  color: #fff;
  text-align: center;
  text-decoration: none;
  border-radius: 4px;
  overflow: hidden;
}

.os-container-utilisation-tile.fill {
  justify-content: flex-start;
  position: absolute;
  inset: 2px;
  min-height: 0;
  padding: 1.5rem 0.45rem 0.35rem;
}

a.os-container-utilisation-tile:hover {
  color: #fff;
  filter: brightness(0.96);
}

.container-name,
.utilisation-counts {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.container-name {
  font-weight: 600;
  min-height: 1.2rem;
}

.utilisation-value {
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.2;
  margin-top: auto;
}

.utilisation-counts {
  font-size: 0.8rem;
}

.full {
  background: #b91c1c;
}

.high {
  background: #dc5f57;
}

.medium {
  background: #d97706;
}

.low {
  background: #2e7d32;
}

.empty {
  background: #2f80a7;
}

.not-available {
  background: #737373;
}
</style>
