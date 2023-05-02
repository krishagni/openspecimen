package krishagni.catissueplus.beans;

import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.UserContext;
import edu.common.dynamicextensions.napi.FormAuditManager;
import edu.common.dynamicextensions.napi.impl.FormAuditManagerImpl;

public class FormRecordEntryBean {

	private Long identifier;
	
	private Long formCtxtId;
	
	private Long objectId;
	
	private Long recordId;
	
	private Long updatedBy;
	
	private Date updatedTime;
	
	private Status status;
	
	private String entityType;

	private BaseEntity.DataEntryStatus formStatus;

	private FormContextBean formCtxt;

	public enum Status {
		ACTIVE, CLOSED
	}

	public Long getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Long identifier) {
		this.identifier = identifier;
	}

	public Long getFormCtxtId() {
		return formCtxtId;
	}

	public void setFormCtxtId(Long formCtxtId) {
		this.formCtxtId = formCtxtId;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public Long getRecordId() {
		return recordId;
	}

	public void setRecordId(Long recordId) {
		this.recordId = recordId;
	}

	public Long getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(Long updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}

	public Status getActivityStatus() {
		return status;
	}

	public void setActivityStatus(Status status) {
	  this.status = status;
	}

	public String getActivityStatusStr() {
	  return status != null ? status.name() : null;
	}

	public void setActivityStatusStr(String status) {
	  this.status = status != null ? Status.valueOf(status) : Status.ACTIVE;
	}

	public BaseEntity.DataEntryStatus getFormStatus() {
		return formStatus;
	}

	public void setFormStatus(BaseEntity.DataEntryStatus formStatus) {
		this.formStatus = formStatus;
	}

	public String getEntityType() {
		return formCtxt != null ? formCtxt.getEntityType() : null;
	}

	public boolean isSysRecord() {
		return formCtxt != null && formCtxt.isSysForm();
	}

	public boolean isMultiRecord() {
		return formCtxt != null && formCtxt.isMultiRecord();
	}

	public FormContextBean getFormCtxt() {
		return formCtxt;
	}

	public void setFormCtxt(FormContextBean formCtxt) {
		this.formCtxt = formCtxt;
	}

	public void delete() {
		setActivityStatus(Status.CLOSED);

		FormAuditManager auditMgr = new FormAuditManagerImpl();
		auditMgr.audit(getUserContext(), getDeForm(), Collections.emptyList(), "DELETE", getRecordId());
	}

	private UserContext getUserContext() {
		User currentUser = AuthUtil.getCurrentUser();
		return new UserContext() {
			@Override
			public Long getUserId() {
				return currentUser.getId();
			}

			@Override
			public String getUserName() {
				return currentUser.getUsername();
			}

			@Override
			public String getIpAddress() {
				return null;
			}
		};
	}

	private Container getDeForm() {
		Container container = new Container();
		container.setId(formCtxt.getForm().getId());
		container.setName(formCtxt.getForm().getName());
		return container;
	}
}