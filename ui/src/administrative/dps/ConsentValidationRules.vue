<template>
  <os-page-toolbar v-show-if-allowed="dpResources.updateOpts">
    <template #default>
      <os-button left-icon="plus" :label="$t('dps.econsents.add_rule')" @click="showAddRule" />
    </template>
  </os-page-toolbar>

  <div v-if="ctx.loading">
    <os-message type="info">
      <span v-t="'dps.econsents.loading_rules'">Loading consent validation rules. Please wait for a moment...</span>
    </os-message>
  </div>

  <div v-else-if="!ctx.rules || !ctx.rules.rules || ctx.rules.rules.length == 0">
    <os-message type="info">
      <span v-t="'dps.econsents.no_rules'">No consent validation rules to show.</span>
    </os-message>
  </div>

  <div v-else>
    <div class="os-econsent-rules-type">
      <div class="label" v-t="'dps.econsents.match'">Match: </div>
      <div class="type">
        <span :class="ctx.rules.type == 'ALL' && 'active'"      @click="changeRuleType('ALL')">
          <span v-t="'dps.econsents.all'">All</span>
        </span>
        <span :class="ctx.rules.type == 'ANY' && 'active'"      @click="changeRuleType('ANY')">
          <span v-t="'dps.econsents.any'">Any</span>
        </span>
        <span :class="ctx.rules.type == 'ADVANCED' && 'active'" @click="changeRuleType('ADVANCED')">
          <span v-t="'dps.econsents.advanced'">Advanced</span>
        </span>
      </div>
    </div>

    <div class="os-econsent-rules-expr" v-if="ctx.rules.type == 'ADVANCED'">
      <os-textarea :disabled="!ctx.allowExprEdit" class="text" v-model="ctx.rules.expr" />

      <span class="buttons" v-show-if-allowed="dpResources.updateOpts">
        <os-button primary :label="$t('common.buttons.edit')" @click="editRulesExpr" v-if="!ctx.allowExprEdit" />
        <os-button primary :label="$t('common.buttons.save')" @click="saveRules"     v-if="ctx.allowExprEdit"  />
        <os-button :label="$t('common.buttons.cancel')" @click="cancelEditRulesExpr" v-if="ctx.allowExprEdit"  />
      </span>
    </div>

    <div class="os-econsent-rules" v-for="rule of ctx.rules.rules" :key="rule.id">
      <div class="rule">
        <div class="id">
          <span>{{rule.id}}</span>
        </div>
        <div class="desc">
          <span>
            <span class="statement">{{rule.statement.statement}} ({{rule.statement.code}})</span>
            <span class="op">{{opDesc(rule.op)}}</span>
            <span class="response">{{rule.values.join(', ')}}</span>
          </span>
        </div>
        <div class="actions" v-show-if-allowed="dpResources.updateOpts">
          <os-button-group>
            <os-button left-icon="edit"  size="small" @click="showEditRule(rule)" />
            <os-button left-icon="trash" size="small" @click="showDeleteRule(rule)" />
          </os-button-group>
        </div>
      </div>
    </div>
  </div>

  <os-dialog ref="addRuleDialog">
    <template #header>
      <span v-if="!ctx.rule.id">
        <span v-t="'dps.econsents.add_rule'">Add Rule</span>
      </span>
      <span v-else>
        <span v-t="'dps.econsents.edit_rule'">Edit Rule</span>
      </span>
    </template>
    <template #content>
      <os-form ref="addEditRuleForm" :schema="ctx.addEditRuleSchema" :data="ctx" @input="handleInput($event)" />
    </template>
    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="cancelAddOrUpdateRule" />
      <os-button primary :label="$t(!ctx.rule.id ? 'common.buttons.add' : 'common.buttons.update')"
        @click="addOrUpdateRule" />
    </template>
  </os-dialog>

  <os-dialog ref="confirmDeleteRuleDialog">
    <template #header>
      <span v-t="'dps.econsents.delete_rule'">Delete Rule?</span>
    </template>
    <template #content>
      <div style="margin-bottom: 1.25rem;">
        <span v-t="'dps.econsents.confirm_delete_rule'">Are you sure you want to delete the rule?</span>
      </div>
      <div style="display: flex;">
        <div><i>{{ctx.toDelete.statement.statement}} ({{ctx.toDelete.statement.code}})</i></div>
        <div style="margin-left: 0.5rem;"><b>{{opDesc(ctx.toDelete.op)}}</b></div>
        <div style="margin-left: 0.5rem;"><i>{{ctx.toDelete.values.join(', ')}}</i></div>
      </div>
    </template>
    <template #footer>
      <os-button text   :label="$t('common.buttons.cancel')" @click="cancelDeleteRule" />
      <os-button danger :label="$t('common.buttons.delete')" @click="deleteRule" />
    </template>
  </os-dialog>
</template>

<script>
import dpResources from './Resources.js';

export default {
  props: ['dp'],

  data() {
    return {
      ctx: {
        loading: false,

        rules: null,

        allowExprEdit: false
      },

      addEditRuleSchema: {rows: []},

      dpResources
    }
  },

  created() {
    this.loadRules();
    this.ctx.addEditRuleSchema = this.getAddEditRuleSchema();
  },

  methods: {
    loadRules: async function() {
      this.ctx.loading = true;
      this.ctx.rules = await this.$osSvc.http.get('consent-validations/dp/' + this.dp.id + '/rules');
      this.ctx.loading = false;
    },

    opDesc: function(op) {
      const t = this.$t;
      switch (op) {
        case 'EQ': return t('dps.econsents.is');
        case 'NE': return t('dps.econsents.is_not');
        case 'IN': return t('dps.econsents.in');
        case 'NOT_IN': return t('dps.econsents.not_in');
      }
    },

    editRulesExpr: function() {
      this.ctx.exprCopy = this.ctx.rules.expr;
      this.ctx.allowExprEdit = true;
    },

    cancelEditRulesExpr: function() {
      this.ctx.rules.expr = this.ctx.exprCopy;
      this.ctx.allowExprEdit = false;
    },

    saveRules: async function() {
      this.ctx.rules = await this.$osSvc.http.put('consent-validations/dp/' + this.dp.id + '/rules', this.ctx.rules);
      this.ctx.allowExprEdit = false;
    },

    changeRuleType: function(ruleType) {
      if (!this.$osSvc.authSvc.isAllowed(dpResources.updateOpts)) {
        return;
      }

      const rules = this.ctx.rules;
      if (rules.type == ruleType) {
        return;
      }

      const previous = rules.type;
      switch (ruleType) {
        case 'ALL':
          rules.type = 'ALL';
          rules.expr = rules.rules.map(rule => rule.id).join(' and ');
          break;

        case 'ANY':
          rules.type = 'ANY';
          rules.expr = rules.rules.map(rule => rule.id).join(' or ');
          break;

        case 'ADVANCED':
          rules.type = 'ADVANCED';
          rules.expr = rules.rules.map(rule => rule.id).join(previous == 'ALL' ? ' and ' : ' or ');
          break;
      }

      this.saveRules();
    },

    showAddRule: function() {
      this.ctx.rule = {};
      this.$refs.addRuleDialog.open();
    },

    showEditRule: function(rule) {
      const toEdit = this.ctx.rule = this.$osSvc.util.clone(rule);
      toEdit.statement.caption = toEdit.statement.statement + ' (' + toEdit.statement.code + ')';
      this.$refs.addRuleDialog.open();
    },

    handleInput: function({field, data}) {
      if (field.name != 'rule.op') {
        return;
      }

      switch (data.rule.op) {
        case 'IN':
        case 'NOT_IN':
          if (data.rule.values && !(data.rule.values instanceof Array)) {
            data.rule.values = [data.rule.values];
          }
          break;

        case 'EQ':
        case 'NE':
        default:
          if (data.rule.values instanceof Array) {
            data.rule.values = data.rule.values[0];
          }
          break;
      }
    },

    addOrUpdateRule: async function() {
      if (!this.$refs.addEditRuleForm.validate()) {
        return;
      }

      const rules = this.ctx.rules = this.ctx.rules || {};
      const rule  = this.ctx.rule;
      if (!(rule.values instanceof Array)) {
        rule.values = [rule.values];
      }

      if (!rule.id) {
        rules.rules = rules.rules || [];

        const maxId = rules.rules.reduce((max, r) => r.id > max ? r.id : max, 0)
        rule.id = maxId + 1;
        rules.rules.push(rule);
      } else {
        const idx = rules.rules.findIndex(r => r.id == rule.id);
        Object.assign(rules.rules[idx], rule);
      }

      await this.saveRules();
      this.$osSvc.alertsSvc.success({code: 'dps.econsents.rule_saved'});
      this.cancelAddOrUpdateRule();
    },

    cancelAddOrUpdateRule: function() {
      this.ctx.rule = null,
      this.$refs.addRuleDialog.close();
    },

    showDeleteRule: function(rule) {
      this.ctx.toDelete = rule;
      this.$refs.confirmDeleteRuleDialog.open();
    },

    cancelDeleteRule: function() {
      this.ctx.toDelete = null;
      this.$refs.confirmDeleteRuleDialog.close();
    },

    deleteRule: async function() {
      const rules = this.ctx.rules;
      const idx = rules.rules.indexOf(this.ctx.toDelete);
      rules.rules.splice(idx, 1);
      await this.saveRules();

      this.$osSvc.alertsSvc.success({code: 'dps.econsents.rule_deleted'});
      this.cancelDeleteRule();
    },

    loadStatements: async function(searchTerm) {
      if (!searchTerm) {
        if (this.ctx.defStatements) {
          return this.ctx.defStatements;
        }
      } else if (this.ctx.defStatements && this.ctx.defStatements.length < 100) {
        searchTerm = searchTerm.toLowerCase();
        return this.ctx.defStatements.filter(
          stmt => this.matches(stmt.statement, searchTerm) || this.matches(stmt.code, searchTerm)
        );
      }

      const statements = await this.$osSvc.http.get('consent-statements', {searchString: searchTerm});
      statements.forEach(stmt => stmt.caption = stmt.statement + ' (' + stmt.code + ')');
      if (!searchTerm) {
        this.ctx.defStatements = statements;
      }

      return statements;
    },

    matches: function(source, test) {
      return source && source.toLowerCase().indexOf(test) > -1;
    },

    getAddEditRuleSchema: function() {
      const t = this.$t;
      return {
        rows: [
          {
            fields: [
              {
                name: 'rule.statement',
                type: 'dropdown',
                labelCode: 'dps.econsents.statement',
                listSource: {
                  loadFn: ({searchTerm}) => this.loadStatements(searchTerm),
                  displayProp: 'caption'
                },
                validations: {
                  required: {
                    messageCode: 'dps.econsents.statement_req'
                  }
                }
              }
            ],
          },
          {
            fields: [
              {
                name: 'rule.op',
                type: 'dropdown',
                labelCode: 'dps.econsents.match',
                listSource: {
                  options: [
                    { name: 'EQ',     caption: t('dps.econsents.is') },
                    { name: 'NE',     caption: t('dps.econsents.is_not') },
                    { name: 'IN',     caption: t('dps.econsents.in') },
                    { name: 'NOT_IN', caption: t('dps.econsents.not_in') }
                  ],
                  displayProp: 'caption',
                  selectProp: 'name'
                },
                validations: {
                  required: {
                    messageCode: 'dps.econsents.match_req'
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                name: 'rule.values',
                type: 'pv',
                labelCode: 'dps.econsents.response',
                attribute: 'consent_response',
                selectProp: 'value',
                multiple: false,
                validations: {
                  required: {
                    messageCode: 'dps.econsents.response_req'
                  }
                },
                showWhen: "rule.op == 'EQ' || rule.op == 'NE' || !rule.op"
              },
              {
                name: 'rule.values',
                type: 'pv',
                labelCode: 'dps.econsents.response',
                attribute: 'consent_response',
                selectProp: 'value',
                multiple: true,
                validations: {
                  required: {
                    messageCode: 'dps.econsents.response_req'
                  }
                },
                showWhen: "rule.op == 'IN' || rule.op == 'NOT_IN'"
              }
            ]
          }
        ]
      };
    }
  },
}
</script>

<style scoped>

.os-econsent-rules-type {
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: row;
}

.os-econsent-rules-type .label {
  padding: 0.5rem;
  font-weight: bold;
}

.os-econsent-rules-type .type {
  display: flex;
  flex-direction: row;
}

.os-econsent-rules-type .type > span {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-right: 0;
  cursor: pointer;
}

.os-econsent-rules-type .type > span.active {
  background: #e6e6e6;
  box-shadow: inset 0 3px 5px rgb(0 0 0 / 13%);
}

.os-econsent-rules-type .type > span:last-child {
  border-right: 1px solid #ddd;
}

.os-econsent-rules-expr {
  display: flex;
  flex-direction: row;
  margin-bottom: 1.25rem;
}

.os-econsent-rules-expr .text {
  flex: 1;
}

.os-econsent-rules-expr .text :deep(textarea) {
  border-top-right-radius: 0px;
  border-bottom-right-radius: 0px;
}

.os-econsent-rules-expr .buttons :deep(button) {
  height: calc(100% - 5px);
  border-radius: 0;
}

.os-econsent-rules-expr .buttons :deep(button:last-child) {
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
}

.os-econsent-rules .rule {
  padding: 1rem;
  margin-bottom: 1.25rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.os-econsent-rules .rule:after {
  content: ' ';
  display: block;
  clear: both;
}

.os-econsent-rules .rule .id {
  height: 3rem;
  width: 3rem;
  border: 1px solid #ddd;
  border-radius: 50%;
  background: #ddd;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
  font-weight: bold;
  float: left;
  margin-right: 1.25rem;
}

.os-econsent-rules .rule .desc {
  height: 3rem;
  float: left;
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin-right: 1.25rem;
}

.os-econsent-rules .rule .desc .statement,
.os-econsent-rules .rule .desc .response {
  font-style: italic;
  display: inline-block;
  margin-right: 0.5rem;
}

.os-econsent-rules .rule .desc .op {
  font-weight: bold;
  display: inline-block;
  margin-right: 0.5rem;
}

.os-econsent-rules .rule .actions {
  height: 3rem;
  float: right;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

</style>
