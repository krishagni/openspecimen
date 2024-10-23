
class QueryResources {
  runOpts       = {resource: 'Query', operations: ['Read']};

  createOpts    = {resource: 'Query', operations: ['Create']};

  updateOpts    = {resource: 'Query', operations: ['Update']};

  deleteOpts    = {resource: 'Query', operations: ['Delete']};

  importOpts    = {resource: 'Query', operations: ['Export Import']};
}

export default new QueryResources();
