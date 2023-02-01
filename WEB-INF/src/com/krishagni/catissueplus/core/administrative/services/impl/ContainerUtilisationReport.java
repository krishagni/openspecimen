package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.repository.ContainerReportCriteria;
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
		return generate(Collections.singletonList(container), params);
	}

	@Override
	public ExportedFileDetail generate(List<StorageContainer> containers, Object... params) {
		ContainerReportCriteria rptCrit = params != null && params.length > 0 ? (ContainerReportCriteria) params[0] : null;
		File file = generateReport(containers, rptCrit);

		// <zip>_<uuid>_<userId>_<name>
		String name = getFilename(containers.size() == 1 ? containers.get(0) : null);
		String zipFilename = getZipFileId(name, file.getName());
		String entryName = name + ".csv";
		Pair<String, String> zipEntry = Pair.make(file.getAbsolutePath(), entryName);
		File zipFile = new File(ConfigUtil.getInstance().getReportsDir(), zipFilename + ".zip");
		Utility.zipFilesWithNames(Collections.singletonList(zipEntry), zipFile.getAbsolutePath());
		file.delete();
		return new ExportedFileDetail(zipFilename, zipFile);
	}

	@Override
	public String getFilenamePrefix() {
		return "utilisation_report";
	}

	private File generateReport(List<StorageContainer> input, ContainerReportCriteria rptCrit) {
		File file = null;
		CsvWriter writer = null;

		try {
			String fileId = UUID.randomUUID().toString();
			file = new File(ConfigUtil.getInstance().getReportsDir(), fileId);
			writer = CsvFileWriter.createCsvFileWriter(file);
			exportHeaders(input.size() == 1 ? input.get(0) : null, writer);

			List<Long> containerIds = input.stream().map(StorageContainer::getId).collect(Collectors.toList());
			Map<Long, StorageContainerSummary> containersMap  = getContainerDetails(containerIds).stream()
				.collect(Collectors.toMap(StorageContainerSummary::getId, c -> c));
			List<StorageContainerSummary> utilisations = getUtilisations(containersMap.values(), rptCrit.types());
			for (StorageContainerSummary utilisation : utilisations) {
				StorageContainerSummary container = containersMap.get(utilisation.getId());
				container.setNoOfRows(utilisation.getNoOfRows());
				container.setNoOfColumns(utilisation.getNoOfColumns());
				container.setUsedPositions(utilisation.getUsedPositions());
			}

			int count = 0;
			Set<String> cps = rptCrit.cps() != null ? new HashSet<>(rptCrit.cps()) : null;

			List<Long> containersList = new ArrayList<>(containersMap.keySet());
			while (!containersList.isEmpty()) {
				Long parentId = containersList.remove(0);
				StorageContainerSummary parent = containersMap.remove(parentId);
				if (CollectionUtils.isEmpty(rptCrit.types()) || (parent.getTypeName() != null && rptCrit.types().contains(parent.getTypeName()))) {
					writeToCsv(parent, writer);
					++count;
					if (count % 25 == 0) {
						writer.flush();
					}
				}

				Map<Long, StorageContainerSummary> childrenMap = getChildContainers(parentId, cps, rptCrit.siteCps());
				if (!childrenMap.isEmpty()) {
					utilisations = getUtilisations(childrenMap.values(), rptCrit.types());
					for (StorageContainerSummary s : utilisations) {
						StorageContainerSummary child = childrenMap.get(s.getId());
						child.setNoOfRows(s.getNoOfRows());
						child.setNoOfColumns(s.getNoOfColumns());
						child.setUsedPositions(s.getUsedPositions());
					}
				}

				containersList.addAll(0, childrenMap.keySet());
				containersMap.putAll(childrenMap);
			}
		} catch (OpenSpecimenException ose) {
			if (writer != null) {
				writer.writeNext(new String[] { ose.getMessage() });
			}

			if (input.size() == 1) {
				logger.error("Error exporting utilisation of container: " + input.get(0).getName() + ": " + ose.getMessage(), ose);
			} else {
				logger.error("Error exporting utilisation of containers", ose);
			}
		} catch (Exception e) {
			if (writer != null) {
				writer.writeNext(new String[] { ExceptionUtils.getStackTrace(e) });
			}

			if (input.size() == 1) {
				logger.error("Error exporting utilisation of container: " + input.get(0).getName() + ": " + e.getMessage(), e);
			} else {
				logger.error("Error exporting utilisation of containers", e);
			}
		} finally {
			IOUtils.closeQuietly(writer);
		}

		return file;
	}

	@PlusTransactional
	private void exportHeaders(StorageContainer container, CsvWriter writer) {
		if (container != null) {
			container = daoFactory.getStorageContainerDao().getById(container.getId());
		}
		exportContainerSummary(container, writer);
		writer.writeNext(new String[] {
			message(CONTAINER_NAME),
			message(CONTAINER_BARCODE),
			message(CONTAINER_DISPLAY_NAME),
			message(FREEZER_NAME),
			message(FREEZER_BARCODE),
			message(FREEZER_DISPLAY_NAME),
			message(CONTAINER_TYPE),
			message(CONTAINER_PROTOCOL),
			message(CONTAINER_STATUS),
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
	private List<StorageContainerSummary> getContainerDetails(List<Long> containerIds) {
		return daoFactory.getStorageContainerDao().fetchReportDetails(containerIds);
	}

	@PlusTransactional
	private List<StorageContainerSummary> getUtilisations(Collection<StorageContainerSummary> containers, Collection<String> types) {
		if (CollectionUtils.isNotEmpty(types)) {
			containers = containers.stream().filter(c -> c.getTypeName() != null && types.contains(c.getTypeName())).collect(Collectors.toList());
		}

		if (containers.isEmpty()) {
			return Collections.emptyList();
		}

		return daoFactory.getStorageContainerDao().getUtilisations(containers.stream().map(StorageContainerSummary::getId).collect(Collectors.toList()));
	}

	private void writeToCsv(StorageContainerSummary container, CsvWriter writer) {
		boolean dimensionless = container.getNoOfRows() == null || container.getNoOfColumns() == null;
		Integer freePositions = dimensionless ? null : container.getNoOfRows() * container.getNoOfColumns() - container.getUsedPositions();
		writer.writeNext(new String[] {
			container.getName(), container.getBarcode(), container.getDisplayName(),
			container.getFreezerName(), container.getFreezerBarcode(), container.getFreezerDisplayName(),
			container.getTypeName(),
			container.getCalcAllowedCollectionProtocols().stream().sorted().collect(Collectors.joining(", ")),
			container.getStatus() != null ? message("storage_container_status_" + container.getStatus()) : container.getActivityStatus(),
			dimensionless ? null : container.getNoOfRows().toString(),
			dimensionless ? null : container.getNoOfColumns().toString(),
			container.getUsedPositions().toString(),
			freePositions != null ? freePositions.toString() : null
		});
	}

	@PlusTransactional
	private Map<Long, StorageContainerSummary> getChildContainers(Long parentId, Set<String> cpShortTitles, Set<SiteCpPair> readAccessSites) {
		StorageContainerListCriteria crit = new StorageContainerListCriteria()
			.parentContainerId(parentId != null ? parentId : null)
			.siteCps(readAccessSites)
			.cpShortTitles(cpShortTitles)
			.startAt(0)
			.maxResults(10000);

		List<StorageContainer> childContainers = daoFactory.getStorageContainerDao().getStorageContainers(crit);
		if (childContainers.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Long> sortedContainerIds = childContainers.stream()
			.sorted(
				(c1, c2) -> {
					boolean c1HasPos = c1.getPosition() != null && c1.getPosition().getPosition() != null;
					boolean c2HasPos = c2.getPosition() != null && c2.getPosition().getPosition() != null;
					if (c1HasPos && c2HasPos) {
						return c1.getPosition().getPosition().compareTo(c2.getPosition().getPosition());
					} else if (c1HasPos) {
						return -1;
					} else if (c2HasPos) {
						return 1;
					} else {
						return c1.getId().compareTo(c2.getId());
					}
				}
			)
			.map(StorageContainer::getId)
			.collect(Collectors.toList());

		Map<Long, StorageContainerSummary> containers = daoFactory.getStorageContainerDao()
			.fetchReportDetails(sortedContainerIds).stream()
			.collect(Collectors.toMap(c -> c.getId(), c -> c));

		return sortedContainerIds.stream()
			.map(containerId -> containers.get(containerId))
			.collect(Collectors.toMap(c -> c.getId(), c -> c, (k1, k2) -> k1, LinkedHashMap::new));
	}

	private static final String CONTAINER_ROWS = "container_no_of_rows";

	private static final String CONTAINER_COLS = "container_no_of_columns";

	private static final String CONTAINER_UTILISED_POS = "container_utilised_positions";

	private static final String CONTAINER_FREE_POS = "container_free_positions";
}
