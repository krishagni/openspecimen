<template>
  <div class="os-label-scanner">
    <div class="input-group">
      <os-textarea :placeholder="hint" v-model="ctx.scannedText" @update:model-value="handleInput" />
    </div>

    <os-message type="error" v-if="ctx.moreScanned" style="margin-bottom: 0px;">
      <span>{{moreItemsLabelsMessage}}</span>
    </os-message>
  </div>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import util from '@/common/services/Util.js';

export default {
  props: [ 'items', 'field', 'hint', 'assign-rule', 'more-labels-message'],

  data() {
    return {
      ctx: {
        scannedText: '',
      }
    }
  },

  created: function() {
  },

  computed: {
    moreItemsLabelsMessage: function() {
      return this.moreLabelsMessage ?  this.moreLabelsMessage : this.$t('common.more_item_labels_scanned');
    }
  },

  methods: {
    handleInput: function() {
      if (!this.items) {
        return;
      }

      if (this.inputTimer) {
        clearTimeout(this.inputTimer);
        this.inputTimer = null;
      }

      const that = this;
      this.inputTimer = setTimeout(
        function() {
          const labels = util.splitStr(that.ctx.scannedText, /,|\t|\n/, true);
          const props = that.field.split('.');
          let moreScanned = false, itemIdx = 0;
          for (let i = 0; i < labels.length; ++i) {
            let found = false;
            for (let j = itemIdx; j < that.items.length; ++j) {
              if (!that.assignRule || exprUtil.eval(that.items[j], that.assignRule)) {
                that.setValue(that.items[j], props, labels[i]);
                itemIdx = (j + 1);
                found = true;
                break;
              }
            }

            if (!found) {
              moreScanned = true;
              break;
            }
          }

          for (let i = itemIdx; i < that.items.length; ++i) {
            if (!that.assignRule || exprUtil.eval(that.items[i], that.assignRule)) {
              that.setValue(that.items[i], props, null);
            }
          }

          that.ctx.moreScanned = moreScanned;
          if (moreScanned) {
            alertsSvc.info(that.moreItemsLabelsMessage);
          }
        },
        500
      );
    },

    setValue: function(object, props, value) {
      for (let i = 0; i < props.length - 1; ++i) {
        object = object[props[i]] = object[props[i]] || {};
      }

      object[props[props.length - 1]] = value;
    }
  }
}
</script>

<style scoped>

</style>
