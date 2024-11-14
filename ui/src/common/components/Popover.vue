<template>
  <div ref="po" :class="classList" :style="style" v-if="show">
    <slot> </slot>

    <div class="arrow" />
  </div>
</template>

<script>
export default {
  props: ['align'],

  data() {
    return {
      show: false,

      style: {},

      arrowStyle: {}
    }
  },

  computed: {
    classList: function() {
      return ['os-popover', this.align || 'left'];
    }
  },

  methods: {
    open: function(event) {
      this.show = true;
      this.currentTarget = event.currentTarget;
      if (this.dimensionObserver) {
        this.dimensionObserver.disconnect();
        this.dimensionObserver = null;
      }

      setTimeout(
        () => {
          this.dimensionObserver = new ResizeObserver(() => this.alignPopover());
          this.dimensionObserver.observe(this.$el);
          this.alignPopover()
        },
        200
      );
    },

    close: function() {
      this.show = false;
      this.dimensionObserver.disconnect();
      this.dimensionObserver = null;
    },

    alignPopover: function() {
      const {top, height, width, left} = this.currentTarget.getBoundingClientRect();

      const adjustedTop  = top +  (window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0);
      const adjustedLeft = left + (window.pageXOffset || document.documentElement.scrollLeft || document.body.scrollLeft || 0);

      const style = this.style = {};
      if (this.align == 'right') {
        style.top  = (adjustedTop - this.$el.clientHeight / 2 + height / 2) + 'px';
        style.left = (adjustedLeft + width + 11) + 'px';
      } else if (this.align == 'top') {
        style.top = (adjustedTop - this.$el.clientHeight - 11) + 'px';
        style.left = (adjustedLeft - (this.$el.clientWidth / 2) + width / 2) + 'px';
      } else if (this.align == 'bottom') {
        style.top = (adjustedTop + height + 11) + 'px';
        style.left = (adjustedLeft - (this.$el.clientWidth / 2) + width / 2) + 'px';
      } else {
        style.top  = (adjustedTop - this.$el.clientHeight / 2 + height / 2) + 'px';
        style.left = (adjustedLeft - this.$el.clientWidth - 11) + 'px';
      }
    }
  }
}
</script>

<style scoped>
.os-popover {
  position: fixed;
  background: #ffffff;
  color: #212529;
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-radius: 4px;
  box-shadow: none;
  z-index: 1001;
  padding: 1.25rem;
}

.os-popover .arrow {
  border-width: 11px;
}

.os-popover .arrow:after {
  border-width: 10px;
}

.os-popover .arrow,
.os-popover .arrow:after {
  position: absolute;
  display: block;
  width: 0;
  height: 0;
  border-color: transparent;
  border-style: solid;
}

.os-popover.top .arrow {
  bottom: -11px;
  left: 50%;
  margin-left: -11px;
  border-top-color: #999999;
  border-top-color: rgba(0, 0, 0, 0.25);
  border-bottom-width: 0;
}

.os-popover.bottom .arrow {
  top: -11px;
  left: 50%;
  margin-left: -11px;
  border-top-width: 0;
  border-bottom-color: #999999;
  border-bottom-color: rgba(0, 0, 0, 0.25);
}

.os-popover.right .arrow {
  top: 50%;
  left: -11px;
  margin-top: -11px;
  border-right-color: #999999;
  border-right-color: rgba(0, 0, 0, 0.25);
  border-left-width: 0;
}

.os-popover.left .arrow {
  top: 50%;
  right: -11px;
  margin-top: -11px;
  border-left-color: #999999;
  border-left-color: rgba(0, 0, 0, 0.25);
  border-right-width: 0;
}

.os-popover.top .arrow:after {
  bottom: 1px;
  margin-left: -10px;
  content: " ";
  border-top-color: #fff;
  border-bottom-width: 0;
}

.os-popover.bottom .arrow:after {
  top: 1px;
  margin-left: -10px;
  content: " ";
  border-top-width: 0;
  border-bottom-color: #fff;
}

.os-popover.right .arrow:after {
  bottom: -10px;
  left: 1px;
  content: " ";
  border-right-color: #fff;
  border-left-width: 0;
}

.os-popover.left .arrow:after {
  bottom: -10px;
  right: 1px;
  content: " ";
  border-left-color: #fff;
  border-right-width: 0;
}
</style>
