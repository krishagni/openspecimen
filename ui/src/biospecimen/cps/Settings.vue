<template>
  <div class="os-cp-settings" v-if="showCards">
    <router-link :to="{name: 'CpDetail.Settings.LabelFormats', params: {cpId: cp.id}}">
      <os-card>
        <template #body>
          <span v-t="'cps.label_fmt_n_print'">Label Format and Print</span>
        </template>
      </os-card>
    </router-link>
  </div>

  <router-view :cp="cp" v-else-if="cp && cp.id > 0" :key="cp.id" @cp-saved="onSave($event)" />
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cp'],

  emits: ['cp-saved'],

  data() {
    return {
      showCards: true
    }
  },

  mounted() {
    this._showOrHideCards();
  },

  watch: {
    '$route': function() {
      this._showOrHideCards();
    }
  },

  methods: {
    onSave: function(savedCp) {
      this.$emit('cp-saved', savedCp);
    },

    _showOrHideCards: function() {
      const {name} = routerSvc.getCurrentRoute();
      this.showCards = name == 'CpDetail.Settings';
    }
  }
}
</script>

<style scoped>
.os-cp-settings {
  display: flex;
  flex-direction: column;
}

.os-cp-settings :deep(.os-card) {
  width: 20%;
  text-align: center;
  font-size: 1rem;
  cursor: pointer;
}

.os-cp-settings a {
  color: #333;
  text-decoration: none;
}
</style>
