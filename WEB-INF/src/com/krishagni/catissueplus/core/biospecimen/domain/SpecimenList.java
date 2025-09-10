package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
@Configurable
public class SpecimenList extends BaseEntity {
	private static final String ENTITY_NAME = "specimen_list";

	private String name;
	
	private User owner;

	private String description;

	private Date createdOn;

	private Date lastUpdatedOn;
	
	private Set<User> sharedWith = new HashSet<>();

	private Set<UserGroup> sharedWithGroups = new HashSet<>();
	
	private Set<SpecimenListItem> specimens = new HashSet<>();

	private Set<SpecimenListsFolder> folders = new HashSet<>();
	
	private Date deletedOn;

	private Boolean sendDigestNotifs;

	private String sourceEntityType;

	private Long sourceEntityId;

	@Autowired
	private DaoFactory daoFactory;

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public Date getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Date lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	public Set<User> getSharedWith() {
		return sharedWith;
	}

	public void setSharedWith(Set<User> sharedWith) {
		this.sharedWith = sharedWith;
	}

	@AuditJoinTable(name = "OS_SPMN_LIST_SHARED_GRPS_AUD")
	public Set<UserGroup> getSharedWithGroups() {
		return sharedWithGroups;
	}

	public void setSharedWithGroups(Set<UserGroup> sharedWithGroups) {
		this.sharedWithGroups = sharedWithGroups;
	}

	public Set<User> getAllSharedUsers() {
		Set<User> users = new HashSet<>(getSharedWith());
		getSharedWithGroups().forEach(g -> users.addAll(g.getUsers()));
		return users;
	}

	public Set<SpecimenListItem> getSpecimens() {
		return specimens;
	}

	public void setSpecimens(Set<SpecimenListItem> specimens) {
		this.specimens = specimens;
	}

	public Set<SpecimenListsFolder> getFolders() {
		return folders;
	}

	public void setFolders(Set<SpecimenListsFolder> folders) {
		this.folders = folders;
	}

	public Date getDeletedOn() {
		return deletedOn;
	}

	public void setDeletedOn(Date deletedOn) {
		this.deletedOn = deletedOn;
	}

	public Boolean getSendDigestNotifs() {
		return sendDigestNotifs;
	}

	public void setSendDigestNotifs(Boolean sendDigestNotifs) {
		this.sendDigestNotifs = sendDigestNotifs;
	}

	public String getSourceEntityType() {
		return sourceEntityType;
	}

	public void setSourceEntityType(String sourceEntityType) {
		this.sourceEntityType = sourceEntityType;
	}

	public Long getSourceEntityId() {
		return sourceEntityId;
	}

	public void setSourceEntityId(Long sourceEntityId) {
		this.sourceEntityId = sourceEntityId;
	}

	public void addSharedUsers(List<User> users) {
		sharedWith.addAll(users);
		setLastUpdatedOn(Calendar.getInstance().getTime());
	}
	
	public void removeSharedUsers(List<User> users) {
		sharedWith.removeAll(users);
		setLastUpdatedOn(Calendar.getInstance().getTime());
	}
	
	public void updateSharedUsers(Collection<User> users) {
		sharedWith.retainAll(users);
		sharedWith.addAll(users);
		setLastUpdatedOn(Calendar.getInstance().getTime());
	}

	public void updateSharedGroups(Collection<UserGroup> groups) {
		sharedWithGroups.retainAll(groups);
		sharedWithGroups.addAll(groups);
		setLastUpdatedOn(Calendar.getInstance().getTime());
	}

	public boolean canUserAccess(Long userId) {
		if (owner != null && userId.equals(owner.getId())) {
			return true;
		}

		return daoFactory.getSpecimenListDao().isListSharedWithUser(getId(), userId);
	}
	
	public void update(SpecimenList specimenList) {
		setName(specimenList.getName());
		setDescription(specimenList.getDescription());
		updateSharedUsers(specimenList.getSharedWith());
		updateSharedGroups(specimenList.getSharedWithGroups());
		setLastUpdatedOn(Calendar.getInstance().getTime());
		setSendDigestNotifs(specimenList.getSendDigestNotifs());
	}

	public void delete() {
		setName(Utility.getDisabledValue(getName(), 255));
		setLastUpdatedOn(Calendar.getInstance().getTime());
		setDeletedOn(Calendar.getInstance().getTime());
	}

	public int size() {
		return specimens.size();
	}

	public boolean isDefaultList(User user) {
		return getDefaultListName(user).equals(getName());
	}

	public boolean isDefaultList() {
		return isDefaultList(getOwner());
	}

	public String getDisplayName() {
		if (isDefaultList()) {
			return getOwner().formattedName() + " Default Cart";
		}

		return getName();
	}

	public static String getDefaultListName(User user) {
		return getDefaultListName(user.getId());
	}

	public static String getDefaultListName(Long userId) {
		return String.format("$$$$user_%d", userId);
	}

	public static List<Specimen> groupByAncestors(Collection<Specimen> spmns) {
		if (spmns == null) {
			return Collections.emptyList();
		}

		Map<String, Set<Specimen>> spmnsMap = new LinkedHashMap<>();
		for (Specimen spmn : spmns) {
			while (spmn != null) {
				String parentLabel = null;
				if (spmn.getParentSpecimen() != null) {
					parentLabel = spmn.getParentSpecimen().getLabel();
				}

				Set<Specimen> childSpmns = spmnsMap.get(parentLabel);
				if (childSpmns == null) {
					childSpmns = new LinkedHashSet<>();
					spmnsMap.put(parentLabel, childSpmns);
				}

				childSpmns.add(spmn);
				spmn = spmn.getParentSpecimen();
			}
		}

		List<Specimen> result = new ArrayList<>();
		addSpecimens(spmnsMap, null, new HashSet<>(spmns), result);
		return result;
	}

	private static void addSpecimens(Map<String, Set<Specimen>> spmnsMap, String parentLabel, Set<Specimen> listSpecimens, List<Specimen> result) {
		Set<Specimen> childSpmns = spmnsMap.get(parentLabel);
		if (childSpmns == null || listSpecimens.isEmpty()) {
			return;
		}

		for (Specimen childSpmn : childSpmns) {
			if (listSpecimens.remove(childSpmn)) {
				result.add(childSpmn);
			}

			if (listSpecimens.isEmpty()) {
				break;
			}

			addSpecimens(spmnsMap, childSpmn.getLabel(), listSpecimens, result);
		}
	}
}
