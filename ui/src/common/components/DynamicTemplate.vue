<script>
export default {
  props: ['template'],

  render() {
    const curatedTmpl = (this.template || '')
      .replaceAll('ng-if',    'v-if')
      .replaceAll('ng-style', ':style');
    return this.$osSvc.util.render(curatedTmpl, this.$attrs);
  }
}
</script>
