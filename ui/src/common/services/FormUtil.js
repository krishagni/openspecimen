
class FormUtil {

  getFormSchema(baseSchema, layoutSchema) {
    if (!layoutSchema.rows || layoutSchema.rows.length == 0) {
      return [];
    }

    let dict = baseSchema.reduce((acc, field) => (acc[field.name] = field) && acc, {});
    let result = {rows: []};
    for (let row of layoutSchema.rows) {
      let fields = [];
      for (let field of row.fields) {
        let ff = JSON.parse(JSON.stringify(field));
        if (!ff.name || !dict[ff.name]) {
          continue;
        }

        ff = Object.assign(JSON.parse(JSON.stringify(dict[ff.name])), ff);
        fields.push(ff);
      }

      if (fields.length > 0) {
        result.rows.push({fields: fields});      
      }
    }

    return result;
  }

  relinkFormRecords(forms, records) {
    let fcMap = {};
    forms.forEach((form) => { form.records = []; fcMap[form.formCtxtId] = form });
    records.forEach(
      (formRecs) => {
        formRecs.records.forEach(
          (record) => {
            let form = fcMap[record.fcId];
            form.records = form.records || [];
            form.records.push(record);
          }
        );
      }
    );
  }
}

export default new FormUtil();
