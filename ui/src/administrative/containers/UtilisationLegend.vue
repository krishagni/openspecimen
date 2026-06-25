<template>
  <span>
    <os-button left-icon="palette" :label="$t('containers.view_utilisation_legend')" @click="toggle" />

    <os-overlay ref="overlay">
      <ul class="os-utilisation-legend">
        <li class="legend-item" v-for="item of legend" :key="item.key">
          <span :class="['legend-swatch', item.key]" />
          <span class="legend-label">{{item.range}}, {{item.label}}</span>
        </li>
      </ul>
    </os-overlay>
  </span>
</template>

<script>
export default {
  computed: {
    legend: function() {
      return [
        {key: 'full',          range: '100%',   label: this.$t('containers.utilisation_full')},
        {key: 'high',          range: '> 80%',  label: this.$t('containers.utilisation_high_label')},
        {key: 'medium',        range: '51-80%', label: this.$t('containers.utilisation_medium_label')},
        {key: 'low',           range: '1-50%',  label: this.$t('containers.utilisation_low_label')},
        {key: 'empty',         range: '0%',     label: this.$t('containers.utilisation_empty_label')},
        {key: 'not-available', range: '-',      label: this.$t('containers.utilisation_not_available')}
      ];
    }
  },

  methods: {
    toggle: function(event) {
      this.$refs.overlay.toggle(event);
    }
  }
}
</script>

<style scoped>
.os-utilisation-legend {
  margin: -0.75rem -1rem;
  padding: 0.5rem 0;
  min-width: 12rem;
  list-style: none;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  white-space: nowrap;
}

.legend-swatch {
  display: inline-block;
  width: 0.9rem;
  height: 0.9rem;
  border-radius: 2px;
  border: 1px solid rgba(0, 0, 0, 0.16);
}

.legend-label {
  font-size: 0.875rem;
}

.legend-swatch.full {
  background: #b91c1c;
}

.legend-swatch.high {
  background: #dc5f57;
}

.legend-swatch.medium {
  background: #d97706;
}

.legend-swatch.low {
  background: #2e7d32;
}

.legend-swatch.empty {
  background: #2f80a7;
}

.legend-swatch.not-available {
  background: #737373;
}
</style>
