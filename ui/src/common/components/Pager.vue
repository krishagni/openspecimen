
<template>
  <div class="os-pager">
    <os-button left-icon="angle-double-left" label="Previous" :disabled="firstPage" @click="previous" />
    <os-button label="Next" right-icon="angle-double-right" :disabled="lastPage" @click="next" />
  </div>
</template>

<script>

export default {
  props: ['startAt', 'haveMore'],

  computed: {
    firstPage: function() {
      return !this.startAt;
    },

    lastPage: function() {
      return !this.haveMore
    }
  },

  methods: {
    previous: function() {
      this.$emit('previous');
    },

    next: function() {
      this.$emit('next');
    }
  }
}

</script>

<style scoped>

.os-pager {
  display: block;
  margin-bottom: 1.25rem;
}

.os-pager:after {
  content: '';
  clear: both;
  display: block;
}

.os-pager button {
  border-radius: 1rem;
  background: #fff;
  color: #337ab7;
}

.os-pager button:disabled {
  cursor: not-allowed;
  color: inherit;
  pointer-events: none;
}

.os-pager button:hover:not([disabled]) {
  background: #eee;
}

.os-pager button:first-child {
  float: left;
}

.os-pager button:last-child {
  float: right;
  margin-right: 1rem;
}
</style>
