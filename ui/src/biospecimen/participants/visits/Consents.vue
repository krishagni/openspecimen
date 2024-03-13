<template>
  <os-plugin-views page="visit-consents" view="list" :viewProps="{cp, cpr, visit}" />
</template>

<script>
export default {
  props: ['cpr', 'visit'],

  inject: ['cpViewCtx'],

  data() {
    return {
      cp: this.cpViewCtx.getCp()
    }
  }
}
</script>
