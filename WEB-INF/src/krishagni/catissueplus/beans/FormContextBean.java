package krishagni.catissueplus.beans;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.de.domain.Form;

@Audited
public class FormContextBean {
	private Long identifier;
	
	private Long containerId;
	
	private String entityType;
	
	private Long cpId;

	private Long entityId;
	
	private Integer sortOrder;
	
	private boolean multiRecord;

	private boolean notifEnabled;

	private boolean dataInNotif;

	private Set<UserGroup> notifUserGroups = new HashSet<>();
	
	private boolean sysForm;
	
	private Date deletedOn;

	private Form form;

	public Long getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Long identifier) {
		this.identifier = identifier;
	}

	public Long getId() {
		return identifier;
	}

	public Long getContainerId() {
		return containerId;
	}

	public void setContainerId(Long containerId) {
		this.containerId = containerId;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public boolean isMultiRecord() {
		return multiRecord;
	}

	public void setMultiRecord(boolean isMultiRecord) {
		this.multiRecord = isMultiRecord;
	}

	public boolean isNotifEnabled() {
		return notifEnabled;
	}

	public void setNotifEnabled(boolean notifEnabled) {
		this.notifEnabled = notifEnabled;
	}

	public boolean isDataInNotif() {
		return dataInNotif;
	}

	public void setDataInNotif(boolean dataInNotif) {
		this.dataInNotif = dataInNotif;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public Set<UserGroup> getNotifUserGroups() {
		return notifUserGroups;
	}

	public void setNotifUserGroups(Set<UserGroup> notifUserGroups) {
		this.notifUserGroups = notifUserGroups;
	}

	public boolean isSysForm() {
		return sysForm;
	}

	public void setSysForm(boolean sysForm) {
		this.sysForm = sysForm;
	}

	public Date getDeletedOn() {
		return deletedOn;
	}

	public void setDeletedOn(Date deletedOn) {
		this.deletedOn = deletedOn;
	}

	@NotAudited
	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}
}
