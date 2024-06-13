
package com.krishagni.catissueplus.core.administrative.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.repository.ContainerRestrictionsCriteria;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerDao;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.TransactionalThreadLocals;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.SchemeOrdinalConverterUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;

@Audited
public class StorageContainer extends BaseExtensionEntity {
	public enum PositionLabelingMode {
		NONE,
		LINEAR,
		TWO_D
	}

	public enum CellDisplayProp {
		SPECIMEN_PPID,
		SPECIMEN_LABEL,
		SPECIMEN_BARCODE
	}

	public enum PositionAssignment {
		HZ_TOP_DOWN_LEFT_RIGHT,
		HZ_TOP_DOWN_RIGHT_LEFT,
		HZ_BOTTOM_UP_LEFT_RIGHT,
		HZ_BOTTOM_UP_RIGHT_LEFT,
		VT_TOP_DOWN_LEFT_RIGHT,
		VT_TOP_DOWN_RIGHT_LEFT,
		VT_BOTTOM_UP_LEFT_RIGHT,
		VT_BOTTOM_UP_RIGHT_LEFT
	}

	public enum UsageMode {
		STORAGE,
		DISTRIBUTION
	}

	public enum Status {
		CHECKED_OUT
	}

	public static final String DUMMY_NAME = "dummy$os";

	private static final String ENTITY_NAME = "storage_container";

	private static final String DEF_SITE_CONT_NAME = "storage_container_site_cont_name";

	public static final String EXTN = "StorageContainerExtension";

	public static final String NUMBER_LABELING_SCHEME = "Numbers";

	public static final String UPPER_CASE_ALPHA_LABELING_SCHEME = "Alphabets Upper Case";

	public static final String LOWER_CASE_ALPHA_LABELING_SCHEME = "Alphabets Lower Case";

	public static final String UPPER_CASE_ROMAN_LABELING_SCHEME = "Roman Upper Case";

	public static final String LOWER_CASE_ROMAN_LABELING_SCHEME = "Roman Lower Case";

	private static final Map<PositionAssignment, PositionAssigner> POS_ASSIGNERS = new HashMap<PositionAssignment, PositionAssigner>() {
		private static final long serialVersionUID = -2190575701287414096L;

		{
			put(PositionAssignment.HZ_TOP_DOWN_LEFT_RIGHT,  new HzTopDownLeftRightPosAssigner());
			put(PositionAssignment.HZ_TOP_DOWN_RIGHT_LEFT,  new HzTopDownRightLeftPosAssigner());
			put(PositionAssignment.HZ_BOTTOM_UP_LEFT_RIGHT, new HzBottomUpLeftRightPosAssigner());
			put(PositionAssignment.HZ_BOTTOM_UP_RIGHT_LEFT, new HzBottomUpRightLeftPosAssigner());
			put(PositionAssignment.VT_TOP_DOWN_LEFT_RIGHT,  new VtTopDownLeftRightPosAssigner());
			put(PositionAssignment.VT_TOP_DOWN_RIGHT_LEFT,  new VtTopDownRightLeftPosAssigner());
			put(PositionAssignment.VT_BOTTOM_UP_LEFT_RIGHT, new VtBottomUpLeftRightPosAssigner());
			put(PositionAssignment.VT_BOTTOM_UP_RIGHT_LEFT, new VtBottomUpRightLeftPosAssigner());
		}
	};

	private static final ThreadLocal<Map<Long, StorageContainerPosition>> lastAssignedPositions =
		new ThreadLocal<Map<Long, StorageContainerPosition>>() {
			@Override
			protected Map<Long, StorageContainerPosition> initialValue() {
				TransactionalThreadLocals.getInstance().register(this);
				return new HashMap<>();
			}
		};

	private String name;

	private String barcode;

	private String displayName;

	private ContainerType type;

	private UsageMode usedFor;
	
	private Double temperature;
	
	private Integer noOfColumns;
	
	private Integer noOfRows;

	private PositionLabelingMode positionLabelingMode = PositionLabelingMode.TWO_D;

	private PositionAssignment positionAssignment = PositionAssignment.HZ_TOP_DOWN_LEFT_RIGHT;
	
	private String columnLabelingScheme = NUMBER_LABELING_SCHEME;
	
	private String rowLabelingScheme = NUMBER_LABELING_SCHEME;
	
	private Site site;

	private StorageContainer parentContainer;

	private CellDisplayProp cellDisplayProp;

	private User createdBy;

	private String activityStatus = com.krishagni.catissueplus.core.common.util.Status.ACTIVITY_STATUS_ACTIVE.getStatus();
	
	private String comments;

	private Integer capacity;

	private Boolean automated;

	private AutoFreezerProvider autoFreezerProvider;

	private Set<StorageContainer> childContainers = new LinkedHashSet<>();
	
	private Set<StorageContainer> ancestorContainers = new HashSet<>();
	
	private Set<StorageContainer> descendentContainers = new HashSet<>();
	
	//
	// all types of these specimen classes are allowed
	//
	private Set<PermissibleValue> allowedSpecimenClasses = new HashSet<>();
	
	private Set<PermissibleValue> allowedSpecimenTypes = new HashSet<>();
	
	private Set<CollectionProtocol> allowedCps = new HashSet<>();

	private Set<DistributionProtocol> allowedDps = new HashSet<>();
	
	private boolean storeSpecimenEnabled = false;
			
	private StorageContainerPosition position;

	private StorageContainerPosition blockedPosition;
	
	private Set<StorageContainerPosition> occupiedPositions = new HashSet<>();

	private Set<ContainerTransferEvent> transferEvents = new LinkedHashSet<>();

	private Status status;

	//
	// query capabilities
	//
	private StorageContainerStats stats;
	
	private Set<PermissibleValue> compAllowedSpecimenClasses = new HashSet<>();
	
	private Set<PermissibleValue> compAllowedSpecimenTypes = new HashSet<>();
	
	private Set<CollectionProtocol> compAllowedCps = new HashSet<>();

	private Set<DistributionProtocol> compAllowedDps = new HashSet<>();

	//
	// transfer event
	//
	private transient User transferredBy;

	private transient Date transferDate;

	private transient Boolean checkOut;

	//
	// position assignment changes
	//
	private transient Map<StorageContainerPosition, Pair<Integer, Integer>> newPositions;

	private transient boolean createStoreList = true;

	public StorageContainer() {
		ancestorContainers.add(this);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public ContainerType getType() {
		return type;
	}

	public void setType(ContainerType type) {
		this.type = type;
	}

	public UsageMode getUsedFor() {
		return usedFor;
	}

	public void setUsedFor(UsageMode usedFor) {
		this.usedFor = usedFor;
	}

	public boolean isDistributionContainer() {
		return UsageMode.DISTRIBUTION.equals(getUsedFor());
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Integer getNoOfColumns() {
		return noOfColumns;
	}

	public void setNoOfColumns(Integer noOfColumns) {
		this.noOfColumns = noOfColumns;
	}

	public Integer getNoOfRows() {
		return noOfRows;
	}

	public void setNoOfRows(Integer noOfRows) {
		this.noOfRows = noOfRows;
	}

	public boolean isDimensionless() {
		return noOfRows == null && noOfColumns == null;
	}

	public PositionLabelingMode getPositionLabelingMode() {
		return positionLabelingMode;
	}

	public void setPositionLabelingMode(PositionLabelingMode positionLabelingMode) {
		this.positionLabelingMode = positionLabelingMode;
	}

	public boolean usesLinearLabelingMode() {
		return PositionLabelingMode.LINEAR.equals(getPositionLabelingMode());
	}

	public PositionAssignment getPositionAssignment() {
		return positionAssignment;
	}

	public void setPositionAssignment(PositionAssignment positionAssignment) {
		this.positionAssignment = positionAssignment;
	}

	public PositionAssigner getPositionAssigner() {
		return POS_ASSIGNERS.get(getPositionAssignment());
	}

	public String getColumnLabelingScheme() {
		return columnLabelingScheme;
	}

	public void setColumnLabelingScheme(String columnLabelingScheme) {
		this.columnLabelingScheme = columnLabelingScheme;
	}

	public String getRowLabelingScheme() {
		return rowLabelingScheme;
	}

	public void setRowLabelingScheme(String rowLabelingScheme) {
		this.rowLabelingScheme = rowLabelingScheme;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public Institute getInstitute() {
		return site != null ? site.getInstitute() : null;
	}

	public StorageContainer getParentContainer() {
		return parentContainer;
	}

	public void setParentContainer(StorageContainer parentContainer) {
		if (this.equals(parentContainer)) {
			return;
		}
		
		StorageContainer currParent = getParentContainer();
		if (currParent != null && currParent.equals(parentContainer)) {
			return;
		}
		
		if (currParent != null) {
			getAncestorContainers().remove(currParent);
			getAncestorContainers().removeAll(currParent.getAncestorContainers());
			
			for (StorageContainer descendent : getDescendentContainers()) {
				descendent.getAncestorContainers().remove(currParent);
				descendent.getAncestorContainers().removeAll(currParent.getAncestorContainers());
			}			
		}
		
		this.parentContainer = parentContainer;
		if (parentContainer != null) {
			getAncestorContainers().add(parentContainer);
			getAncestorContainers().addAll(parentContainer.getAncestorContainers());
			for (StorageContainer descendent : getDescendentContainers()) {
				descendent.getAncestorContainers().add(parentContainer);
				descendent.getAncestorContainers().addAll(parentContainer.getAncestorContainers());
			}
		}
	}

	public CellDisplayProp getCellDisplayProp() {
		return cellDisplayProp;
	}

	public void setCellDisplayProp(CellDisplayProp cellDisplayProp) {
		this.cellDisplayProp = cellDisplayProp;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	public boolean isAutomated() {
		return BooleanUtils.isTrue(automated);
	}

	public void setAutomated(Boolean automated) {
		this.automated = automated;
	}

	@NotAudited
	public AutoFreezerProvider getAutoFreezerProvider() {
		return autoFreezerProvider;
	}

	public void setAutoFreezerProvider(AutoFreezerProvider autoFreezerProvider) {
		this.autoFreezerProvider = autoFreezerProvider;
	}

	public StorageContainerPosition getLastAssignedPos() {
		return lastAssignedPositions.get().get(getId());
	}

	public void setLastAssignedPos(StorageContainerPosition lastAssignedPos) {
		lastAssignedPositions.get().put(getId(), lastAssignedPos);
	}

	public User getTransferredBy() {
		return transferredBy;
	}

	public void setTransferredBy(User transferredBy) {
		this.transferredBy = transferredBy;
	}

	public Date getTransferDate() {
		return transferDate;
	}

	public void setTransferDate(Date transferDate) {
		this.transferDate = transferDate;
	}

	public Boolean getCheckOut() {
		return checkOut;
	}

	public void setCheckOut(Boolean checkOut) {
		this.checkOut = checkOut;
	}

	public boolean isCreateStoreList() {
		return createStoreList;
	}

	public void setCreateStoreList(boolean createStoreList) {
		this.createStoreList = createStoreList;
	}

	@NotAudited
	public Set<StorageContainer> getChildContainers() {
		return childContainers;
	}

	public void setChildContainers(Set<StorageContainer> childContainers) {
		this.childContainers = childContainers;
	}
	
	public void addChildContainer(StorageContainer container) {
		container.setParentContainer(this);
		childContainers.add(container);
	}

	@NotAudited
	public Set<StorageContainer> getAncestorContainers() {
		return ancestorContainers;
	}

	public void setAncestorContainers(Set<StorageContainer> ancestorContainers) {
		this.ancestorContainers = ancestorContainers;
	}

	@NotAudited
	public Set<StorageContainer> getDescendentContainers() {
		return descendentContainers;
	}

	public void setDescendentContainers(Set<StorageContainer> descendentContainers) {
		this.descendentContainers = descendentContainers;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public Set<PermissibleValue> getAllowedSpecimenClasses() {
		return allowedSpecimenClasses;
	}

	public void setAllowedSpecimenClasses(Set<PermissibleValue> allowedSpecimenClasses) {
		this.allowedSpecimenClasses = allowedSpecimenClasses;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public Set<PermissibleValue> getAllowedSpecimenTypes() {
		return allowedSpecimenTypes;
	}

	public void setAllowedSpecimenTypes(Set<PermissibleValue> allowedSpecimenTypes) {
		this.allowedSpecimenTypes = allowedSpecimenTypes;
	}

	public Set<CollectionProtocol> getAllowedCps() {
		return allowedCps;
	}

	public void setAllowedCps(Set<CollectionProtocol> allowedCps) {
		this.allowedCps = allowedCps;
	}

	public Set<DistributionProtocol> getAllowedDps() {
		return allowedDps;
	}

	public void setAllowedDps(Set<DistributionProtocol> allowedDps) {
		this.allowedDps = allowedDps;
	}

	public boolean isStoreSpecimenEnabled() {
		return storeSpecimenEnabled;
	}

	public void setStoreSpecimenEnabled(boolean storeSpecimenEnabled) {
		this.storeSpecimenEnabled = storeSpecimenEnabled;
	}

	public StorageContainerPosition getPosition() {
		return position;
	}

	public void setPosition(StorageContainerPosition position) {
		this.position = position;
	}

	public StorageContainerPosition getBlockedPosition() {
		return blockedPosition;
	}

	public void setBlockedPosition(StorageContainerPosition blockedPosition) {
		this.blockedPosition = blockedPosition;
	}

	@NotAudited
	public Set<StorageContainerPosition> getOccupiedPositions() {
		return occupiedPositions;
	}

	public void setOccupiedPositions(Set<StorageContainerPosition> occupiedPositions) {
		this.occupiedPositions = occupiedPositions;
	}

	@NotAudited
	public Set<ContainerTransferEvent> getTransferEvents() {
		return transferEvents;
	}

	public void setTransferEvents(Set<ContainerTransferEvent> transferEvents) {
		this.transferEvents = transferEvents;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@NotAudited
	public boolean isCheckedOut() {
		return status == Status.CHECKED_OUT;
	}

	@NotAudited
	public StorageContainerStats getStats() {
		return stats;
	}

	public void setStats(StorageContainerStats stats) {
		this.stats = stats;
	}

	@NotAudited
	public Set<PermissibleValue> getCompAllowedSpecimenClasses() {
		return compAllowedSpecimenClasses;
	}

	public void setCompAllowedSpecimenClasses(Set<PermissibleValue> compAllowedSpecimenClasses) {
		this.compAllowedSpecimenClasses = compAllowedSpecimenClasses;
	}

	@NotAudited
	public Set<PermissibleValue> getCompAllowedSpecimenTypes() {
		return compAllowedSpecimenTypes;
	}

	public void setCompAllowedSpecimenTypes(Set<PermissibleValue> compAllowedSpecimenTypes) {
		this.compAllowedSpecimenTypes = compAllowedSpecimenTypes;
	}

	@NotAudited
	public Set<CollectionProtocol> getCompAllowedCps() {
		return compAllowedCps;
	}

	public void setCompAllowedCps(Set<CollectionProtocol> compAllowedCps) {
		this.compAllowedCps = compAllowedCps;
	}

	@NotAudited
	public Set<DistributionProtocol> getCompAllowedDps() {
		return compAllowedDps;
	}

	public void setCompAllowedDps(Set<DistributionProtocol> compAllowedDps) {
		this.compAllowedDps = compAllowedDps;
	}

	public boolean isActive() {
		return com.krishagni.catissueplus.core.common.util.Status.isActiveStatus(getActivityStatus());
	}

	public boolean isArchived() {
		return com.krishagni.catissueplus.core.common.util.Status.isClosedStatus(getActivityStatus());
	}

	public boolean isDeleted() {
		return com.krishagni.catissueplus.core.common.util.Status.isDisabledStatus(getActivityStatus());
	}

	@Override
	public String getEntityType() {
		return EXTN;
	}

	public void update(StorageContainer other) {
		updateActivityStatus(other);
		if (isDeleted()) {
			return;
		}

		boolean hasParentChanged = false;
		if (getParentContainer() == null && other.getParentContainer() != null) {
			hasParentChanged = true;
		} else if (getParentContainer() != null && !getParentContainer().equals(other.getParentContainer())) {
			hasParentChanged = true;
		}
		
		setName(other.getName());
		setBarcode(other.getBarcode());
		setDisplayName(other.getDisplayName());
		setType(other.getType());
		setTemperature(other.getTemperature());
		updateCapacity(other);
		setPositionLabelingMode(other.getPositionLabelingMode());
		updateLabelingScheme(other);
		updatePositionAssignment(other);
		updateContainerLocation(other);
		setComments(other.getComments());
		updateAllowedSpecimenClassAndTypes(other, hasParentChanged);
		updateAllowedCps(other, hasParentChanged);
		updateAllowedDps(other, hasParentChanged);
		updateStoreSpecimenEnabled(other);
		updateCellDisplayProp(other);
		setExtension(other.getExtension());
	}

	public void updatePositionsIfChanged() {
		if (newPositions == null || newPositions.isEmpty()) {
			return;
		}

		for (Map.Entry<StorageContainerPosition, Pair<Integer, Integer>> me : newPositions.entrySet()) {
			StorageContainerPosition pos = me.getKey();
			Integer row = me.getValue().first();
			Integer col = me.getValue().second();

			pos.setPosTwoOrdinal(row);
			pos.setPosTwo(fromOrdinal(getRowLabelingScheme(), row));
			pos.setPosOneOrdinal(col);
			pos.setPosOne(fromOrdinal(getColumnLabelingScheme(), col));
		}

		newPositions = null;
	}

	public void moveTo(StorageContainer newContainer) {
		StorageContainerPosition pos = newContainer.nextAvailablePosition();
		pos.setOccupyingContainer(this);
		updateContainerLocation(newContainer.getSite(), newContainer, pos);
	}

	public void moveTo(Site newSite, StorageContainer newParent, StorageContainerPosition newPos) {
		updateContainerLocation(newSite, newParent, newPos);
	}

	public Integer freePositionsCount() {
		return isDimensionless() ? null : getNoOfColumns() * getNoOfRows() - getOccupiedPositions().size();
	}

	public boolean hasFreePositionsForReservation() {
		return hasFreePositionsForReservation(1);
	}

	public boolean hasFreePositionsForReservation(int freePositions) {
		Integer availablePositions = freePositionsCount();
		return availablePositions == null || availablePositions > (freePositions - 1);
	}

	public List<StorageContainer> getChildContainersSortedByPosition() {
		return sort(this, getChildContainers());
	}

	public Set<Integer> occupiedPositionsOrdinals() {
		if (isDimensionless()) {
			return Collections.emptySet();
		} else {
			return getOccupiedPositions().stream().map(StorageContainerPosition::getPosition).collect(Collectors.toSet());
		}
	}

	public Set<Integer> emptyPositionsOrdinals() {
		if (isDimensionless()) {
			return Collections.emptySet();
		}

		Set<Integer> occupiedPositions = occupiedPositionsOrdinals();
		if (occupiedPositions.size() >= getNoOfRows() * getNoOfColumns()) {
			return Collections.emptySet();
		}


		Set<Integer> emptyPositions = new HashSet<>();
		for (int ri = 0; ri < getNoOfRows(); ++ri) {
			for (int ci = 0; ci < getNoOfColumns(); ++ci) {
				int pos = getPositionAssigner().toPosition(this, ri + 1, ci + 1);
				if (!occupiedPositions.contains(pos)) {
					emptyPositions.add(pos);
				}
			}
		}

		return emptyPositions;
	}
	
	public String toColumnLabelingScheme(int ordinal) {
		return fromOrdinal(getColumnLabelingScheme(), ordinal);
	}
	
	public String toRowLabelingScheme(int ordinal) {
		return fromOrdinal(getRowLabelingScheme(), ordinal);
	}
	
	public boolean areValidPositions(String posOne, String posTwo) {
		if (isDimensionless()) {
			return true;
		}

		int posOneOrdinal = toOrdinal(getColumnLabelingScheme(), posOne);
		int posTwoOrdinal = toOrdinal(getRowLabelingScheme(), posTwo);
		return getPositionAssigner().isValidPosition(this, posTwoOrdinal, posOneOrdinal);
	}
	
	public boolean areValidPositions(int posOne, int posTwo) {
		if (isDimensionless()) {
			return true;
		}

		return getPositionAssigner().isValidPosition(this, posTwo, posOne);
	}
	
	public StorageContainerPosition createPosition(String posOne, String posTwo) {
		if (isDimensionless()) {
			return createPosition(null, null, null, null);
		}

		Integer posOneOrdinal = toOrdinal(getColumnLabelingScheme(), posOne);
		Integer posTwoOrdinal = toOrdinal(getRowLabelingScheme(), posTwo);
		return createPosition(posOneOrdinal, posOne, posTwoOrdinal, posTwo);
	}
	
	public void removePosition(StorageContainerPosition position) {
		if (position == null || position.getId() == null) {
			return;
		}

		if (isDimensionless()) {
			getDaoFactory().getStorageContainerPositionDao().delete(position);
		} else {
			Iterator<StorageContainerPosition> iter = getOccupiedPositions().iterator();
			while (iter.hasNext()) {
				if (position.getId().equals(iter.next().getId())) {
					iter.remove();
					break;
				}
			}
		}
	}
	
	public void addPosition(StorageContainerPosition position) {
		position.setContainer(this);
		if (isDimensionless()) {
			//
			// For dimensionless containers we directly update DB as the container might
			// contain large no. of specimens. Further occupiedPositions is not used
			// for allocating next available position in case of dimensionless container
			//
			getDaoFactory().getStorageContainerPositionDao().saveOrUpdate(position);
		} else {
			//
			// Update in-memory set as it is used for assigning next available position
			//
			getOccupiedPositions().add(position);
		}
	}

	public StorageContainerPosition nextAvailablePosition() {
		return nextAvailablePosition(null, null);
	}

	public StorageContainerPosition nextAvailablePosition(boolean fromLastAssignedPos) {
		String row = null, col = null;
		StorageContainerPosition lastAssignedPos = lastAssignedPositions.get().get(getId());
		if (!isDimensionless() && fromLastAssignedPos && lastAssignedPos != null) {
			Pair<Integer, Integer> startPos = getPositionAssigner().nextPosition(this, lastAssignedPos.getPosTwoOrdinal(), lastAssignedPos.getPosOneOrdinal());
			row = fromOrdinal(getRowLabelingScheme(), startPos.first());
			col = fromOrdinal(getColumnLabelingScheme(), startPos.second());
		}

		return nextAvailablePosition(row, col);
	}

	public StorageContainerPosition nextAvailablePosition(int position) {
		String row = null, column = null;
		if (!isDimensionless() && position > 0) {
			Pair<Integer, Integer> coord = getPositionAssigner().fromPosition(this, position);
			row    = fromOrdinal(getRowLabelingScheme(),    coord.first());
			column = fromOrdinal(getColumnLabelingScheme(), coord.second());
		}

		return nextAvailablePosition(row, column);
	}

	public StorageContainerPosition nextAvailablePosition(String row, String col) {
		if (isDimensionless()) {
			return createPosition(null, null, null, null);
		}

		int startRow = 1, startCol = 1;
		boolean startPosSpecified = StringUtils.isNotBlank(row) && StringUtils.isNotBlank(col);
		if (startPosSpecified) {
			startRow = toOrdinal(getRowLabelingScheme(), row);
			startCol = toOrdinal(getColumnLabelingScheme(), col);
		}

		Pair<Integer, Integer> nextPos = getPositionAssigner().nextAvailablePosition(this, startRow, startCol);
		if (nextPos != null) {
			String posOne = fromOrdinal(getColumnLabelingScheme(), nextPos.second());
			String posTwo = fromOrdinal(getRowLabelingScheme(), nextPos.first());
			StorageContainerPosition position = createPosition(nextPos.second(), posOne, nextPos.first(), posTwo);
			lastAssignedPositions.get().put(getId(), position);
			return position;
		}

		if (startPosSpecified) {
			return nextAvailablePosition(null, null);
		}

		return null;
	}

	public boolean isPositionOccupied(String posOne, String posTwo) {
		if (isDimensionless()) {
			return false;
		}

		int posOneOrdinal = toOrdinal(getColumnLabelingScheme(), posOne);
		int posTwoOrdinal = toOrdinal(getRowLabelingScheme(), posTwo);
		return getOccupiedPosition(posOneOrdinal, posTwoOrdinal) != null;
	}

	public boolean isPositionOccupied(int posOneOrdinal, int posTwoOrdinal) {
		if (isDimensionless()) {
			return false;
		}

		return getOccupiedPosition(posOneOrdinal, posTwoOrdinal) != null;
	}
	
	public boolean canSpecimenOccupyPosition(Long specimenId, String posOne, String posTwo) {
		return canOccupyPosition(true, specimenId, posOne, posTwo, false);
	}

	public boolean canSpecimenOccupyPosition(Long specimenId, String posOne, String posTwo, boolean vacateOccupant) {
		return canOccupyPosition(true, specimenId, posOne, posTwo, vacateOccupant);
	}

	public boolean canContainerOccupyPosition(Long containerId, String posOne, String posTwo) {
		return canOccupyPosition(false, containerId, posOne, posTwo, false);
	}
	
	public boolean canContain(Specimen specimen) {
		if (isDistributionContainer()) {
			return canContainSpecimen(specimen.getDp());
		} else if (specimen.getSpecimenClass() != null && specimen.getSpecimenType() != null) {
			return canContainSpecimen(specimen.getCollectionProtocol(), specimen.getSpecimenClass().getValue(), specimen.getSpecimenType().getValue());
		}

		return false;
	}
	
	public boolean canContain(StorageContainer container) {
		if (isDistributionContainer()) {
			return getCompAllowedDps().isEmpty() || getCompAllowedDps().containsAll(container.getCompAllowedDps());
		}

		Set<PermissibleValue> allowedClasses = getCompAllowedSpecimenClasses();
		if (!allowedClasses.containsAll(container.getCompAllowedSpecimenClasses())) {
			return false;
		}
		
		Set<PermissibleValue> allowedTypes = getCompAllowedSpecimenTypes();
		if (!allowedTypes.containsAll(container.getCompAllowedSpecimenTypes())) {
			allowedTypes = computeAllAllowedSpecimenTypes();
			if (!allowedTypes.containsAll(container.getCompAllowedSpecimenTypes())) { 
				return false;
			}			
		}
				
		if (!getCompAllowedCps().isEmpty()) {
			return getCompAllowedCps().containsAll(container.getCompAllowedCps());
		} else if (!container.getCompAllowedCps().isEmpty()) {
			for (CollectionProtocol cp : container.getCompAllowedCps()) {
				if (!cp.getRepositories().contains(getSite())) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public boolean canContainSpecimen(CollectionProtocol cp, String specimenClass, String specimenType) {
		if (!isStoreSpecimenEnabled()) {
			return false;
		}

		if (!contains(getCompAllowedSpecimenClasses(), specimenClass) &&
				!contains(getCompAllowedSpecimenTypes(), specimenType)) {
			return false;
		}
		
		if (!getCompAllowedCps().isEmpty()) {
			return getCompAllowedCps().contains(cp);
		} else {
			return cp.getRepositories().contains(getSite());
		}
	}

	public boolean canContainSpecimen(DistributionProtocol dp) {
		return isStoreSpecimenEnabled() && isDistributionContainer() &&
			(getCompAllowedDps().isEmpty() || getCompAllowedDps().contains(dp));
	}

	public StorageContainerPosition getReservedPosition(String row, String column, String reservationId) {
		StorageContainerPosition reservedPos = getOccupiedPositions().stream()
			.filter(pos -> pos.equals(row, column, reservationId))
			.findFirst().orElse(null);
		if (reservedPos == null) {
			return null;
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -5);
		if (reservedPos.getReservationTime().before(cal.getTime())) {
			getOccupiedPositions().remove(reservedPos);
			return null;
		}

		return reservedPos;
	}

	public void validateRestrictions() {
		StorageContainer parent = getParentContainer();
		if (parent != null && !parent.canContain(this)) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.CANNOT_HOLD_CONTAINER, parent.getName(), getName());
		}

		ContainerRestrictionsCriteria crit = new ContainerRestrictionsCriteria()
			.containerId(getId())
			.specimenClasses(getCompAllowedSpecimenClasses())
			.specimenTypes(getCompAllowedSpecimenTypes())
			.collectionProtocols(getCompAllowedCps())
			.distributionProtocols(getCompAllowedDps())
			.site(getSite());

		StorageContainerDao containerDao = getDaoFactory().getStorageContainerDao();
		List<String> nonCompliantContainers = containerDao.getNonCompliantContainers(crit);
		if (CollectionUtils.isNotEmpty(nonCompliantContainers)) {
			// Show first non compliant container in error message
			throw OpenSpecimenException.userError(
				StorageContainerErrorCode.CANNOT_HOLD_CONTAINER,
				getName(),
				nonCompliantContainers.get(0));
		}

		List<String> nonCompliantSpecimens = null;
		if (isDistributionContainer()) {
			nonCompliantSpecimens = containerDao.getNonCompliantDistributedSpecimens(crit);
		} else {
			nonCompliantSpecimens = containerDao.getNonCompliantSpecimens(crit);
		}

		if (CollectionUtils.isNotEmpty(nonCompliantSpecimens)) {
			// Show first non compliant specimen in error message
			throw OpenSpecimenException.userError(
				StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN,
				getName(),
				nonCompliantSpecimens.get(0));
		}
	}
	
	public Set<CollectionProtocol> computeAllowedCps() {
		if (CollectionUtils.isNotEmpty(getAllowedCps())) {
			return new HashSet<>(getAllowedCps());
		} else if (getParentContainer() == null) {
			return new HashSet<>();
		} else {
			return getParentContainer().computeAllowedCps();
		}
	}
	
	public Set<PermissibleValue> computeAllowedSpecimenClasses() {
		if (CollectionUtils.isNotEmpty(getAllowedSpecimenTypes()) || 
				CollectionUtils.isNotEmpty(getAllowedSpecimenClasses())) {
			return new HashSet<>(getAllowedSpecimenClasses());
		}
		
		if (getParentContainer() != null) {
			return getParentContainer().computeAllowedSpecimenClasses();
		}
				
		return new HashSet<>(getDaoFactory().getPermissibleValueDao().getSpecimenClasses());
	}
	
	public Set<PermissibleValue> computeAllowedSpecimenTypes() {
		Set<PermissibleValue> types = new HashSet<>();
		if (CollectionUtils.isNotEmpty(getAllowedSpecimenTypes())) {
			types.addAll(getAllowedSpecimenTypes());
		} else if (CollectionUtils.isEmpty(getAllowedSpecimenClasses()) && getParentContainer() != null) {
			types.addAll(getParentContainer().computeAllowedSpecimenTypes());			
		}
		
		return types;
	}

	public Set<DistributionProtocol> computeAllowedDps() {
		if (CollectionUtils.isNotEmpty(getAllowedDps())) {
			return new HashSet<>(getAllowedDps());
		} else if (getParentContainer() == null) {
			return new HashSet<>();
		} else {
			return getParentContainer().computeAllowedDps();
		}
	}

	public List<DependentEntityDetail> getDependentEntities() {
		return DependentEntityDetail.singletonList(
				Specimen.getEntityName(), getSpecimensCount());
	}
	
	public void delete(boolean checkSpecimens) {
		if (checkSpecimens) {
			int specimensCnt = getSpecimensCount();
			if (specimensCnt > 0) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.REF_ENTITY_FOUND, getName());
			}
		}

		deleteWithoutCheck();
	}

	public String getStringifiedAncestors() {
		StringBuilder names = new StringBuilder();
		getStringifiedAncestors(names);
		names.delete(names.length() - 2, names.length());
		return names.toString();
	}

	//
	// Assign unoccupied positions in container
	//
	public void assignPositions(Collection<StorageContainerPosition> positions) {
		assignPositions(positions, false);
	}

	//
	// Two cases:
	// case #1: vacateOccupant: true - Assign unoccupied positions
	// case #2: Otherwise - Vacate occupant before assigning position to new occupant
	//
	public void assignPositions(Collection<StorageContainerPosition> positions, boolean vacateOccupant) {
		vacateOccupant = !isDimensionless() && vacateOccupant;

		Set<Long> specimenIds = Collections.emptySet();
		if (vacateOccupant) {
			specimenIds = new HashSet<>();
			for (StorageContainerPosition position : positions) {
				if (position.getOccupyingSpecimen() != null) {
					specimenIds.add(position.getOccupyingSpecimen().getId());
				}
			}
		}

		for (StorageContainerPosition position : positions) {
			StorageContainerPosition existing = null;
			if (!isDimensionless() && position.isSpecified()) {
				existing = getOccupiedPosition(position.getPosOneOrdinal(), position.getPosTwoOrdinal());
			}

			if (existing != null && !vacateOccupant) {
				continue; 
			}
						
			if (position.getOccupyingSpecimen() != null) {
				if (existing != null && !specimenIds.contains(existing.getOccupyingSpecimen().getId())) {
					//
					// The occupant that is being vacated is not assigned any new position
					// in this transaction. Therefore virtualise it.
					//
					AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(existing.getOccupyingSpecimen(), false);
					existing.getOccupyingSpecimen().updatePosition(null);
				}

				position.getOccupyingSpecimen().updatePosition(position);
			} else {
				StorageContainer childContainer = position.getOccupyingContainer();
				boolean hasParentChanged = !this.equals(childContainer.getParentContainer());
				childContainer.updateContainerLocation(getSite(), this, position);
				
				if (hasParentChanged) {
					childContainer.updateComputedClassAndTypes();
					childContainer.updateComputedCps();
				}
			}
		}
	}

	public List<StorageContainerPosition> reservePositions(int numPositions) {
		return reservePositions(getReservationId(), Calendar.getInstance().getTime(), numPositions);
	}

	public List<StorageContainerPosition> reservePositions(String reservationId, Date reservationTime, int numPositions) {
		List<StorageContainerPosition> reservedPositions = new ArrayList<>();

		while (numPositions != 0) {
			StorageContainerPosition pos = nextAvailablePosition(true);
			if (pos == null) {
				break;
			}

			pos.setReservationId(reservationId);
			pos.setReservationTime(reservationTime);
			reservedPositions.add(pos);

			--numPositions;
			if (!isDimensionless()) {
				addPosition(pos);
			}
		}

		return reservedPositions;
	}

	public void blockPositions(Collection<StorageContainerPosition> positions) {
		if (isDimensionless()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.DL_POS_BLK_NP, getName());
		}

		Date reservationTime = Calendar.getInstance().getTime();
		String reservationId = getReservationId();
		for (StorageContainerPosition position : positions) {
			if (!position.isSpecified() || !areValidPositions(position.getPosOneOrdinal(), position.getPosTwoOrdinal())) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.INV_POS, getName(), position.getPosOne(), position.getPosTwo());
			}

			if (isPositionOccupied(position.getPosOneOrdinal(), position.getPosTwoOrdinal())) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.POS_OCCUPIED, getName(), position.getPosOne(), position.getPosTwo());
			}

			position.setBlocked(true);
			position.setReservationTime(reservationTime);
			position.setReservationId(reservationId);
			addPosition(position);
		}
	}

	public void blockAllPositions() {
		if (isDimensionless()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.DL_POS_BLK_NP, getName());
		}

		StorageContainerPosition position = null;
		Date reservationTime = Calendar.getInstance().getTime();
		String reservationId = getReservationId();

		while ((position = nextAvailablePosition(true)) != null) {
			position.setBlocked(true);
			position.setReservationTime(reservationTime);
			position.setReservationId(reservationId);
			addPosition(position);
		}
	}

	public void unblockPositions(Collection<StorageContainerPosition> positions) {
		if (isDimensionless()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.DL_POS_BLK_NP, getName());
		}

		for (StorageContainerPosition position : positions) {
			StorageContainerPosition occupied = getOccupiedPosition(position.getPosOneOrdinal(), position.getPosTwoOrdinal());
			if (occupied != null && occupied.isBlocked()) {
				occupied.vacate();
			}
		}
	}

	public void unblockAllPositions() {
		if (isDimensionless()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.DL_POS_BLK_NP, getName());
		}

		List<StorageContainerPosition> blockedPositions = getOccupiedPositions().stream()
			.filter(StorageContainerPosition::isBlocked)
			.collect(Collectors.toList());

		//
		// Note this loop cannot be streamed concurrently in the above pipeline
		//
		blockedPositions.forEach(StorageContainerPosition::vacate);
	}
	
	public StorageContainer copy() {
		StorageContainer copy = new StorageContainer();
		copy.setType(getType());
		copy.setUsedFor(getUsedFor());
		copy.setSite(getSite());
		copy.setParentContainer(getParentContainer());
		copy.setNoOfColumns(getNoOfColumns());
		copy.setNoOfRows(getNoOfRows());
		copy.setPositionLabelingMode(getPositionLabelingMode());
		copy.setColumnLabelingScheme(getColumnLabelingScheme());
		copy.setRowLabelingScheme(getRowLabelingScheme());
		copy.setPositionAssignment(getPositionAssignment());
		copy.setTemperature(getTemperature());
		copy.setStoreSpecimenEnabled(isStoreSpecimenEnabled());
		copy.setCellDisplayProp(getCellDisplayProp());
		copy.setComments(getComments());
		copy.setCreatedBy(getCreatedBy());
		copy.setAllowedSpecimenClasses(new HashSet<>(getAllowedSpecimenClasses()));
		copy.setAllowedSpecimenTypes(new HashSet<>(getAllowedSpecimenTypes()));
		copy.setAllowedCps(new HashSet<>(getAllowedCps()));
		copy.setAllowedDps(new HashSet<>(getAllowedDps()));
		copy.setCompAllowedSpecimenClasses(computeAllowedSpecimenClasses());
		copy.setCompAllowedSpecimenTypes(computeAllowedSpecimenTypes());
		copy.setCompAllowedCps(computeAllowedCps());
		copy.setCompAllowedDps(computeAllowedDps());
		copyExtensionTo(copy);
		return copy;
	}
	
	public void removeCpRestriction(CollectionProtocol cp) {
		getAllowedCps().remove(cp);
		updateComputedCps();
	}

	public void removeDpRestriction(DistributionProtocol dp) {
		getAllowedDps().remove(dp);
		updateComputedDps();
	}

	public void setFreezerCapacity() {
		List<StorageContainer> containers = new ArrayList<>();
		StorageContainer freezer = this;
		while (freezer.getParentContainer() != null) {
			containers.add(freezer);
			freezer = freezer.getParentContainer();
		}
		containers.add(freezer);

		if (freezer.getCapacity() != null && freezer.getCapacity() > 0) {
			return;
		}

		if (containers.stream().anyMatch(StorageContainer::isDimensionless)) {
			return;
		}

		Integer capacity = 1;
		for (StorageContainer container : containers) {
			capacity *= container.getNoOfRows() * container.getNoOfColumns();
		}

		freezer.setCapacity(capacity);
	}

	public void retrieveSpecimen(Specimen specimen) {
		AutomatedContainerContext.getInstance().retrieveSpecimen(this, specimen);
	}

	public void storeSpecimen(Specimen specimen) {
		AutomatedContainerContext.getInstance().storeSpecimen(this, specimen);
	}

	public ContainerStoreList.Status processList(ContainerStoreList list) {
		return getAutoFreezerProvider().getInstance().processList(list);
	}

	public boolean isSiteContainer(Site site) {
		return this.equals(site.getContainer());
	}

	public boolean isSiteContainer() {
		return isSiteContainer(getSite());
	}

	public static String getDefaultSiteContainerName(Site site) {
		return MessageUtil.getInstance().getMessage(DEF_SITE_CONT_NAME, new Object[] { site.getName() });
	}

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	public static boolean isValidScheme(String scheme) {
		if (StringUtils.isBlank(scheme)) {
			return false;
		}

		return scheme.equals(NUMBER_LABELING_SCHEME) ||
				scheme.equals(UPPER_CASE_ALPHA_LABELING_SCHEME) ||
				scheme.equals(LOWER_CASE_ALPHA_LABELING_SCHEME) ||
				scheme.equals(UPPER_CASE_ROMAN_LABELING_SCHEME) ||
				scheme.equals(LOWER_CASE_ROMAN_LABELING_SCHEME);
	}

	public static String getReservationId() {
		return UUID.randomUUID().toString();
	}

	public static List<StorageContainer> sort(StorageContainer parent, Collection<StorageContainer> containers) {
		return containers.stream()
			.sorted((c1, c2) -> {
				if (parent.isDimensionless()) {
					return c1.getId().compareTo(c2.getId());
				} else if (c1.getPosition() != null && c2.getPosition() != null) {
					return c1.getPosition().getPosition().compareTo(c2.getPosition().getPosition());
				} else if (c1.getPosition() != null) {
					return -1;
				} else if (c2.getPosition() != null) {
					return 1;
				} else {
					// archived or checked out boxes
					return c1.getId().compareTo(c2.getId());
				}
			})
			.collect(Collectors.toList());
	}

	private void deleteWithoutCheck() {
		getChildContainers().forEach(StorageContainer::deleteWithoutCheck);

		if (isSiteContainer()) {
			getSite().setContainer(null);
		}

		if (getParentContainer() != null) {
			getParentContainer().removePosition(getPosition());
			setPosition(null);
		}

		if (getBlockedPosition() != null) {
			getBlockedPosition().getContainer().removePosition(getBlockedPosition());
			setBlockedPosition(null);
		}

		DeObject extension = getExtension();
		if (extension != null) {
			extension.delete();
		}

		setName(Utility.getDisabledValue(getName(), 64));
		setBarcode(Utility.getDisabledValue(getBarcode(), 64));
		setActivityStatus(com.krishagni.catissueplus.core.common.util.Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	private void archive(String reason) {
		if (isArchived() || isDeleted()) {
			return;
		}

		int specimensCnt = getSpecimensCount();
		if (specimensCnt > 0) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.CANNOT_ARCHIVE, getName(), specimensCnt);
		}

		archive0(true, reason);
	}

	private void archive0(boolean vacatePosition, String reason) {
		getChildContainers().forEach(c -> c.archive0(false, null));
		if (isSiteContainer()) {
			getSite().setContainer(null);
		}

		if (vacatePosition && getParentContainer() != null) {
			StorageContainer parent = getParentContainer();
			updateContainerLocation(
				getParentContainer().getSite(),
				null,
				null,
				null,
				null,
				"ARCHIVED: " + (reason != null ? reason : ""),
				false);
			setParentContainer(parent);
		}

		setActivityStatus(com.krishagni.catissueplus.core.common.util.Status.ACTIVITY_STATUS_CLOSED.getStatus());
	}

	private int getSpecimensCount() {
		return getDaoFactory().getStorageContainerDao().getSpecimensCount(getId());
	}
	
	private Set<PermissibleValue> computeAllAllowedSpecimenTypes() {
		Set<PermissibleValue> types = new HashSet<>();
		
		if (CollectionUtils.isNotEmpty(getAllowedSpecimenTypes())) {
			types.addAll(getAllowedSpecimenTypes());
		} else if (CollectionUtils.isEmpty(getAllowedSpecimenClasses())) {
			if (getParentContainer() != null) {
				return getParentContainer().computeAllAllowedSpecimenTypes();
			}
		}

		Set<PermissibleValue> classes = getCompAllowedSpecimenClasses();
		if (CollectionUtils.isNotEmpty(classes)) {
			for (PermissibleValue classPv : classes) {
				types.addAll(classPv.getChildren());
			}
		}
				
		return types;
	}
	
	private StorageContainerPosition createPosition(Integer posOneOrdinal, String posOne, Integer posTwoOrdinal, String posTwo) {
		StorageContainerPosition position = new StorageContainerPosition();
		position.setPosOneOrdinal(posOneOrdinal);
		position.setPosOne(posOne);
		position.setPosTwoOrdinal(posTwoOrdinal);
		position.setPosTwo(posTwo);
		position.setContainer(this);

		return position;
	}
	
	private StorageContainerPosition getOccupiedPosition(int posOne, int posTwo) {
		StorageContainerPosition result = null;
		
		for (StorageContainerPosition pos : getOccupiedPositions()) {
			if (pos.getPosOneOrdinal() == posOne && pos.getPosTwoOrdinal() == posTwo) {
				result = pos;
				break;
			}
		}
		
		return result;
	}
	
	private boolean canOccupyPosition(
			boolean isSpecimenEntity,
			Long entityId, 
			String posOne, 
			String posTwo, 
			boolean vacateOccupant) {

		if (isDimensionless()) {
			return isSpecimenEntity;
		}

		int posOneOrdinal = toOrdinal(getColumnLabelingScheme(), posOne);
		int posTwoOrdinal = toOrdinal(getRowLabelingScheme(), posTwo);
		
		if (!areValidPositions(posOneOrdinal, posTwoOrdinal)) {
			return false;
		}

		StorageContainerPosition pos = getOccupiedPosition(posOneOrdinal, posTwoOrdinal);
		if (pos == null) {
			return true; // vacant position
		} else if (entityId == null) { 
			return false; // position is not vacant and entity is new
		} else if (isSpecimenEntity) {
			return (vacateOccupant && pos.getOccupyingContainer() == null) ||
				(pos.getOccupyingSpecimen() != null && pos.getOccupyingSpecimen().getId().equals(entityId)) ||
				(pos.getCheckoutSpecimen() != null && pos.getCheckoutSpecimen().getId().equals(entityId));
		} else {
			return (pos.getOccupyingContainer() != null && pos.getOccupyingContainer().getId().equals(entityId)) ||
				(pos.getBlockedForContainer() != null && pos.getBlockedForContainer().getId().equals(entityId));
		}
	}
	
	private void updateCapacity(StorageContainer other) {
		if (isDimensionless() && !other.isDimensionless()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.DL_TO_REG_NA);
		} else if (!isDimensionless() && other.isDimensionless()) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.REG_TO_DL_NA);
		} else if (!isDimensionless()) {
			if (other.getNoOfColumns() < getNoOfColumns() || other.getNoOfRows() < getNoOfRows()) {
				if (arePositionsOccupiedBeyondCapacity(other.getNoOfColumns(), other.getNoOfRows())) {
					throw OpenSpecimenException.userError(StorageContainerErrorCode.CANNOT_SHRINK_CONTAINER);
				}
			}

			setNoOfColumns(other.getNoOfColumns());
			setNoOfRows(other.getNoOfRows());
		}

		if (other.getCapacity() != null && other.getCapacity() > 0) {
			setCapacity(other.getCapacity());
		}
	}

	private void updateLabelingScheme(StorageContainer other) {
		if (isDimensionless()) {
			return;
		}

		boolean colSchemeChanged = !getColumnLabelingScheme().equals(other.getColumnLabelingScheme());
		boolean rowSchemeChanged = !getRowLabelingScheme().equals(other.getRowLabelingScheme());
		if (!colSchemeChanged && !rowSchemeChanged) {
			return;
		}
		
		for (StorageContainerPosition pos : getOccupiedPositions()) {
			if (colSchemeChanged) {
				pos.setPosOne(fromOrdinal(other.getColumnLabelingScheme(), pos.getPosOneOrdinal()));
			}
			
			if (rowSchemeChanged) {
				pos.setPosTwo(fromOrdinal(other.getRowLabelingScheme(), pos.getPosTwoOrdinal()));
			}
		}
		
		setColumnLabelingScheme(other.getColumnLabelingScheme());
		setRowLabelingScheme(other.getRowLabelingScheme());
	}

	private void updatePositionAssignment(StorageContainer other) {
		if (isDimensionless()) {
			return;
		}

		if (getPositionAssignment() == other.getPositionAssignment()) {
			return;
		}

		newPositions = new HashMap<>();
		for (StorageContainerPosition pos : getOccupiedPositions()) {
			Pair<Integer, Integer> mapIdx = getPositionAssigner().getMapIdx(this, pos.getPosTwoOrdinal(), pos.getPosOneOrdinal());
			Pair<Integer, Integer> rowCol = other.getPositionAssigner().fromMapIdx(other, mapIdx.first(), mapIdx.second());
			newPositions.put(pos, rowCol);

			pos.setPosTwoOrdinal(null);
			pos.setPosOneOrdinal(null);
			pos.setPosTwo(null);
			pos.setPosOne(null);
		}

		setPositionAssignment(other.getPositionAssignment());
	}
	
	private void updateContainerLocation(StorageContainer other) {
		if (!isActive()) {
			return;
		}

		updateContainerLocation(
			other.getSite(), other.getParentContainer(), other.getPosition(),
			other.getTransferredBy(), other.getTransferDate(), other.getOpComments(),
			Boolean.TRUE.equals(other.getCheckOut())
		);
	}

	private void updateContainerLocation(Site otherSite, StorageContainer otherParentContainer, StorageContainerPosition otherPos) {
		updateContainerLocation(otherSite, otherParentContainer, otherPos, null, null, null, false);
	}

	private void updateContainerLocation(
		Site otherSite, StorageContainer otherParentContainer, StorageContainerPosition otherPos,
		User transferredBy, Date transferDate, String transferReasons, boolean checkOut) {
		Site existing = site;

		if (Objects.equals(site, otherSite) &&
			Objects.equals(parentContainer, otherParentContainer) &&
			StorageContainerPosition.areSame(position, otherPos)) {
			return;
		}

		ContainerTransferEvent transferEvent = new ContainerTransferEvent().fromLocation(site, position != null ? position.getContainer() : null, position);
		transferEvent.setContainer(this);
		transferEvent.setUser(transferredBy != null ? transferredBy : AuthUtil.getCurrentUser());
		transferEvent.setTime(transferDate != null ? transferDate : Calendar.getInstance().getTime());
		transferEvent.setReason(transferReasons);

		StorageContainerPosition oldPosition = null;
		if (otherParentContainer == null) {
			if (getParentContainer() != null) {
				oldPosition = position;
				getParentContainer().removePosition(position);
			} else if (checkOut) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.CHECKED_OUT_ALREADY, getName());
			}

			if (!checkOut) {
				setParentContainer(null);
			}

			setPosition(null);
			setSite(otherSite);
		} else {
			setParentContainer(otherParentContainer);
			setSite(otherParentContainer.getSite());
			if (cycleExistsInHierarchy(otherParentContainer)) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.HIERARCHY_CONTAINS_CYCLE);
			}

			if (position != null) {
				position.update(otherPos);
				position.setBlockedForContainer(null);
				position.setBlocked(false);
			} else {
				setPosition(otherPos);
			}
		}

		//
		// has site changed?
		//
		if (!site.equals(existing)) {
			//
			// if yes, ensure all the child containers beneath it are updated
			//
			updateSite(site);
		}

		transferEvent.toLocation(site, !checkOut ? parentContainer : null, position);
		getTransferEvents().add(transferEvent);

		if (checkOut && oldPosition != null) {
			StorageContainerPosition blockedPosition = oldPosition.getContainer().createPosition(
				oldPosition.getPosOneOrdinal(), oldPosition.getPosOne(),
				oldPosition.getPosTwoOrdinal(), oldPosition.getPosTwo()
			);
			blockedPosition.setBlocked(true);
			blockedPosition.setBlockedForContainer(this);
			setBlockedPosition(blockedPosition);
			setStatus(Status.CHECKED_OUT);
			transferEvent.setReason("CHECK-OUT: " + (transferReasons != null ? transferReasons : ""));
		} else if (getBlockedPosition() != null) {
			String prefix = "";
			if (!"ARCHIVED".equals(transferReasons)) {
				prefix = "CHECK-IN: ";
			}

			getBlockedPosition().getContainer().removePosition(getBlockedPosition());
			setBlockedPosition(null);
			setStatus(null);
			transferEvent.setReason(prefix + (transferReasons != null ? transferReasons : ""));
		}

		oldPosition = null;
	}
	
	private void updateSite(Site site) {
		setSite(site);
		for (StorageContainer container : getChildContainers()) {
			container.updateSite(site);
		}
	}
	
	private void updateActivityStatus(StorageContainer other) {
		if (getActivityStatus().equals(other.getActivityStatus())) {
			// activity status has not changed
			return;
		}

		if (other.isDeleted()) {
			delete(true);
		} else if (other.isArchived()) {
			archive(other.getOpComments());
		} else if (isArchived() && other.isActive()) {
			if (other.getParentContainer() != null && !other.getParentContainer().isActive()) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.PARENT_ARCHIVED, other.getParentContainer().getName());
			}

			List<StorageContainer> containers = new ArrayList<>();
			containers.add(this);
			while (!containers.isEmpty()) {
				StorageContainer container = containers.remove(0);
				if (!container.isArchived()) {
					// either active or deleted
					continue;
				}

				container.setActivityStatus(other.getActivityStatus()); // active
				containers.addAll(container.getChildContainers());
			}

			other.setOpComments("UNARCHIVED: " + (other.getOpComments() != null ? other.getOpComments() : ""));
		}
	}

	private void updateAllowedSpecimenClassAndTypes(StorageContainer other, boolean updateComputedTypes) {
		boolean computeTypes = updateComputedTypes;
		
		if (!CollectionUtils.isEqualCollection(getAllowedSpecimenClasses(), other.getAllowedSpecimenClasses())) {
			getAllowedSpecimenClasses().clear();
			getAllowedSpecimenClasses().addAll(other.getAllowedSpecimenClasses());
			computeTypes = true;			
		}
		
		if (!CollectionUtils.isEqualCollection(getAllowedSpecimenTypes(), other.getAllowedSpecimenTypes())) {
			getAllowedSpecimenTypes().clear();
			getAllowedSpecimenTypes().addAll(other.getAllowedSpecimenTypes());
			computeTypes = true;			
		}
		
		if (computeTypes) {
			updateComputedClassAndTypes();
		}
				
	}
	
	private void updateComputedClassAndTypes() {
		getCompAllowedSpecimenClasses().clear();
		getCompAllowedSpecimenClasses().addAll(computeAllowedSpecimenClasses());

		getCompAllowedSpecimenTypes().clear();
		getCompAllowedSpecimenTypes().addAll(computeAllowedSpecimenTypes());				
		
		for (StorageContainer childContainer : getChildContainers()) {
			childContainer.updateComputedClassAndTypes();
		}		
	}
	
	private void updateAllowedCps(StorageContainer other, boolean updateComputedCps) {
		boolean computeCps = updateComputedCps;		
		if (!CollectionUtils.isEqualCollection(getAllowedCps(), other.getAllowedCps())) {
			getAllowedCps().clear();
			getAllowedCps().addAll(other.getAllowedCps());
			computeCps = true;
		}
		
		if (computeCps) {
			updateComputedCps();			
		}
	}
	
	private void updateComputedCps() {
		getCompAllowedCps().clear();
		getCompAllowedCps().addAll(computeAllowedCps());
		getChildContainers().forEach(StorageContainer::updateComputedCps);
	}

	private void updateAllowedDps(StorageContainer other, boolean updateComputedDps) {
		boolean computeDps = updateComputedDps;
		if (!CollectionUtils.isEqualCollection(getAllowedDps(), other.getAllowedDps())) {
			getAllowedDps().clear();
			getAllowedDps().addAll(other.getAllowedDps());
			computeDps = true;
		}

		if (computeDps) {
			updateComputedDps();
		}
	}

	private void updateComputedDps() {
		getCompAllowedDps().clear();
		getCompAllowedDps().addAll(computeAllowedDps());
		getChildContainers().forEach(StorageContainer::updateComputedDps);
	}
	
	private void updateStoreSpecimenEnabled(StorageContainer other) {
		if (isStoreSpecimenEnabled() != other.isStoreSpecimenEnabled() && other.isStoreSpecimenEnabled()) {
			setFreezerCapacity();
		}

		setStoreSpecimenEnabled(other.isStoreSpecimenEnabled());
	}

	private void updateCellDisplayProp(StorageContainer other) {
		if (getCellDisplayProp() == other.getCellDisplayProp() || getParentContainer() != null) {
			return;
		}

		updateCellDisplayProp(this, other.getCellDisplayProp());
	}

	private void updateCellDisplayProp(StorageContainer container, CellDisplayProp cellDisplayProp) {
		container.setCellDisplayProp(cellDisplayProp);
		for (StorageContainer childContainer : container.getChildContainers()) {
			updateCellDisplayProp(childContainer, cellDisplayProp);
		}
	}
		
	private boolean arePositionsOccupiedBeyondCapacity(int noOfCols, int noOfRows) {
		boolean result = false;

		PositionAssigner assigner = getPositionAssigner();
		for (StorageContainerPosition pos : getOccupiedPositions()) {
			if (!assigner.isValidPosition(noOfRows, noOfCols, pos.getPosTwoOrdinal(), pos.getPosOneOrdinal())) {
				result = true;
				break;
			}
		}
		
		return result;
	}

	private boolean cycleExistsInHierarchy(StorageContainer parentContainer) {
		if (parentContainer == null) {
			return false;
		}

		if (getId().equals(parentContainer.getId())) {
			return true;
		}
		
		for (StorageContainer child : getChildContainers()) {
			if (parentContainer.isDescendantOf(child)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isDescendantOf(StorageContainer other) {
		if (getId() == null || other == null || other.getId() == null) {
			return false;
		}
		
		StorageContainer container = this;
		while (container != null) {
			if (other.getId().equals(container.getId())) {
				return true;
			}
			
			container = container.getParentContainer();
		}
		
		return false;
	}
	
	private void getStringifiedAncestors(StringBuilder names) {
		if (getParentContainer() != null) {
			getParentContainer().getStringifiedAncestors(names);
		}
		
		names.append(getName()).append(", ");
	}
	
	//
	// Unfortunately @Configurable is not working for objects created by 
	// hibernate and java assist
	//
	private DaoFactory getDaoFactory() {
		return (DaoFactory)OpenSpecimenAppCtxProvider.getAppCtx().getBean("biospecimenDaoFactory");
	}

	private String fromOrdinal(String scheme, Integer pos) {
		try {
			return SchemeOrdinalConverterUtil.fromOrdinal(scheme, pos);
		} catch (IllegalArgumentException iae) {
			throw OpenSpecimenException.userError(getSchemeOrdinalErrorCode(scheme));
		}
	}

	private Integer toOrdinal(String scheme, String pos) {
		try {
			return SchemeOrdinalConverterUtil.toOrdinal(scheme, pos);
		} catch (IllegalArgumentException iae) {
			throw OpenSpecimenException.userError(getSchemeOrdinalErrorCode(scheme));
		}
	}

	private StorageContainerErrorCode getSchemeOrdinalErrorCode(String scheme) {
		StorageContainerErrorCode code = null;
		if (scheme.equals(NUMBER_LABELING_SCHEME)) {
			code = StorageContainerErrorCode.INVALID_NUMBER_POSITION;
		} else if (scheme.equals(UPPER_CASE_ALPHA_LABELING_SCHEME) || scheme.equals(LOWER_CASE_ALPHA_LABELING_SCHEME)) {
			code = StorageContainerErrorCode.INVALID_ALPHA_POSITION;
		} else if (scheme.equals(UPPER_CASE_ROMAN_LABELING_SCHEME) || scheme.equals(LOWER_CASE_ROMAN_LABELING_SCHEME)) {
			code = StorageContainerErrorCode.INVALID_ROMAN_POSITION;
		}

		return code;
	}

	private boolean contains(Collection<PermissibleValue> pvs, String value) {
		return Utility.nullSafeStream(pvs).anyMatch(pv -> pv.getValue().equals(value));
	}
}
