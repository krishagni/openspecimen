package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.ContainerSpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.TransactionCache;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class ContainerSpecimensImporter implements ObjectImporter<ContainerSpecimenDetail, ContainerSpecimenDetail> {



	private DaoFactory daoFactory;

	private SpecimenService spmnSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpmnSvc(SpecimenService spmnSvc) {
		this.spmnSvc = spmnSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerSpecimenDetail> importObject(RequestEvent<ImportObjectDetail<ContainerSpecimenDetail>> req) {
		try {
			ImportObjectDetail<ContainerSpecimenDetail> importObj = req.getPayload();
			StorageLocationSummary location = importObj.getObject().getLocation();
			SpecimenDetail spmn = importObj.getObject().getSpecimen();

			StorageContainer container = null;
			if (location == null) {
				return ResponseEvent.userError(SpecimenErrorCode.LOC_NOT_SPECIFIED);
			} else if (StringUtils.isNotBlank(location.getName())) {
				String name = location.getName().toLowerCase();
				container = getContainersByName().get(name);
				if (container == null && !getContainersByName().containsKey(name)) {
					container = daoFactory.getStorageContainerDao().getByName(location.getName());
					getContainersByName().put(name, container);
				}

				if (container == null) {
					return ResponseEvent.userError(StorageContainerErrorCode.NOT_FOUND, location.getName(), 1);
				}
			} else if (StringUtils.isNotBlank(location.getBarcode())) {
				String barcode = location.getBarcode().toLowerCase();
				String name = getBarcodeToName().get(barcode);
				if (name != null) {
					container = getContainersByName().get(name);
				} else if (!getBarcodeToName().containsKey(barcode)) {
					container = daoFactory.getStorageContainerDao().getByBarcode(location.getBarcode());
					getBarcodeToName().put(barcode, container != null ? container.getName().toLowerCase() : null);
					if (container != null) {
						getContainersByName().put(container.getName().toLowerCase(), container);
					}
				}

				if (container == null) {
					return ResponseEvent.userError(StorageContainerErrorCode.NOT_FOUND, location.getBarcode(), 1);
				}
			}

			if (container == null) {
				return ResponseEvent.userError(StorageContainerErrorCode.NAME_REQUIRED);
			}

			if (container.isDimensionless()) {
				return ResponseEvent.userError(SpecimenErrorCode.DIMLESS_CONTAINER);
			}

			String row = location.getPositionY();
			String column = location.getPositionX();
			if ((StringUtils.isBlank(row) || StringUtils.isBlank(column)) && location.getPosition() != null) {
				Pair<Integer, Integer> pos = container.getPositionAssigner().fromPosition(container, location.getPosition());
				row = container.toRowLabelingScheme(pos.first());
				column = container.toColumnLabelingScheme(pos.second());
			}

			if (StringUtils.isBlank(row) || StringUtils.isBlank(column)) {
				return ResponseEvent.userError(SpecimenErrorCode.LOC_NOT_SPECIFIED);
			}

			if (!container.areValidPositions(column, row)) {
				return ResponseEvent.userError(StorageContainerErrorCode.INV_POS, container.getName(), column, row);
			}

			Long specimenId = daoFactory.getStorageContainerPositionDao()
				.getSpecimenIdByPosition(container.getId(), row, column);
			if (specimenId == null) {
				return ResponseEvent.userError(SpecimenErrorCode.NO_SPMN_AT_LOC);
			}

			spmn.setId(specimenId);
			SpecimenDetail result = ResponseEvent.unwrap(spmnSvc.updateSpecimen(RequestEvent.wrap(spmn)));
			return ResponseEvent.response(new ContainerSpecimenDetail(result.getStorageLocation(), result));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private Map<String, StorageContainer> getContainersByName() {
		return TransactionCache.getInstance().get("containersByName", new HashMap<>());
	}

	private Map<String, String> getBarcodeToName() {
		return TransactionCache.getInstance().get("containerBarcodesToName", new HashMap<>());
	}

}
