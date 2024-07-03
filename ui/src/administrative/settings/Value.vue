
<template>
  <span>
    <span v-if="setting.value == undefined || setting.value == null || setting.value.trim().length == 0">
      <span>-</span>
    </span>
    <span v-else-if="setting.type == 'BOOLEAN'">
      <span v-show="setting.value == 'true'" v-t="'common.enabled'">Enabled</span>
      <span v-show="setting.value == 'false'" v-t="'common.disabled'">Disabled</span>
    </span>
    <span v-else-if="setting.type == 'CHAR'">
      <span v-if="setting.value.charCodeAt(0) <= 32">
        <span>Code: {{setting.value.charCodeAt(0)}}</span>
      </span>
      <span v-else>
        <span>{{setting.value}}</span>
      </span>
    </span>
    <span v-else-if="setting.type == 'FILE'" @click="$event.stopPropagation()">
      <a :href="setting.fileUrl" target="_blank" rel="noopener" v-if="showLink != false">
        <span>{{setting.filename}}</span>
      </a>
      <span v-else>{{setting.filename}}</span>
    </span>
    <span v-else>
      <span>{{setting.value}}</span>
    </span>
  </span>
</template>

<script>
export default {
  props: ['setting', 'show-link']
}
</script>
