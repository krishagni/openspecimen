
<template>
  <div class="os-storage-position" :class="$attrs['md-type'] && 'md-type'">
    <div class="container">
      <os-dropdown :md-type="$attrs['md-type']" v-model="position.name" :form="context"
        :tab-order="tabOrder" :list-source="containersLs" @update:model-value="handlePositionInput(true)" />
    </div>

    <div class="position" v-if="!position.mode || position.mode == 'TWO_D'">
      <os-input-text :md-type="$attrs['md-type']" v-model="position.positionY"
        :tab-order="tabOrder" @update:model-value="handlePositionInput(false)" />
    </div>

    <div class="position" v-if="!position.mode || position.mode == 'TWO_D'">
      <os-input-text :md-type="$attrs['md-type']" v-model="position.positionX"
        :tab-order="tabOrder" @update:model-value="handlePositionInput(false)" />
    </div>

    <div class="position" v-if="position.mode == 'LINEAR'">
      <os-input-text :md-type="$attrs['md-type']" v-model="position.position"
        :tab-order="tabOrder" @update:model-value="handlePositionInput(false)" />
    </div>

    <div class="search">
      <os-button size="small" left-icon="search" @click="selectPosition"/>
    </div>

    <os-storage-position-selector ref="positionSelector"
      :entity-type="entityType" :criteria="containersListCrit" :query="position.name"
      @position-selected="positionSelected($event)" />
  </div>
</template>

<script>

import http from '@/common/services/HttpClient.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'context', 'listSource', 'tabOrder'],

  computed: {
    position: {
      get() {
        return this.modelValue || {};
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    staticParams: function() {
      const ls = this.listSource || {};
      const qp = ls.queryParams || {};
      return qp.static || {};
    },

    dynamicParams: function() {
      const ls = this.listSource || {};
      const qp = ls.queryParams || {};
      return qp.dynamic || {};
    },

    entityType: function() {
      return this.staticParams.entityType || 'specimen';
    },

    entity: function() {
      return exprUtil.eval(this.context || {}, this.dynamicParams.entity || 'specimen') || {};
    },

    containersListCrit: function() {
      let qp = { onlyFreeContainers: true };
      if (this.entityType == 'specimen') {
        let spmn = this.entity;
        Object.assign(qp, {
          cpId: spmn.cpId,
          specimenClass: spmn.specimenClass,
          specimenType: spmn.type,
          storeSpecimensEnabled: true,
        });

        if (this.dynamicParams.site) {
          qp.site = exprUtil.eval(this.context || {}, this.dynamicParams.site);
        }
      } else if (this.entityType == 'storage_container') {
        let container = this.entity;
        Object.assign(qp, {
          site: container.siteName,
          usageMode: container.usedFor || 'STORAGE'
        });
      } else if (this.entityType == 'order_item') {
        let dp = exprUtil.eval(this.context || {}, this.dynamicParams.dp || 'distributionProtocol');
        Object.assign(qp, {
          dpShortTitle: typeof dp == 'string' ? dp : (dp && dp.shortTitle),
          storeSpecimensEnabled: true
        });
      }

      return qp;
    },

    containersLs: function() {
      let self = this;
      return {
        searchProp: 'name',
        selectProp: 'name',
        displayProp: 'displayTitle',
        loadFn: (opts) => {
          return self.getContainers(opts).then(
            (containers) => {
              containers.forEach(container => {
                container.displayTitle = container.name;
                if (container.displayName) {
                  container.displayTitle = container.displayName + ' (' + container.name + ')';
                }
              });

              self.containers = containers;
              return containers;
            }
          );
        }
      }
    }
  },

  watch: {
    position: function() {
      this.handlePositionInput(false);
    }
  },

  methods: {
    handlePositionInput: function(containerSelected) {
      const {reservationId, name} = this.position || {};
      if (containerSelected && !reservationId && (!this.previousContainer || this.previousContainer != name)) {
        let selectedContainer = (this.containers || []).find(container => container.name == name);
        if (selectedContainer) {
          this.position.mode = selectedContainer.positionLabelingMode;
        }

        Object.assign(this.position, {positionX: undefined, positionY: undefined, position: undefined});
        delete this.position.id;
      }

      this.$emit('update:modelValue', this.position);
      this.previousContainer = this.position && this.position.name;
    },

    getContainers: async function(opts) {
      let crit = {...this.containersListCrit, name: opts.name || opts.query, maxResults: 10};
      if (opts._selected) {
        crit.onlyFreeContainers = null;
        crit.name = null;
        crit.naam = opts._selected;
      }

      if (this.entityType == 'specimen') {
        if (!crit.specimenType) {
          return [];
        }
      } else if (this.entityType == 'storage_container') {
        // if (!crit.site) {
        //  return [];
        // }
      } else if (this.entityType == 'order_item') {
        if (!crit.dpShortTitle) {
          return [];
        }
      }

      let cache = (this.context && this.context._formCache) || {};
      cache = cache['storage-position.containers'] = cache['storage-position.containers'] || {};

      const qs = util.queryString(crit);
      if (!cache[qs]) {
        cache[qs] = http.get('storage-containers', crit);
      }

      return await cache[qs];
    },
 
    selectPosition: function() {
      this.$refs.positionSelector.open();
    },

    positionSelected: function({position}) {
      this.position = position;
      this.previousContainer = position && position.name;
    },

    getDisplayValue: function() {
      const position = this.position;
      if (!position || !position.name) {
        return null;
      }

      if ((!position.mode || position.mode == 'TWO_D') && position.positionY && position.positionX) {
        return position.name + ' (' + position.positionY + ', ' + position.positionX + ')';
      } else if (position.position) {
        return position.name + ' (' + position.position + ')';
      } else {
        return position.name;
      }
    }
  }
}
</script>

<style>

.os-storage-position {
  display: flex!important;
  flex-direction: row;
  align-items: stretch;
}

.os-storage-position .container {
  width: 60%;
  margin-right: 0.2rem;
}

.os-storage-position .position {
  width: 15%;
  margin-right: 0.2rem;
}

.os-storage-position.md-type .search {
  font-size: 75%;
}

</style>
