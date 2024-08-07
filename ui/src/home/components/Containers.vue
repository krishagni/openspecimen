<template>
  <os-home-list-card :icon="'boxes'" :title="$t('common.home.containers')"
    :show-star="true" :list-url="{name: 'ContainersList'}" :list="ctx.containers"
    @search="search($event)" @toggle-star="toggleStar($event)" />
</template>

<script>
import containerSvc from '@/administrative/services/Container.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['widget'],

  data() {
    return {
      ctx: {
        containers: [],

        defList: null,

        search: null
      }
    };
  },

  created() {
    this._loadContainers();
  },

  methods: {
    search: function({searchTerm}) {
      this.ctx.search = searchTerm;
      this._loadContainers(searchTerm);
    },

    toggleStar: function(container) {
      const promise = container.starred ? containerSvc.unstar(container.id) : containerSvc.star(container.id);
      promise.then(
        () => {
          this.ctx.defList = null;
          this._loadContainers(this.ctx.search);
        }
      );
    },

    _loadContainers: function(searchTerm) {
      if (!searchTerm && this.ctx.defList) {
        this.ctx.containers = this.ctx.defList;
        return;
      }

      const filterOpts = {topLevelContainers: true, name: searchTerm, orderByStarred: true, maxResults: 25};
      containerSvc.getContainers(filterOpts).then(
        (containers) => {
          this.ctx.containers = containers;
          if (!searchTerm) {
            this.ctx.defList = containers;
          }

          for (let container of containers) {
            if (container.displayName) {
              container.displayName += ' (' + container.name + ')';
            } else {
              container.displayName = container.name;
            }

            container.url = routerSvc.getUrl('ContainerDetail.Locations', {containerId: container.id});
          }
        }
      );
    }
  }
}
</script>
