<template>
  <Dropdown class="os-cp-group-dropdown"
    ref="selectWidget"
    v-model="selected"
    :options="options"
    option-label="label"
    option-group-label="group"
    option-group-children="items"
    :filter="true"
    :show-clear="true"
    :disabled="disabled"
    :tabindex="tabOrder"
    @focus="loadOptions()"
    @filter="loadOptions($event.value)"
  />
</template>

<script>
import Dropdown from 'primevue/dropdown';

import http     from '@/common/services/HttpClient.js';

export default {
  props: ['modelValue', 'form', 'disabled', 'context', 'tabOrder'],

  emits: ['update:modelValue'],

  components: {
    Dropdown
  },

  data() {
    return { 
      options: []
    };
  },

  computed: {
    selected: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  async created() {
    this.options = await this._getSelectedOption(this.selected);
    if (this.options.length > 0 && this.selected && this.selected.id) {
      for (const group of this.options) {
        for (const item of group.items) {
          if (item.id == this.selected.id) {
            this.selected = item;
            break;
          }
        }
      }
    }
  },

  methods: {
    getDisplayValue: function() {
      if (!this.selected) {
        return null;
      }

      return this.selected.label;
    }, 

    loadOptions: async function(searchTerm) {
      const selected = await this._getSelectedOption(this.modelValue);
      const items    = await http.get('collection-protocols/cps-n-groups', {query: searchTerm});
      this.options = this._dedup(this._concat(selected, this._toOptions(items)));
    },

    _getSelectedOption: async function(value) {
      if (!value || !value.id) {
        return [];
      }

      let selected = {};
      for (const group of this.options || []) {
        for (const item of group.items) {
          if (item.id == value.id) {
            selected = item;
            break;
          }
        }
      }

      if (!selected.id) {
        return http.get('collection-protocols/cps-n-groups', {query: value.id}).then(items => this._toOptions(items));
      } else if (selected) {
        return this._toOptions([selected]);
      } else {
        return [];
      }
    },

    _toOptions: function(items) {
      const map = {}; 
      for (const item of items) {
        map[item.group] = map[item.group] || {group: item.group, items: []};
        map[item.group].items.push(item);
      }
          
      return Object.values(map);
    },

    _concat: function(opts1, opts2) {
      for (const g2 of opts2) {
        let found = false;
        for (const g1 of opts1) {
          if (g1.group == g2.group) {
            g1.items = g1.items.concat(g2.items);
            found = true;
            break;
          }
        }

        if (!found) {
          opts1.push(g2);
        }
      }

      return opts1;
    },

    _dedup: function(options) {
      const seen = {}, groups = [];
      for (const option of options) {
        const grp = {...option};

        const result = [];
        for (const item of grp.items) {
          if (!seen[item.id]) {
            result.push(item);
            seen[item.id] = true;
          }
        }

        grp.items = result;
        groups.push(grp);
      }

      return groups;
    }
  }
}
</script>

<style scoped>
.os-cp-group-dropdown {
  width: 100%;
}
</style>
