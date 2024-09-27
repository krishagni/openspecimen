<template>
  <div class="os-container-selector">
    <div class="search">
      <os-input-text v-model="ctx.name" :placeholder="$t('containers.search_by_name')" />
    </div>

    <div class="containers">
      <div v-if="ctx.loading">
        <os-message type="info">
          <span v-t="'containers.loading'">Loading containers. Please wait for a moment...</span>
        </os-message>
      </div>
      <div v-else-if="!containerTree || containerTree.length == 0">
        <os-message type="warn">
          <span v-t="'containers.no_containers_match_criteria'">No containers match the criteria.</span>
        </os-message>
      </div>
      <table v-else class="os-table os-boxed">
        <thead>
          <tr>
            <th v-t="'containers.name'">Name</th>
            <th v-t="'containers.dimension'">Dimension</th>
            <th v-t="'containers.free_locations'">Free Locations</th>
            <th>&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="cont of containerTree" :key="cont.id">
            <td>
              <span @click="toggleContainerNode(cont)" :style="{'padding-left': cont.iconPadding}">
                <os-icon name="chevron-circle-right" v-show="cont.hasChildren && !cont.isOpened" />
                <os-icon name="chevron-circle-down"  v-show="cont.hasChildren && cont.isOpened" />
              </span>
              <span :style="{'padding-left': cont.namePadding}">
                {{cont.displayName ? cont.displayName + ' (' + cont.name + ')' : cont.name}}
              </span>
            </td>
            <td>
              <span v-if="!cont.positionLabelingMode || cont.positionLabelingMode == 'NONE'"> - </span>
              <span v-else> {{cont.noOfRows}} x {{cont.noOfColumns}} </span>
            </td>
            <td>
              <span v-if="cont.positionLabelingMode != 'NONE' && cont.canStore">{{cont.freePositions}}</span>
              <span v-else> - </span>
            </td>
            <td>
              <os-button v-if="cont.canStore && (cont.positionLabelingMode == 'NONE' || cont.freePositions > 0) &&
                (!ctx.selectedContainer || ctx.selectedContainer.id != cont.id)"
                size="small" left-icon="check" label="Select" @click="toggleContainerSelection(cont)">
              </os-button>
              <os-button v-if="cont.canStore && (cont.positionLabelingMode == 'NONE' || cont.freePositions > 0) &&
                (ctx.selectedContainer && ctx.selectedContainer.id == cont.id)"
                size="small" left-icon="times" label="Unselect" @click="toggleContainerSelection(cont)">
              </os-button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>

import http from '@/common/services/HttpClient.js';

export default {
  props: ['entityType', 'criteria', 'query'],

  emits: ['container-selected'],

  data: function() {
    return {
      ctx: {
        containers: undefined,
        loading: true,
        name: this.query,
      }
    }
  },

  created() {
    this.loadContainers();
  },

  watch: {
    'ctx.name': function() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
      }

      this.searchTimer = setTimeout(() => this.loadContainers(null), 500);
    }
  },

  computed: {
    containerTree: function() {
      let tree = (this.ctx.containers || []).filter(container => this.isContainerNodeVisible(container));
      tree.forEach(
        (cont) => {
          cont.canStore = this.entityType == 'storage_container' ||
            (this.entityType == 'specimen'   && cont.usedFor == 'STORAGE'      && cont.storeSpecimensEnabled) ||
            (this.entityType == 'order_item' && cont.usedFor == 'DISTRIBUTION' && cont.storeSpecimensEnabled);

          cont.iconPadding = (cont.hasChildren ? cont.depth : (cont.depth ? cont.depth + 0.875 : 0)) + 'rem';
          cont.namePadding = (cont.hasChildren || cont.depth ? 0.5 : 0) + 'rem';
        }
      );

      return tree;
    },
  },

  methods: {
    loadContainers: function(parentContainerId) {
      let hierarchical = true;
      if (this.ctx.name) {
        hierarchical = false;
      }

      let qp = {
        ...this.criteria, 
        name: this.ctx.name, 
        topLevelContainers: !parentContainerId && hierarchical, 
        hierarchical: hierarchical
      };

      this.ctx.loading = true;
      http.get('storage-containers', qp).then(
        (containers) => {
          this.ctx.loading = false;
          this.ctx.containers = containers;
          containers.forEach(
            (container) => {
              container.hasChildren = qp.hierarchical;
              container.isOpened = false;
              container.depth = 0;
            }
          );
        }
      );
    },

    toggleContainerNode: function(container) {
      container.isOpened = !container.isOpened;
      if (container.isOpened) {
        this.loadChildren(container);
      }
    },

    isContainerNodeVisible: function(container) {
      if (container.depth == 0) {
        return true;
      }

      let show = true;
      while (container.parent) {
        if (!container.parent.isOpened) {
          show = false;
          break;
        }

        container = container.parent;
      }

      return show;
    },

    loadChildren: function(container) {
      if (container.childrenLoaded) {
        return;
      }

      const parentIdx = this.ctx.containers.indexOf(container);
      this.ctx.containers.splice(parentIdx + 1, 0, {id: -container.id, name: 'Loading...', depth: container.depth + 1});

      let qp = {...this.criteria, hierarchical: true, parentContainerId: container.id};
      http.get('storage-containers', qp).then(
        (children) => {
          container.childrenLoaded = true;
          container.hasChildren = children.length > 0;
          children.forEach(
            (child) => {
              child.hasChildren = true;
              child.isOpened = false;
              child.parent = container;
              child.depth = container.depth + 1;
            }
          );

          this.ctx.containers.splice(parentIdx + 1, 1, ...children);
        }
      );
    },

    toggleContainerSelection: function(cont) {
      if (this.ctx.selectedContainer && this.ctx.selectedContainer.id == cont.id) {
        this.ctx.selectedContainer = null;
      } else {
        this.ctx.selectedContainer = cont;
      }

      this.$emit('container-selected', {container: this.ctx.selectedContainer});
    }
  }
}
</script>

<style scoped>

.os-container-selector .search {
  margin-bottom: 1.25rem;
}

</style>
