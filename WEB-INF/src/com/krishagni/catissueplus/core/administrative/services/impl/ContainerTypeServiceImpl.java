package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTypeErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTypeFactory;
import com.krishagni.catissueplus.core.administrative.events.ContainerTypeDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerTypeSummary;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTypeListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerTypeService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;

public class ContainerTypeServiceImpl implements ContainerTypeService, ObjectAccessor, InitializingBean {
	private DaoFactory daoFactory;
	
	private ContainerTypeFactory containerTypeFactory;

	private ExportService exportSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setContainerTypeFactory(ContainerTypeFactory containerTypeFactory) {
		this.containerTypeFactory = containerTypeFactory;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ContainerTypeSummary>> getContainerTypes(RequestEvent<ContainerTypeListCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureReadContainerTypeRights();
			List<ContainerType> types = daoFactory.getContainerTypeDao().getTypes(req.getPayload());
			return ResponseEvent.response(ContainerTypeSummary.from(types));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getContainerTypesCount(RequestEvent<ContainerTypeListCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureReadContainerTypeRights();
			return ResponseEvent.response(daoFactory.getContainerTypeDao().getTypesCount(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerTypeDetail> getContainerType(RequestEvent<EntityQueryCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureReadContainerTypeRights();
			EntityQueryCriteria crit = req.getPayload();
			ContainerType containerType = getContainerType(crit.getId(), crit.getName());
			return ResponseEvent.response(ContainerTypeDetail.from(containerType));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ContainerTypeDetail> createContainerType(RequestEvent<ContainerTypeDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureCreateOrUpdateContainerTypeRights();

			ContainerTypeDetail input = req.getPayload();
			ContainerType containerType = containerTypeFactory.createContainerType(input);
			ensureUniqueConstraints(null, containerType);
			
			daoFactory.getContainerTypeDao().saveOrUpdate(containerType, true);
			return ResponseEvent.response(ContainerTypeDetail.from(containerType));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ContainerTypeDetail> updateContainerType(RequestEvent<ContainerTypeDetail> req) {
		return updateContainerType(req, false);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerTypeDetail> patchContainerType(RequestEvent<ContainerTypeDetail> req) {
		return updateContainerType(req, true);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req) {
		try {
			ContainerType existing = getContainerType(req.getPayload(), null);
			return ResponseEvent.response(existing.getDependentEntities());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ContainerTypeDetail> deleteContainerType(RequestEvent<Long> req) {
		try {
			AccessCtrlMgr.getInstance().ensureCreateOrUpdateContainerTypeRights();
			ContainerType existing = getContainerType(req.getPayload(), null);
			existing.delete();
			return ResponseEvent.response(ContainerTypeDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> deleteContainerTypes(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			AccessCtrlMgr.getInstance().ensureCreateOrUpdateContainerTypeRights();

			int count = 0;
			List<ContainerType> types = getTypesOrderedByDependents(new ArrayList<>(req.getPayload().getIds()));
			Collections.reverse(types);
			for (ContainerType type : types) {
				type.delete();
				daoFactory.getContainerTypeDao().saveOrUpdate(type, true);
				++count;
			}

			return ResponseEvent.response(count);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public String getObjectName() {
		return ContainerType.getEntityName();
	}

	@Override
	public Map<String, Object> resolveUrl(String key, Object value) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public String getAuditTable() {
		return "OS_CONTAINER_TYPES_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		getContainerType(id, null); // ensures container type exists
		AccessCtrlMgr.getInstance().ensureReadContainerTypeRights();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exportSvc.registerObjectsGenerator("storageContainerType", this::getTypesGenerator);
	}

	private ContainerType getContainerType(Long id, String name) {
		ContainerType containerType = null;
		Object key = null;
		if (id != null) {
			containerType = daoFactory.getContainerTypeDao().getById(id);
			key = id;
		} else if (StringUtils.isNotBlank(name)) {
			containerType = daoFactory.getContainerTypeDao().getByName(name);
			key = name;
		}
		
		if (containerType == null) {
			throw OpenSpecimenException.userError(ContainerTypeErrorCode.NOT_FOUND, key);
		}
		
		return containerType;
	}
	
	private void ensureUniqueConstraints(ContainerType existing, ContainerType newContainerType) {
		if (existing != null && existing.getName().equals(newContainerType.getName())) {
			return;
		}
		
		ContainerType containerType = daoFactory.getContainerTypeDao().getByName(newContainerType.getName());
		if (containerType != null) {
			throw OpenSpecimenException.userError(ContainerTypeErrorCode.DUP_NAME, newContainerType.getName());
		}
	}

	private ResponseEvent<ContainerTypeDetail> updateContainerType(RequestEvent<ContainerTypeDetail> req, boolean partial) {
		try {
			AccessCtrlMgr.getInstance().ensureCreateOrUpdateContainerTypeRights();

			ContainerTypeDetail input = req.getPayload();
			ContainerType existing = getContainerType(input.getId(), input.getName());
			ContainerType containerType = containerTypeFactory.createContainerType(input, partial ? existing : null);
			ensureUniqueConstraints(existing, containerType);

			existing.update(containerType);
			daoFactory.getContainerTypeDao().saveOrUpdate(existing);
			return ResponseEvent.response(ContainerTypeDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private List<ContainerType> getTypesOrderedByDependents(List<Long> ids) {
		List<ContainerType> types = daoFactory.getContainerTypeDao().getByIds(ids);
		types.sort(Comparator.comparingInt(t -> ids.indexOf(t.getId())));

		Set<Long> dependents = new HashSet<>();
		Map<Long, List<Long>> parentsMap = new LinkedHashMap<>();
		for (ContainerType type : types) {
			if (type.getCanHold() == null || ids.indexOf(type.getCanHold().getId()) == -1) {
				continue;
			}

			dependents.add(type.getId());
			List<Long> parents = parentsMap.computeIfAbsent(type.getCanHold().getId(), (k) -> new ArrayList<>());
			parents.add(type.getId());
		}

		List<ContainerType> result = new ArrayList<>();
		while (!types.isEmpty()) {
			ContainerType type = types.remove(0);
			if (dependents.contains(type.getId())) {
				types.add(type);
				continue;
			}

			result.add(type);

			List<Long> parents = parentsMap.get(type.getId());
			if (parents != null) {
				dependents.removeAll(parents);
			}
		}

		return result;
	}


	private Function<ExportJob, List<? extends Object>> getTypesGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean paramsInited;

			private boolean endOfTypes;

			private List<Long> leafTypeIds;

			private ContainerTypeListCriteria typeCrit = new ContainerTypeListCriteria().maxResults(100000);

			@Override
			public List<ContainerTypeDetail> apply(ExportJob job) {
				initParams();

				if (endOfTypes) {
					return Collections.emptyList();
				}

				if (CollectionUtils.isNotEmpty(job.getRecordIds())) {
					endOfTypes = true;
					return getTypesOrderedByDependents(job.getRecordIds()).stream()
						.map(ContainerTypeDetail::from).collect(Collectors.toList());
				}

				if (leafTypeIds == null) {
					leafTypeIds = daoFactory.getContainerTypeDao().getLeafTypeIds();
				}

				if (leafTypeIds.isEmpty()) {
					return Collections.emptyList();
				}

				int maxIdx = leafTypeIds.size() > 100 ? 100 : leafTypeIds.size();
				List<Long> ids = leafTypeIds.subList(0, maxIdx);
				leafTypeIds = leafTypeIds.subList(maxIdx, leafTypeIds.size());

				List<ContainerType> leafTypes = daoFactory.getContainerTypeDao().getByIds(ids);
				leafTypes.sort(Comparator.comparingInt(c -> ids.indexOf(c.getId())));

				Map<Long, List<ContainerType>> parentsMap = new HashMap<>();
				List<ContainerType> types = leafTypes;
				while (!types.isEmpty()) {
					typeCrit.canHold(types.stream().map(ContainerType::getName).collect(Collectors.toList()));
					types = daoFactory.getContainerTypeDao().getTypes(typeCrit);

					for (ContainerType type : types) {
						List<ContainerType> parents = parentsMap.computeIfAbsent(type.getCanHold().getId(), (k) -> new ArrayList<>());
						parents.add(type);
					}
				}

				List<ContainerTypeDetail> result = new ArrayList<>();
				for (ContainerType leafType : leafTypes) {
					List<ContainerType> workList = new ArrayList<>();
					workList.add(leafType);

					while (!workList.isEmpty()) {
						ContainerType type = workList.remove(0);
						result.add(ContainerTypeDetail.from(type));

						List<ContainerType> parents = parentsMap.get(type.getId());
						if (parents != null) {
							workList.addAll(0, parents);
						}
					}
				}

				return result;
			}

			private void initParams() {
				if (paramsInited) {
					return;
				}

				endOfTypes = !AccessCtrlMgr.getInstance().hasStorageContainerEximRights();
				paramsInited = true;
			}
		};
	}
}
