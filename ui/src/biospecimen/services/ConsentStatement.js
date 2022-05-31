import http     from '@/common/services/HttpClient.js';
import formUtil from '@/common/services/FormUtil.js';
import util     from '@/common/services/Util.js';

import statementSchema from '@/biospecimen/schemas/consents/statement.js';
import addEditLayout   from '@/biospecimen/schemas/consents/addedit.js';

class ConsentStatement {
  async getStatements(filterOpts) {
    return http.get('consent-statements', filterOpts || {});
  }

  async getStatementsCount(filterOpts) {
    return http.get('consent-statements/count', filterOpts || {});
  }

  async getStatement(statementId) {
    return http.get('consent-statements/' + statementId);
  }

  async saveOrUpdate(statement) {
    if (statement.id) {
      return http.put('consent-statements/' + statement.id, statement);
    } else {
      return http.post('consent-statements', statement);
    }
  }

  getDict() {
    return util.clone(statementSchema.fields);
  }

  getAddEditFormSchema() {
    const addEditFs = formUtil.getFormSchema(statementSchema.fields, addEditLayout.layout);
    return { schema: addEditFs };
  }
}

export default new ConsentStatement();
