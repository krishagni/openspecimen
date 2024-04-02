
<template>
  <span class="os-list-size">
    <span v-t="{path: 'common.list_size.showing', args: {size: viewSize}}">Showing {{viewSize}}</span>

    <span v-show="hasMore">
      <span v-t="'common.list_size.of'"> of </span>

      <a v-show="!showListSize" @click="loadListSize"><span v-t="'common.list_size.many_more'"> many more </span></a>

      <span v-show="showListSize">{{listSize == -1 ? '...' : listSize}}&nbsp;</span>
    </span>

    <span v-t="'common.list_size.records'"> records </span>
  </span>
</template>

<script>

export default {
  props: ['list', 'pageSize', 'listSize'],

  emits: ['updateListSize'],

  data() {
    return {
      showListSize: false
    }
  },

  methods: {
    loadListSize: function() {
      this.showListSize = true;
      this.$emit('updateListSize');
    }
  },

  computed: {
    viewSize: function() {
      if (this.list.length < this.pageSize) {
        return this.list.length;
      } else {
        return this.list.length - 1;
      }
    },

    hasMore: function() {
      return this.list.length >= this.pageSize;
    }
  },

  watch: {
    list: {
      deep: true,

      handler: function(newVal, oldVal) {
        if (this.list.length < this.pageSize || !this.showListSize || newVal == oldVal) {
          return;
        }

        this.loadListSize();
      }
    }
  }
}

</script>

<style scoped>
  .os-list-size a {
    cursor: pointer;
  }
</style>
