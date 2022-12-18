package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.services.ContainerDefragmenter;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class DefaultContainerDefragmenter implements ContainerDefragmenter {
	private File outputFile;

	private CsvWriter writer;

	private Map<Long, ContainerInfo> containerInfo = new HashMap<>();

	private int movedSpmnsCnt;

	private boolean aliquotsInSameBox;

	@Autowired
	private DaoFactory daoFactory;

	private class ContainerInfo {
		List<Integer> emptyPositions;

		List<Long> occupiedPositionIds;

		Map<Long, List<Long>> aliquotsMap;
	}

	public DefaultContainerDefragmenter(File outputFile) {
		this(outputFile, false);
	}

	public DefaultContainerDefragmenter(File outputFile, boolean aliquotsInSameBox) {
		this.outputFile = outputFile;
		this.aliquotsInSameBox = aliquotsInSameBox;
	}

	@Override
	public int defragment(StorageContainer container) {
		try {
			createWriter(container);

			List<Long> leafContainers = getLeafContainers(container.getId());
			for (Long leafContainerId : leafContainers) {
				if (!isContainerFilled(leafContainerId)) {
					defragment(leafContainerId, leafContainers);
				}

				// clean up to free the memory
				cleanupContainerInfo(leafContainerId);
			}

			flush();
		} finally {
			IOUtils.closeQuietly(writer);
		}

		return movedSpmnsCnt;
	}

	@PlusTransactional
	private List<Long> getLeafContainers(Long containerId) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerId);
		if (container == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, containerId, 1);
		}

		List<Long> result = new ArrayList<>();
		getLeafContainers(container, result);
		return result;
	}

	private void getLeafContainers(StorageContainer container, List<Long> result) {
		if (!container.isDimensionless() && container.isStoreSpecimenEnabled() && container.getChildContainers().isEmpty()) {
			result.add(container.getId());
			return;
		}

		for (StorageContainer child : container.getChildContainersSortedByPosition()) {
			getLeafContainers(child, result);
		}
	}

	private boolean isContainerFilled(Long containerId) {
		if (!containerInfo.containsKey(containerId)) {
			loadContainerInfo(containerId);
		}

		return containerInfo.get(containerId).emptyPositions.isEmpty();
	}

	@PlusTransactional
	private void loadContainerInfo(Long containerId) {
		ContainerInfo info = containerInfo.get(containerId);
		if (info != null) {
			return;
		}

		StorageContainer container = getContainer(containerId);
		List<StorageContainerPosition> positions = container.getOccupiedPositions().stream()
			.sorted((p1, p2) -> p1.getPosition().compareTo(p2.getPosition()))
			.collect(Collectors.toList());

		info = new ContainerInfo();
		info.emptyPositions      = container.emptyPositionsOrdinals().stream()
			.sorted((p1, p2) -> p1.compareTo(p2))
			.collect(Collectors.toList());
		info.occupiedPositionIds = positions.stream()
			.filter(p -> !p.isBlocked())
			.map(StorageContainerPosition::getId)
			.collect(Collectors.toList());
		containerInfo.put(containerId, info);
	}

	// clean up to free the memory
	private void cleanupContainerInfo(Long containerId) {
		ContainerInfo info = containerInfo.remove(containerId);
		info.emptyPositions.clear();
		info.emptyPositions = null;

		info.occupiedPositionIds.clear();
		info.occupiedPositionIds = null;

		if (info.aliquotsMap != null) {
			info.aliquotsMap.clear();
			info.aliquotsMap = null;
		}
	}

	private void defragment(Long containerId, List<Long> leafContainerIds) {
		int srcIdx = -1;
		List<Integer> emptyPositions = containerInfo.get(containerId).emptyPositions;
		while (!emptyPositions.isEmpty()) {
			srcIdx = getNonEmptyContainerIdxFromLast(leafContainerIds, containerId, srcIdx);
			if (srcIdx == -1) {
				defragment0(containerId);
				return;
			}

			moveSpecimens(leafContainerIds.get(srcIdx), containerId);
		}
	}

	@PlusTransactional
	private void defragment0(Long containerId) {
		StorageContainer container = getContainer(containerId);
		List<Integer> emptyPositions = containerInfo.get(containerId).emptyPositions;
		List<StorageContainerPosition> occupiedPositions = getOccupiedPositions(container);

		int lastIdx = -1;
		while (!emptyPositions.isEmpty() && lastIdx < occupiedPositions.size() - 1) {
			++lastIdx;

			StorageContainerPosition oldPosition = occupiedPositions.get(lastIdx);
			if (emptyPositions.get(0) < oldPosition.getPosition()) {
				int emptyPosition = emptyPositions.remove(0);
				addToEmptyPositions(containerId, oldPosition.getPosition());
				write(container, oldPosition, container, emptyPosition);
			}
		}
	}

	@PlusTransactional
	private void moveSpecimens(Long srcContainerId, Long tgtContainerId) {
		StorageContainer tgtContainer = getContainer(tgtContainerId);
		List<Integer> tgtEmptyPositions = containerInfo.get(tgtContainerId).emptyPositions;

		StorageContainer srcContainer = getContainer(srcContainerId);
		List<Long> occupiedPositionIds = containerInfo.get(srcContainerId).occupiedPositionIds;
		List<StorageContainerPosition> occupiedPositions = getOccupiedPositions(srcContainer);
		if (aliquotsInSameBox && containerInfo.get(srcContainerId).aliquotsMap == null) {
			containerInfo.get(srcContainerId).aliquotsMap = getAliquotsMap(occupiedPositions);
		}

		for (int i = occupiedPositions.size() - 1; i >= 0 && !tgtEmptyPositions.isEmpty(); --i) {
			StorageContainerPosition position = occupiedPositions.get(i);
			if (!occupiedPositionIds.contains(position.getId())) {
				// might have got moved along with other aliquots of same parent
				continue;
			}

			List<StorageContainerPosition> toMoveList = new ArrayList<>();
			if (aliquotsInSameBox && position.getOccupyingSpecimen().isAliquot()) {
				if (tgtEmptyPositions.size() < getAliquotsCount(srcContainerId, position)) {
					continue;
				}

				toMoveList.addAll(getAliquots(srcContainerId, occupiedPositions, position));
			} else {
				toMoveList.add(position);
			}

			if (!canContainAll(tgtContainer, toMoveList)) {
				continue;
			}

			for (StorageContainerPosition toMove : toMoveList) {
				occupiedPositionIds.remove(toMove.getId());
				addToEmptyPositions(srcContainerId, toMove.getPosition());

				int tgtPos = tgtEmptyPositions.remove(0);
				write(srcContainer, toMove, tgtContainer, tgtPos);
			}
		}
	}

	private int getNonEmptyContainerIdxFromLast(List<Long> containerIds, Long containerId, int lastIdx) {
		int startIdx = lastIdx == -1 ? containerIds.size() - 1 : lastIdx - 1;
		for (int i = startIdx; i >= 0; --i) {
			Long r = containerIds.get(i);
			if (r.equals(containerId)) {
				return -1;
			} else if (hasSpecimens(r)) {
				return i;
			}
		}

		return -1;
	}

	private boolean hasSpecimens(Long containerId) {
		if (!containerInfo.containsKey(containerId)) {
			loadContainerInfo(containerId);
		}

		return !containerInfo.get(containerId).occupiedPositionIds.isEmpty();
	}

	private void addToEmptyPositions(Long containerId, int position) {
		List<Integer> emptyPositions = containerInfo.get(containerId).emptyPositions;

		int idx = 0;
		for (int ep : emptyPositions) {
			if (ep > position) {
				break;
			}

			idx++;
		}

		emptyPositions.add(idx, position);
	}

	private List<StorageContainerPosition> getOccupiedPositions(StorageContainer container) {
		List<Long> occupiedPosIds = containerInfo.get(container.getId()).occupiedPositionIds;
		return getPositions(container.getOccupiedPositions(), occupiedPosIds, true);
	}

	private List<StorageContainerPosition> getPositions(Collection<StorageContainerPosition> positions, Collection<Long> posIds, boolean forward) {
		int factor = forward ? 1 : -1;
		return positions.stream()
			.filter(pos -> posIds.contains(pos.getId()))
			.sorted((p1, p2) -> factor * p1.getPosition().compareTo(p2.getPosition()))
			.collect(Collectors.toList());
	}

	private StorageContainer getContainer(Long id) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(id);
		if (container == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, id, 1);
		}

		return container;
	}

	private Map<Long, List<Long>> getAliquotsMap(List<StorageContainerPosition> positions) {
		Map<Long, List<Long>> aliquotsMap = new HashMap<>();
		for (StorageContainerPosition position : positions) {
			Specimen spmn = position.getOccupyingSpecimen();
			if (spmn == null || !spmn.isAliquot()) {
				continue;
			}

			List<Long> ap = aliquotsMap.computeIfAbsent(spmn.getParentSpecimen().getId(), (k) -> new ArrayList<>());
			ap.add(position.getId());
		}

		return aliquotsMap;
	}

	private int getAliquotsCount(Long containerId, StorageContainerPosition pos) {
		Specimen spmn = pos.getOccupyingSpecimen();
		Map<Long, List<Long>> aliquotsMap = containerInfo.get(containerId).aliquotsMap;
		List<Long> aliquots = aliquotsMap.get(spmn.getParentSpecimen().getId());
		return aliquots != null ? aliquots.size() : 1;
	}

	private List<StorageContainerPosition> getAliquots(Long containerId, List<StorageContainerPosition> positions, StorageContainerPosition pos) {
		Specimen spmn = pos.getOccupyingSpecimen();
		Map<Long, List<Long>> aliquotsMap = containerInfo.get(containerId).aliquotsMap;
		List<Long> aliquots = aliquotsMap.get(spmn.getParentSpecimen().getId());
		return getPositions(positions, aliquots, true);
	}

	private boolean canContainAll(StorageContainer container, List<StorageContainerPosition> positions) {
		return positions.stream().map(StorageContainerPosition::getOccupyingSpecimen).allMatch(container::canContain);
	}

	private void createWriter(StorageContainer container) {
		String nameLabel         = msg("storage_container_name");
		String barcodeLabel      = msg("container_barcode");
		String displayNameLabel  = msg("storage_container_display_name");

		String currLocationLabel = msg("specimen_current_location");
		String locationLabel     = msg("specimen_location");
		String containerLabel    = msg("container");
		String containerBarcode  = msg("specimen_location_container_barcode");
		String rowLabel          = msg("specimen_location_row");
		String colLabel          = msg("specimen_location_column");
		String posLabel          = msg("specimen_location_position");

		writer = CsvFileWriter.createCsvFileWriter(outputFile);
		writer.writeNext(new String[] { "#" + msg("common_exported_by"), AuthUtil.getCurrentUser().formattedName() });
		writer.writeNext(new String[] { "#" + msg("common_exported_on"), Utility.getDateTimeString(Calendar.getInstance().getTime())});
		writer.writeNext(new String[] { "#" + nameLabel, container.getName() });
		writer.writeNext(new String[] { "#" + barcodeLabel, container.getBarcode() });
		writer.writeNext(new String[] { "#" + displayNameLabel, container.getDisplayName() });
		writer.writeNext(new String[] { "#" });

		writer.writeNext(new String[] {
			msg("specimen_identifier"), msg("specimen_label"), msg("specimen_barcode"),
			currLocationLabel + "#" + displayNameLabel, currLocationLabel + "#" + containerLabel, currLocationLabel + "#" + containerBarcode,
			currLocationLabel + "#" + rowLabel, currLocationLabel + "#" + colLabel, currLocationLabel + "#" + posLabel,
			locationLabel + "#" + displayNameLabel, locationLabel + "#" + containerLabel, locationLabel + "#" + containerBarcode,
			locationLabel + "#" + rowLabel, locationLabel + "#" + colLabel, locationLabel + "#" + posLabel
		});
	}

	private void write(StorageContainer srcContainer, StorageContainerPosition srcPos, StorageContainer tgtContainer, int tgtPos) {
		Specimen spmn = srcPos.getOccupyingSpecimen();
		Pair<Integer, Integer> tgtPosOrdinals = tgtContainer.getPositionAssigner().fromPosition(tgtContainer, tgtPos);
		String tgtRow = tgtContainer.toRowLabelingScheme(tgtPosOrdinals.first()); // pos 2
		String tgtCol = tgtContainer.toColumnLabelingScheme(tgtPosOrdinals.second()); // pos 1

		++movedSpmnsCnt;
		writer.writeNext(new String[] {
			spmn.getId().toString(), spmn.getLabel(), spmn.getBarcode(),
			srcContainer.getDisplayName(), srcContainer.getName(), srcContainer.getBarcode(),
			srcPos.getPosTwo(), srcPos.getPosOne(), srcPos.getPosition().toString(),
			tgtContainer.getDisplayName(), tgtContainer.getName(), tgtContainer.getBarcode(),
			tgtRow, tgtCol, String.valueOf(tgtPos)
		});
		flush();
	}

	private void flush() {
		if (writer == null) {
			return;
		}

		if (movedSpmnsCnt % 50 == 0) {
			writer.flush();
		}
	}

	private String msg(String key) {
		return MessageUtil.getInstance().getMessage(key);
	}
}
