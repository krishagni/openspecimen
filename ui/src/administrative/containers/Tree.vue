<template>
  <Tree class="os-container-tree"
    :value="containerTree"
    :selectionMode="'single'"
    :selectionKeys="selectedNodes"
    :expandedKeys="expandedNodes"
    @node-expand="loadChildContainers"
    @node-collapse="hideContainers"
    @node-select="selectContainer"
    :loading="loading">
    <template #default="slotProps">
      <a :ref="'node-' + slotProps.node.key" @click="nodeClick">
        <span>{{slotProps.node.label}}</span>
      </a>
    </template>
  </Tree>
</template>

<script>
import Tree from 'primevue/tree';

import containerSvc from '@/administrative/services/Container.js';

export default {

  props: ['container'],

  emits: ['container-selected'],

  components: {
    Tree
  },

  data() {
    return {
      containerTree: [],

      loading: false,

      selectedNodes: {},

      expandedNodes: {},
    }
  },

  created() {
    this.setupTree();
  },

  watch: {
    'container.id': function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      if (!this.selectedNodes[newVal]) {
        this.selectedNodes = {};
        this.selectedNodes[newVal] = true;
      }
    }
  },

  methods: {
    setupTree: async function() {
      const container = this.container;

      let root, children;
      if (!container.storageLocation || !container.storageLocation.id || container.storageLocation.id <= 0) {
        // freezer
        children = await containerSvc.getChildContainers(container);
        root = {
          key: container.id,
          icon: 'pi pi-circle-fill ' + this.getStatusBallColor(container),
          label: container.displayName || container.name,
          data: {parent: null, container}
        };
      } else {
        // intermediate container
        const hierarchy = await containerSvc.getAncestorsHierarchy(container);
        children = hierarchy.childContainers;
        root = {
          key: hierarchy.id,
          icon: 'pi pi-circle-fill ' + this.getStatusBallColor(hierarchy),
          label: hierarchy.displayName || hierarchy.name,
          data: {parent: null, container: hierarchy}
        };
      }

      this.addTreeNodes(root, children);
      this.containerTree = [root];

      let found = null;
      const working = [root];
      while (working.length > 0) {
        const current = working.splice(0, 1);
        if (current[0].data.container.id == container.id) {
          found = current[0];
          break;
        }

        Array.prototype.push.apply(working, current[0].children || []);
      }

      while (found) {
        if (found.children) {
          this.expandedNodes[found.key] = true;
        }

        found = found.data.parent;
      }

      const that = this;
      this.selectedNodes[container.id] = true;
      setTimeout(
        () => {
          let el = that.$refs['node-' + container.id];
          if (el instanceof Array) {
            el = el[0];
          }

          el.scrollIntoView({block: 'center', behaviour: 'smooth'});
        }, 500);
    },

    loadChildContainers: async function(node) {
      const container = node.data.container;
      if (container.childContainers instanceof Array) {
        return;
      }

      this.loading = true;
      const children = container.childContainers = await containerSvc.getChildContainers(container);
      if (children.length == 0) {
        node.leaf = true;
      } else {
        this.addTreeNodes(node, children);
      }

      this.loading = false;
      this.expandedNodes[node.key] = true;
    },

    hideContainers: function(node) {
      delete this.expandedNodes[node.key];
    },

    addTreeNodes(parent, children) {
      parent.children = [];
      for (let child of children) {
        const node = {
          key: child.id,
          icon: 'pi pi-circle-fill ' + this.getStatusBallColor(child),
          label: child.displayName || child.name,
          data: {parent: parent, container: child},
          leaf: false
        };

        parent.children.push(node);
        if (child.childContainers instanceof Array) {
          this.addTreeNodes(node, child.childContainers);
        }
      }
    },

    selectContainer: function(node) {
      if (!this.selectedNodes[node.key]) {
        this.$emit('container-selected', node.data.container);
      }

      this.selectedNodes = {}
      this.selectedNodes[node.key] = true;
    },

    getStatusBallColor: function(container) {
      if (container.status == 'CHECKED_OUT') {
        return 'os-checked-out-ball';
      } else if (container.activityStatus == 'Closed') {
        return 'os-archived-ball';
      } else {
        return 'os-active-ball';
      }
    }
  }
}
</script>

<style scoped>
.os-container-tree {
  height: 100%;
  overflow-y: auto;
  padding: 0px;
}

.os-container-tree :deep(.p-tree-container .p-treenode .p-treenode-content.p-highlight) {
  background: #ffc;
}

.os-container-tree :deep(.p-tree-container .p-treenode .p-treenode-content.p-highlight .p-tree-toggler) {
  color: #6c757d;
}

.os-container-tree :deep(.p-tree-container .p-treenode .p-treenode-content:focus) {
  box-shadow: none;
}

.os-container-tree :deep(.p-treenode-icon.os-active-ball) {
  color: #5CB85C!important;
}

.os-container-tree :deep(.p-treenode-icon.os-archived-ball) {
  color: #DC3545!important;
}

.os-container-tree :deep(.p-treenode-icon.os-checked-out-ball) {
  color: #FEAE65!important;
}

.os-container-tree :deep(.p-treenode-icon) {
  position: relative;
}

.os-container-tree :deep(.p-treenode-icon::after) {
  background-color: #000000;
  border-radius: 3px;
  color: #ffffff;
  display: none;
  padding: .25em .5rem;
  max-width: 12.5rem;
  position: absolute;
  text-align: center;
  z-index: 999;
  bottom: 0;
  left: 50%;
  transform: translate(-50%, calc(100% + 10px));
}

.os-container-tree :deep(.p-treenode-icon.os-active-ball::after) {
  content: 'Active';
}

.os-container-tree :deep(.p-treenode-icon.os-archived-ball::after) {
  content: 'Archived';
}

.os-container-tree :deep(.p-treenode-icon.os-checked-out-ball::after) {
  content: 'Checked Out';
}


.os-container-tree :deep(.p-treenode-icon:hover::after) {
  display: block;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
  font-size: 0.9rem;
  font-weight: normal;
}
</style>
