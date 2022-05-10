
class ContainerResources {
  createOpts    = {resource: 'StorageContainer', operations: ['Create']};

  updateOpts    = {resource: 'StorageContainer', operations: ['Update']};

  deleteOpts    = {resource: 'StorageContainer', operations: ['Delete']};

  importOpts    = {resource: 'StorageContainer', operations: ['Export Import']};
}

export default new ContainerResources();
