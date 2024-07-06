<template>
  <os-page>
    <os-page-head :noNavButton="true">
      <span class="os-title">
        <h3>{{form.caption}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div>
        <os-tab-menu>
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('Preview')">
                <span v-t="'forms.preview'">Preview</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('Associations')">
                <span v-t="'forms.associations'">Associations</span>
              </router-link>
            </li>
          </ul>
        </os-tab-menu>

        <router-view :form="form" v-if="form && form.formId > 0" :key="form.formId" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
export default {
  props: ['form'],

  data() {
    return { };
  },

  created() {
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }
  },

  watch: {
  },

  methods: {
    getRoute: function(routeName) {
      return { name: 'FormsListItemDetail.' + routeName, params: {}, query: this.query };
    }
  }
}
</script>
