package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;


import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerReport;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ContainerUtilisationReport extends AbstractContainerReport implements ContainerReport {
	private static final LogUtil logger = LogUtil.getLogger(ContainerEmptyPositionsReport.class);

	@Override
	public String getName() {
		return "container_utilisation_report";
	}

	@Override
	public ExportedFileDetail generate(StorageContainer container, Object... params) {
		File file = generateReport(container);

		// <zip>_<uuid>_<userId>_<name>
		String zipFilename = getZipFileId(container, file.getName());
		Pair<String, String> zipEntry = Pair.make(file.getAbsolutePath(), container.getName() + ".csv");
		File zipFile = new File(ConfigUtil.getInstance().getReportsDir(), zipFilename + ".zip");
		Utility.zipFilesWithNames(Collections.singletonList(zipEntry), zipFile.getAbsolutePath());
		file.delete();
		return new ExportedFileDetail(zipFilename, zipFile);
	}

	private File generateReport(StorageContainer container) {
		File file = null;
		CsvWriter writer = null;

		try {
			String fileId = UUID.randomUUID().toString();
			file = new File(ConfigUtil.getInstance().getReportsDir(), fileId);
			writer = CsvFileWriter.createCsvFileWriter(file);
			exportHeaders(container, writer);

			List<StorageContainerSummary> utilisations = getUtilisations(Collections.singletonList(container.getId()));
			Map<Long, StorageContainerSummary> containersMap = new HashMap<>();
			containersMap.put(container.getId(), utilisations.get(0));

			int count = 0;
			Set<SiteCpPair> readAccessSites = getReadAccessSites();

			List<Long> containersList = new ArrayList<>();
			containersList.add(container.getId());
			while (!containersList.isEmpty()) {
				Long containerId = containersList.remove(0);
				writeToCsv(containersMap.remove(containerId), writer);
				++count;
				if (count % 25 == 0) {
					writer.flush();
				}

				List<Long> childContainerIds = getChildContainers(containerId, readAccessSites);
				if (!childContainerIds.isEmpty()) {
					utilisations = getUtilisations(childContainerIds);
					utilisations.forEach(s -> containersMap.put(s.getId(), s));
				}

				containersList.addAll(0, childContainerIds);
			}
		} catch (OpenSpecimenException ose) {
			if (writer != null) {
				writer.writeNext(new String[] { ose.getMessage() });
			}

			logger.error("Error exporting utilisation of container: " + container.getName() + ": " + ose.getMessage(), ose);
		} catch (Exception e) {
			if (writer != null) {
				writer.writeNext(new String[] { ExceptionUtils.getStackTrace(e) });
			}

			logger.error("Error exporting utilisation of container: " + container.getName(), e);
		} finally {
			IOUtils.closeQuietly(writer);
		}

		return file;
	}

	@PlusTransactional
	private void exportHeaders(StorageContainer container, CsvWriter writer) {
		container = daoFactory.getStorageContainerDao().getById(container.getId());
		exportContainerSummary(container, writer);
		writer.writeNext(new String[] {
			message(CONTAINER_NAME),
			message(CONTAINER_ROWS),
			message(CONTAINER_COLS),
			message(CONTAINER_UTILISED_POS),
			message(CONTAINER_FREE_POS)
		});
	}

	@PlusTransactional
	private Set<SiteCpPair> getReadAccessSites() {
		return AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
	}

	@PlusTransactional
	private List<StorageContainerSummary> getUtilisations(List<Long> containerIds) {
		return daoFactory.getStorageContainerDao().getUtilisations(containerIds);
	}

	@PlusTransactional
	private void writeToCsv(StorageContainerSummary container, CsvWriter writer) {
		boolean dimensionless = container.getNoOfRows() == null || container.getNoOfColumns() == null;
		Integer freePositions = dimensionless ? null : container.getNoOfRows() * container.getNoOfColumns() - container.getUsedPositions();
		writer.writeNext(new String[] {
			container.getName(),
			dimensionless ? null : container.getNoOfRows().toString(),
			dimensionless ? null : container.getNoOfColumns().toString(),
			container.getUsedPositions().toString(),
			freePositions != null ? freePositions.toString() : null
		});
	}

	@PlusTransactional
	private List<Long> getChildContainers(Long containerId, Set<SiteCpPair> readAccessSites) {
		StorageContainerListCriteria crit = new StorageContainerListCriteria()
			.parentContainerId(containerId)
			.siteCps(readAccessSites)
			.startAt(0)
			.maxResults(10000);

		List<StorageContainer> childContainers = daoFactory.getStorageContainerDao().getStorageContainers(crit);
		if (childContainers.isEmpty()) {
			return Collections.emptyList();
		}

		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerId);
		return StorageContainer.sort(container, childContainers).stream()
			.map(StorageContainer::getId)
			.collect(Collectors.toList());
	}

	private static final String CONTAINER_ROWS = "container_no_of_rows";

	private static final String CONTAINER_COLS = "container_no_of_columns";

	private static final String CONTAINER_UTILISED_POS = "container_utilised_positions";

	private static final String CONTAINER_FREE_POS = "container_free_positions";
}
