package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;


import com.krishagni.catissueplus.core.administrative.domain.PositionAssigner;
import com.krishagni.catissueplus.core.administrative.domain.RowMajorPositionAssigner;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.services.ContainerReport;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class ContainerEmptyPositionsReport extends AbstractContainerReport implements ContainerReport {
	private static final LogUtil logger = LogUtil.getLogger(ContainerEmptyPositionsReport.class);

	@Override
	public String getName() {
		return "container_empty_positions_report";
	}

	@Override
	public ExportedFileDetail generate(StorageContainer container, Object... params) {
		File file = exportEmptySlotsReport(container);

		// <zip>_<uuid>_<userId>_<name>
		String zipFilename = getZipFileId(container, file.getName());
		String entryName = container.getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + ".csv";
		Pair<String, String> zipEntry = Pair.make(file.getAbsolutePath(), entryName);
		File zipFile = new File(ConfigUtil.getInstance().getReportsDir(), zipFilename + ".zip");
		Utility.zipFilesWithNames(Collections.singletonList(zipEntry), zipFile.getAbsolutePath());
		file.delete();
		return new ExportedFileDetail(zipFilename, zipFile);
	}

	private File exportEmptySlotsReport(StorageContainer container) {
		File file = null;
		CsvWriter writer = null;

		try {
			String fileId = UUID.randomUUID().toString();
			file = new File(ConfigUtil.getInstance().getReportsDir(), fileId);
			writer = CsvFileWriter.createCsvFileWriter(file);

			List<Long> containersList = new ArrayList<>();
			containersList.add(container.getId());

			boolean firstContainer = true;
			while (!containersList.isEmpty()) {
				Long containerId = containersList.remove(0);
				exportEmptySlots(containerId, writer, firstContainer);
				containersList.addAll(0, getChildContainers(containerId));
				writer.flush();

				firstContainer = false;
			}
		} catch (OpenSpecimenException ose) {
			if (writer != null) {
				writer.writeNext(new String[] { ose.getMessage() });
			}

			logger.error("Error exporting the empty slots of container: " + container.getName() + ": " + ose.getMessage(), ose);
		} catch (Exception e) {
			if (writer != null) {
				writer.writeNext(new String[] { ExceptionUtils.getStackTrace(e) });
			}

			logger.error("Error exporting the empty slots of container: " + container.getName(), e);
		} finally {
			IOUtils.closeQuietly(writer);
		}

		return file;
	}

	@PlusTransactional
	private void exportEmptySlots(Long containerId, CsvWriter writer, boolean firstContainer) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerId);
		try {
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		} catch (OpenSpecimenException ose) {
			if (ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
				return;
			}

			throw ose;
		}

		if (firstContainer) {
			exportContainerSummary(container, writer);
			writer.writeNext(new String[] {
				message(CONTAINER_NAME),
				message(CONTAINER_HIERARCHY),
				message(CONTAINER_TYPE),
				message(STORES_SPMN),
				message(CELL_ROW),
				message(CELL_COL),
				message(CELL_POS)
			});
		}

		if (container.isDimensionless()) {
			return;
		}

		PositionAssigner pa = container.getPositionAssigner();
		boolean rowMajor = pa instanceof RowMajorPositionAssigner;

		Map<Integer, StorageContainerPosition> occupantsMap =
			container.getOccupiedPositions().stream().collect(
				Collectors.toMap(
					pos -> pa.toPosition(container, pos.getPosTwoOrdinal(), pos.getPosOneOrdinal()),
					pos -> pos
				)
			);

		String name = container.getName();
		String hierarchy = container.getStringifiedAncestors();
		String type = container.getType() != null ? container.getType().getName() : null;
		String storesSpmn = container.isStoreSpecimenEnabled() ? message(YES) : message(NO);
		for (int r = 1; r <= container.getNoOfRows(); ++r) {
			for (int c = 1; c <= container.getNoOfColumns(); ++c) {
				Pair<Integer, Integer> cood = pa.fromMapIdx(container, r - 1, c - 1);
				Integer pos = pa.toPosition(container, cood.first(), cood.second());
				StorageContainerPosition occupant = occupantsMap.get(pos);
				if (occupant != null) {
					continue;
				}

				String row    = container.toRowLabelingScheme(rowMajor ? cood.first() : cood.second());
				String column = container.toColumnLabelingScheme(rowMajor ? cood.second() : cood.first());
				writer.writeNext(new String[] { name, hierarchy, type, storesSpmn, row, column, pos.toString()});
			}
		}
	}

	@PlusTransactional
	private List<Long> getChildContainers(Long containerId) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerId);
		return daoFactory.getStorageContainerDao().getChildContainers(container)
			.stream().map(StorageContainerSummary::getId)
			.collect(Collectors.toList());
	}
}
