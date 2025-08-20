<template>
  <os-button :left-icon="icon" right-icon="caret-down" :label="label" @click="toggleMenu" />

  <os-overlay class="os-dynamic-menu" ref="menuOptions">
    <ul class="search">
      <li>
        <os-input-text v-model="searchTerm" :placeholder="searchHint" @update:modelValue="searchOptions" />
      </li>
    </ul>
    <ul class="options">
      <li v-for="(option, idx) of options || []" :key="idx" @click="optionSelected($event, option)">
        <a> {{option.displayName}} </a>
      </li>

      <li v-if="options && options.length == 0">
        <a class="no-click">{{noOptionsLabel}}</a>
      </li>
      <li v-if="!options">
        <a class="no-click" v-t="'common.loading'"></a>
      </li>
    </ul>
    <ul class="fixed-options" v-if="$slots['fixed-options']">
      <slot name="fixed-options"></slot>
    </ul>
  </os-overlay>
</template>

<script>

export default {
  props: ['icon', 'label', 'options', 'searchHint', 'noOptionsLabel'],

  emits: ['search-options', 'option-selected'],

  data() {
    return {
      searchTerm: '',
    }
  },

  methods: {
    toggleMenu: function(event) {
      this.$refs.menuOptions.toggle(event);
      if (this.optionsLoaded) {
        return;
      }

      this.optionsLoaded = true;
      this.$emit('search-options', null);
    },

    searchOptions: function(searchTerm) {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
        this.searchTimer = null;
      }

      this.searchTimer = setTimeout(() => this.$emit('search-options', searchTerm), searchTerm ? 500 : 0)
    },

    optionSelected: function(event, option) {
      if (event) {
        this.$refs.menuOptions.toggle(event);
      }


      this.$emit('option-selected', option);
    }
  }
}
</script>

<style scoped>
.os-dynamic-menu .os-input-text {
  padding: 0.5rem 1rem;
}

.os-dynamic-menu ul {
  margin: 0.5rem -1.25rem;
  padding: 0rem 0rem 0.5rem 0rem;
  list-style: none;
  border-bottom: 1px solid #ddd;
}

.os-dynamic-menu ul.options {
  max-height: 262px;
  overflow: scroll;
}

.os-dynamic-menu ul:last-child {
  margin-bottom: 0rem;
  border-bottom: 0px;
}

.os-dynamic-menu ul.options li a,
.os-dynamic-menu ul.fixed-options :deep(li a) {
  padding: 0.5rem 1rem;
  text-decoration: none;
  display: inline-block;
  width: 100%;
  color: #212529;
}

.os-dynamic-menu ul.options li a:hover:not(.no-click),
.os-dynamic-menu ul.fixed-options :deep(li a:hover:not(.no-click)) {
  background: #e9ecef;
}

.os-dynamic-menu ul.options li a.no-click,
.os-dynamic-menu ul.fixed-options :deep(li a.no-click) {
  cursor: initial;
}
</style>

<style>
.os-dynamic-menu .p-overlaypanel-content {
  padding: 0.5rem 1.25rem;
}
</style>
