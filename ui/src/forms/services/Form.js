
class Form {
  deleteRecord(record) {
    alert('Deleting ' + record.recordId + ' of ' + record.formCaption);
    return new Promise((resolve) => resolve(record));
  }
}

export default new Form();
