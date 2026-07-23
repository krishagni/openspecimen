package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.krishagni.catissueplus.core.administrative.domain.PositionAssigner;
import com.krishagni.catissueplus.core.administrative.domain.RowMajorPositionAssigner;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerReport;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ContainerUtilisationMapReport extends AbstractContainerReport implements ContainerReport {
	private static final LogUtil logger = LogUtil.getLogger(ContainerUtilisationMapReport.class);

	//
	// number of rows to keep in memory before flushing them to the disk
	// once flushed, these rows cannot be accessed again
	//
	private static final int SHEET_WINDOW_SIZE = 50;

	@Override
	public String getName() {
		return "container_utilisation_map_report";
	}

	@Override
	public ExportedFileDetail generate(StorageContainer container, Object... params) {
		File file = exportUtilisationMap(container);

		String filename = getFilename(container);
		String zipFilename = getZipFileId(filename, UUID.randomUUID().toString());
		String entryName = filename + ".xlsx";
		Pair<String, String> zipEntry = Pair.make(file.getAbsolutePath(), entryName);
		File zipFile = new File(ConfigUtil.getInstance().getReportsDir(), zipFilename + ".zip");
		Utility.zipFilesWithNames(Collections.singletonList(zipEntry), zipFile.getAbsolutePath());
		file.delete();
		return new ExportedFileDetail(zipFilename, zipFile);
	}

	@Override
	public String getFilenamePrefix() {
		return "utilisation_map";
	}

	private File exportUtilisationMap(StorageContainer container) {
		File file = new File(ConfigUtil.getInstance().getReportsDir(), UUID.randomUUID() + ".xlsx");
		SXSSFWorkbook workbook = null;
		FileOutputStream out = null;

		try {
			workbook = new SXSSFWorkbook(SHEET_WINDOW_SIZE);
			workbook.setCompressTempFiles(true); // compress the raw XML flushed to disk on fly

			Map<String, CellStyle> styles = createStyles(workbook);

			Set<String> sheetNames = new HashSet<>(); // tracks unique sheet names
			Map<Long, SheetDetail> sheets = new LinkedHashMap<>(); // tracks sheet details in the spreadsheet order

			ensureUtilisationMapAvailable(container.getId());
			collectSheets(container.getId(), sheetNames, sheets);
			if (sheets.isEmpty()) {
				createMessageSheet(workbook, styles, message("storage_container_no_utilisation_maps"));
			} else {
				for (SheetDetail sheet : sheets.values()) {
					exportHeatMap(sheet, workbook, styles, sheets);
				}
			}

			out = new FileOutputStream(file);
			workbook.write(out);
		} catch (OpenSpecimenException ose) {
			logger.error("Error exporting utilisation map of container: " + container.getName() + ": " + ose.getMessage(), ose);
			throw ose;
		} catch (Exception e) {
			logger.error("Error exporting utilisation map of container: " + container.getName(), e);
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(workbook);
			if (workbook != null) {
				workbook.dispose();
			}
		}

		return file;
	}

	@PlusTransactional
	private void ensureUtilisationMapAvailable(Long containerId) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerId);
		if (container == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, containerId, 1);
		}

		if (container.isArchived()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.ARCHIVED, container.getName());
		}

		if (container.isDimensionless()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.DIMLESS_NO_UTILISATION_MAP, container.getName());
		}

		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

		StorageContainerListCriteria crit = new StorageContainerListCriteria().parentContainerId(container.getId());
		if (daoFactory.getStorageContainerDao().getStorageContainersCount(crit) == 0) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_CHILDREN_UTILISATION_MAP, container.getName());
		}
	}

	private void collectSheets(Long containerId, Set<String> sheetNames, Map<Long, SheetDetail> sheets) {
		Deque<Long> containers = new ArrayDeque<>();
		containers.push(containerId);

		while (!containers.isEmpty()) {
			SheetDetail detail = getSheet(containers.pop(), sheetNames);
			if (detail == null) {
				continue;
			}

			sheets.put(detail.containerId(), detail);

			List<Long> childIds = detail.childIds();
			for (int i = childIds.size() - 1; i >= 0; --i) {
				containers.push(childIds.get(i));
			}
		}
	}

	@PlusTransactional
	private SheetDetail getSheet(Long containerId, Set<String> sheetNames) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerId);
		if (container == null || container.isArchived() || container.isDimensionless()) {
			return null;
		}

		List<StorageContainerSummary> children = daoFactory.getStorageContainerDao().getChildContainers(container);
		if (CollectionUtils.isEmpty(children)) {
			return null;
		}

		String sheetName = getSheetName(sheetNames, getDisplayName(container));
		List<Long> childIds = children.stream().map(StorageContainerSummary::getId).filter(Objects::nonNull).collect(Collectors.toList());
		return new SheetDetail(container.getId(), sheetName, childIds);
	}

	@PlusTransactional
	private void exportHeatMap(SheetDetail detail, SXSSFWorkbook workbook, Map<String, CellStyle> styles, Map<Long, SheetDetail> sheets) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(detail.containerId);
		List<StorageContainerSummary> children = daoFactory.getStorageContainerDao().getChildContainers(container);
		if (CollectionUtils.isEmpty(children)) {
			return;
		}

		setUtilisationStats(container, children);

		PositionAssigner pa = container.getPositionAssigner();
		boolean rowMajor = pa instanceof RowMajorPositionAssigner;
		Map<Integer, StorageContainerSummary> childrenMap = children.stream()
			.filter(child -> child.getStorageLocation() != null && child.getStorageLocation().getPosition() != null)
			.collect(Collectors.toMap(child -> child.getStorageLocation().getPosition(), child -> child, (c1, c2) -> c1));

		SXSSFSheet sheet = workbook.createSheet(detail.sheetName);
		int startRow = writeContainerDetails(sheet, styles, container);
		Row header = sheet.createRow(startRow);
		createCell(header, 0, "", styles.get("header"));
		for (int c = 1; c <= container.getNoOfColumns(); ++c) {
			Pair<Integer, Integer> coord = pa.fromMapIdx(container, 0, c - 1);
			String label = rowMajor ? container.toColumnLabelingScheme(coord.second()) : container.toRowLabelingScheme(coord.first());
			createCell(header, c, label, styles.get("header"));
		}

		for (int r = 1; r <= container.getNoOfRows(); ++r) {
			Row row = sheet.createRow(startRow + r);
			row.setHeightInPoints(68);

			Pair<Integer, Integer> rowCoord = pa.fromMapIdx(container, r - 1, 0);
			String rowLabel = rowMajor ? container.toRowLabelingScheme(rowCoord.first()) : container.toColumnLabelingScheme(rowCoord.second());
			createCell(row, 0, rowLabel, styles.get("header"));

			for (int c = 1; c <= container.getNoOfColumns(); ++c) {
				Pair<Integer, Integer> posCoord = pa.fromMapIdx(container, r - 1, c - 1);
				Integer pos = pa.toPosition(container, posCoord.first(), posCoord.second());
				StorageContainerSummary child = childrenMap.get(pos);
				if (child == null) {
					createCell(row, c, "", styles.get("emptyCell"));
				} else {
					int total = child.getTotalPositions() != null ? child.getTotalPositions() : 0;
					int free  = child.getFreePositions() != null ? child.getFreePositions() : 0;
					int used  = total - free;
					int utilisation = total > 0 ? Math.round((float) used * 100 / total) : 0;
					Cell cell = createCell(row, c, getCellText(child, utilisation, used, total), styles.get(getUtilisationClass(utilisation)));
					addHyperlink(workbook, cell, child, sheets);
				}
			}
		}

		sheet.createFreezePane(1, startRow + 1);
		sheet.setColumnWidth(0, 18 * 256);
		for (int i = 1; i <= container.getNoOfColumns(); ++i) {
			sheet.setColumnWidth(i, 22 * 256);
		}
	}

	private void setUtilisationStats(StorageContainer container, List<StorageContainerSummary> children) {
		StorageContainerSummary.addEmptySlots(container, children);
		Map<Long, int[]> stats = daoFactory.getStorageContainerDao().getUtilisationStats(
			children.stream().map(StorageContainerSummary::getId).filter(Objects::nonNull).collect(Collectors.toList())
		);

		for (StorageContainerSummary child : children) {
			int[] stat = stats.get(child.getId());
			if (stat == null) {
				continue;
			}

			child.setUsedPositions(stat[0]);
			child.setFreePositions(stat[1]);
			child.setTotalPositions(stat[0] + stat[1]);
		}
	}

	private void createMessageSheet(SXSSFWorkbook workbook, Map<String, CellStyle> styles, String message) {
		SXSSFSheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(message("container_utilisation_map_report")));
		createCell(sheet.createRow(0), 0, message, styles.get("message"));
		sheet.autoSizeColumn(0);
	}

	private int writeContainerDetails(SXSSFSheet sheet, Map<String, CellStyle> styles, StorageContainer container) {
		int rowIdx = 0;
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_DETAILS), null);
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_NAME), container.getName());
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_BARCODE), container.getBarcode());
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_DISPLAY_NAME), container.getDisplayName());
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_HIERARCHY), container.getStringifiedAncestors());
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_SITE), container.getSite().getName());
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_RESTRICTIONS), null);
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_PROTOCOL), getCollectionProtocols(container));
		writeDetail(sheet, styles, rowIdx++, message(CONTAINER_SPECIMEN_TYPES), getSpecimenTypes(container));
		writeDetail(sheet, styles, rowIdx++, message(EXPORTED_BY), AuthUtil.getCurrentUser().formattedName());
		writeDetail(sheet, styles, rowIdx++, message(EXPORTED_ON), Utility.getDateTimeString(new Date()));
		sheet.setColumnWidth(0, 18 * 256);
		sheet.setColumnWidth(1, 34 * 256);
		return rowIdx + 2;
	}

	private void writeDetail(SXSSFSheet sheet, Map<String, CellStyle> styles, int rowIdx, String key, String value) {
		Row row = sheet.createRow(rowIdx);
		createCell(row, 0, key, styles.get("detailLabel"));
		createCell(row, 1, value, styles.get("detailValue"));
	}

	private String getSpecimenTypes(StorageContainer container) {
		List<String> types = new ArrayList<>();
		types.addAll(container.getCompAllowedSpecimenClasses().stream().map(pv -> pv.getValue()).sorted().toList());
		types.addAll(container.getCompAllowedSpecimenTypes().stream().map(pv -> pv.getValue()).sorted().toList());
		return types.isEmpty() ? message(ALL) : String.join(", ", types);
	}

	private String getCollectionProtocols(StorageContainer container) {
		if (CollectionUtils.isEmpty(container.getCompAllowedCps())) {
			return message(ALL);
		}

		return container.getCompAllowedCps().stream().map(CollectionProtocol::getTitle).sorted().collect(Collectors.joining(", "));
	}

	private void addHyperlink(SXSSFWorkbook workbook, Cell cell, StorageContainerSummary child, Map<Long, SheetDetail> sheets) {
		if (child.getId() == null) {
			return;
		}

		SheetDetail detail = sheets.get(child.getId());
		if (detail == null || StringUtils.isBlank(detail.sheetName())) {
			return;
		}

		Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.DOCUMENT);
		link.setAddress("'" + detail.sheetName().replace("'", "''") + "'!A1");
		cell.setHyperlink(link);
	}

	private String getCellText(StorageContainerSummary child, int utilisation, int used, int total) {
		String name;
		if (child.getId() == null) {
			name = message("storage_container_empty_container_slot", child.getTypeName());
		} else if (StringUtils.isNotBlank(child.getDisplayName())) {
			name = child.getDisplayName();
		} else {
			name = child.getName();
		}

		return name + "\n" + utilisation + "%\n" + used + " / " + total;
	}

	private String getUtilisationClass(int utilisation) {
		if (utilisation >= 100) {
			return "full";
		} else if (utilisation > 80) {
			return "high";
		} else if (utilisation > 50) {
			return "medium";
		} else if (utilisation > 0) {
			return "low";
		}

		return "empty";
	}

	private Map<String, CellStyle> createStyles(SXSSFWorkbook workbook) {
		Map<String, CellStyle> styles = new HashMap<>();
		styles.put("header",      createStyle(workbook, IndexedColors.GREY_25_PERCENT.getIndex(), IndexedColors.BLACK.getIndex(), true));
		styles.put("message",     createStyle(workbook, IndexedColors.WHITE.getIndex(), IndexedColors.BLACK.getIndex(), false));
		styles.put("detailLabel", createStyle(workbook, IndexedColors.GREY_25_PERCENT.getIndex(), IndexedColors.BLACK.getIndex(), true));
		styles.put("detailValue", createStyle(workbook, IndexedColors.WHITE.getIndex(), IndexedColors.BLACK.getIndex(), false));
		styles.put("emptyCell",   createStyle(workbook, IndexedColors.WHITE.getIndex(), IndexedColors.BLACK.getIndex(), false));
		styles.put("full",        createStyle(workbook, IndexedColors.DARK_RED.getIndex(), IndexedColors.WHITE.getIndex(), true));
		styles.put("high",        createStyle(workbook, IndexedColors.RED.getIndex(), IndexedColors.WHITE.getIndex(), true));
		styles.put("medium",      createStyle(workbook, IndexedColors.ORANGE.getIndex(), IndexedColors.WHITE.getIndex(), true));
		styles.put("low",         createStyle(workbook, IndexedColors.GREEN.getIndex(), IndexedColors.WHITE.getIndex(), true));
		styles.put("empty",       createStyle(workbook, IndexedColors.BLUE.getIndex(), IndexedColors.WHITE.getIndex(), true));
		return styles;
	}

	private CellStyle createStyle(SXSSFWorkbook workbook, short bgColor, short fontColor, boolean bold) {
		CellStyle style = workbook.createCellStyle();
		style.setFillForegroundColor(bgColor);
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setWrapText(true);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);

		Font font = workbook.createFont();
		font.setColor(fontColor);
		font.setBold(bold);
		style.setFont(font);
		return style;
	}

	private Cell createCell(Row row, int column, String value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value != null ? value : "");
		cell.setCellStyle(style);
		return cell;
	}

	private String getDisplayName(StorageContainer container) {
		return StringUtils.isNotBlank(container.getDisplayName()) ? container.getDisplayName() : container.getName();
	}

	//
	// Ensures the sheet name is within the permissible length - 31 characters
	// longer sheet names are truncated and a numeric suffix is added to make it unique
	//
	private String getSheetName(Set<String> sheetNames, String input) {
		String name = WorkbookUtil.createSafeSheetName(StringUtils.defaultIfBlank(input, "Container"));
		if (name.length() > 31) {
			name = name.substring(0, 31);
		}

		String result = name;
		for (int count = 1; !sheetNames.add(result); ++count) {
			String suffix = " " + count;
			int maxLen = 31 - suffix.length();
			result = name.substring(0, Math.min(name.length(), maxLen)) + suffix;
		}

		return result;
	}

	private record SheetDetail(Long containerId, String sheetName, List<Long> childIds) { }
}
