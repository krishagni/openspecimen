package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.de.domain.DeObject;

@Audited
public abstract class BaseExtensionEntity extends BaseEntity {
	private DeObject extension;

	protected Integer extensionRev;

	public DeObject getExtensionIfPresent() {
		return extension;
	}

	public DeObject getExtension() {
		if (extension == null) {
			extension = createExtension();
		}
		
		return extension;
	}

	public void setExtension(DeObject extension) {
		this.extension = extension;
	}
	
	public void addOrUpdateExtension() {
		if (extension == null || extension.getFormId() == null) {
			return;			
		}
		
		if (extension.saveOrUpdate()) {
			extensionRev = (extensionRev == null) ? 1 : extensionRev + 1;
		}
	}

	public Integer getExtensionRev() {
		return extensionRev;
	}

	public void setExtensionRev(Integer extensionRev) {
		this.extensionRev = extensionRev;
	}

	public boolean hasPhiFields() {
		return getExtension() != null && getExtension().hasPhiFields();
	}
	
	public void copyExtensionTo(BaseExtensionEntity entity) {
		if (getExtension() == null) {
			return;
		}
		
		getExtension().copyAttrsTo(entity.getExtension());
	}

	public DeObject createExtension() {
		DeObject extnObj = new DeObject() {	
			@Override
			public void setAttrValues(Map<String, Object> attrValues) {
				// TODO Auto-generated method stub				
			}
			
			@Override
			public Long getObjectId() {
				return BaseExtensionEntity.this.getId();
			}
			
			@Override
			public String getFormName() {
				return getFormNameByEntityType();
			}
			
			@Override
			public String getEntityType() {
				return BaseExtensionEntity.this.getEntityType();
			}
			
			@Override
			public Long getCpId() {
				return BaseExtensionEntity.this.getCpId();
			}

			@Override
			public boolean isCpBased() {
				return BaseExtensionEntity.this.isCpBased();
			}

			@Override
			public Long getEntityId() {
				return BaseExtensionEntity.this.getEntityId();
			}

			@Override
			public DataEntryStatus getDataEntryStatus() {
				DataEntryStatus status = BaseExtensionEntity.this.getDataEntryStatus();
				return status != null ? status : DataEntryStatus.COMPLETE;
			}
		};
		
		if (StringUtils.isBlank(extnObj.getFormName())) {
			return null;
		}
		
		if (getId() == null) {
			return extnObj;
		}
		
		Long recId = getRecordId(extnObj);
		extnObj.setId(recId);
		return extnObj;
	}
	
	public abstract String getEntityType();

	public Long getCpId() {
		return -1L;
	}

	public boolean isCpBased() {
		return true;
	}

	public Long getEntityId() {
		return null;
	}

	public String getCustomFieldDisplayValue(String udn) {
		DeObject extn = getExtension();
		if (extn == null) {
			return null;
		}

		return extn.getAttrDisplayValue(udn);
	}

	public Object getCustomFieldValue(String udn) {
		DeObject extn = getExtension();
		if (extn == null) {
			return null;
		}

		DeObject.Attr reqAttr = extn.getAttrs().stream().filter(attr -> attr.getUdn().equals(udn)).findFirst().orElse(null);
		return reqAttr != null ? extn.getAttrValue(reqAttr) : null;
	}
	
	private Long getRecordId(DeObject extnObj) {
		List<Long> recIds = extnObj.getRecordIds();
		if (CollectionUtils.isEmpty(recIds)) {
			return null;
		}
		
		return recIds.iterator().next();		
	}
}
