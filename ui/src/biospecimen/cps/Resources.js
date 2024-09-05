
class CpResources {
  createOpts    = {resource: 'CollectionProtocol', operations: ['Create']};

  updateOpts    = {resource: 'CollectionProtocol', operations: ['Update']};

  deleteOpts    = {resource: 'CollectionProtocol', operations: ['Delete']};

  importOpts    = {resource: 'CollectionProtocol', operations: ['Export Import']};
}

export default new CpResources();
