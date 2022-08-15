<template>
  <div class="os-add-items">
    <os-textarea :placeholder="placeholder" v-model="input" />

    <span class="buttons">
      <os-button primary :label="$t('common.buttons.add')" @click="addItems" />
      <slot></slot>
    </span>
  </div>
</template>

<script>

export default {

  props: ['placeholder'],

  emits: ['on-add'],

  data() {
    return {
      input: '',
    }
  },

  methods: {
    getLabels: function() {
      return this.input.split(/,|\t|\n/)
        .map(function(label) { return label.trim(); })
        .filter(function(label) { return label.length != 0; });
    },

    addItems: function() {
      let itemLabelsMap = this.getLabels().reduce(
        (acc, itemLabel) => {
          acc[itemLabel] = true;
          return acc
        }, {}
      );
      this.$emit('on-add', {itemLabels: Object.keys(itemLabelsMap)});
    }
  }
}

</script>

<style scoped>

.os-add-items {
  display: flex;
}

/* Make the text area consume as much free space available */
.os-add-items :deep(.os-input-text) {
  flex: 1;
}

.os-add-items :deep(.os-input-text > div) {
  margin-bottom: -5px;
}

.os-add-items :deep(.os-input-text textarea) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.os-add-items :deep(.os-input-text textarea:focus) {
  border-color: #ced4da;
}

.os-add-items .buttons :deep(button) {
  height: 100%;
  border-radius: 0;
}

.os-add-items .buttons :deep(button) {
  border-left: 0;
}

.os-add-items .buttons :deep(button:last-child) {
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
}
</style>
