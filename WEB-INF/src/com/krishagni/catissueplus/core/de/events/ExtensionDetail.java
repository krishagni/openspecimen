package com.krishagni.catissueplus.core.de.events;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.DeObject.Attr;
import com.krishagni.catissueplus.core.exporter.services.impl.ExporterContextHolder;

public class ExtensionDetail implements Serializable {
	private static final LogUtil logger = LogUtil.getLogger(ExtensionDetail.class);

	private Long id;
	
	private Long objectId;
	
	private Long formId;

	private String formCaption;

	private String dataEntryStatus;
	
	private List<AttrDetail> attrs = new ArrayList<>();

	private Map<String, Object> attrsMap;

	private boolean useUdn;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}
	
	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public String getFormCaption() {
		return formCaption;
	}

	public void setFormCaption(String formCaption) {
		this.formCaption = formCaption;
	}

	public String getDataEntryStatus() {
		return dataEntryStatus;
	}

	public void setDataEntryStatus(String dataEntryStatus) {
		this.dataEntryStatus = dataEntryStatus;
	}

	public List<AttrDetail> getAttrs() {
		return attrs;
	}

	public void setAttrs(List<AttrDetail> attrs) {
		this.attrs = attrs;
		this.attrsMap = null;
	}

	@JsonProperty
	public void setAttrsMap(Map<String, Object> attrsMap) {
		attrs.clear();
		
		for (Map.Entry<String, Object> entry : attrsMap.entrySet()) {
			AttrDetail attr = new AttrDetail();
			attr.setName(entry.getKey()); 
			attr.setValue(entry.getValue());
			attrs.add(attr);
		}

		this.attrsMap = null;
	}

	@JsonIgnore
	public Map<String, Object> getAttrsMap() {
		if (attrsMap == null) {
			attrsMap = getAttrsMap(attrs);
		}

		return attrsMap;
	}

	@JsonIgnore
	public AttrDetail getAttr(String name) {
		return Utility.nullSafeStream(attrs).filter(a -> a.getName().equals(name))
			.findFirst().orElse(null);
	}

	@JsonIgnore
	public Object getAttrValue(String name) {
		AttrDetail attr = getAttr(name);
		if (attr == null || attr.getValue() == null) {
			return null;
		}

		if ("datePicker".equals(attr.getType())) {
			if (attr.getValue() instanceof String valueStr) {
				try {
					long time = Long.parseLong(valueStr);
					return new Date(time);
				} catch (Exception e) {
					try {
						// let's assume the input is in ISO format - yyyy-MM-dd'T'HH:mm:ss.SSSZ
						return Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(valueStr)));
					} catch (Exception e1) {
						try {
							// let's assume the input is in ISO format - yyyy-MM-dd
							return new SimpleDateFormat("yyyy-MM-dd").parse(valueStr);
						} catch (Exception e2) {
							logger.error("Error converting the value " + attr.getValue() + " to date object. Error: " + e.getMessage(), e);
							logger.error("Error converting the value " + attr.getValue() + " to date object. Error: " + e1.getMessage(), e1);
							logger.error("Error converting the value " + attr.getValue() + " to date object. Error: " + e2.getMessage(), e2);
						}
					}
				}
			} else if (attr.getValue() instanceof Number) {
				return new Date(((Number) attr.getValue()).longValue());
			}
		}

		return attr.getValue();
	}

	@JsonIgnore
	public Object getAttrDisplayValue(String name) {
		AttrDetail attr = getAttr(name);
		return attr != null ? attr.getDisplayValue() : null;
	}

	public boolean isUseUdn() {
		return useUdn;
	}

	public void setUseUdn(boolean useUdn) {
		this.useUdn = useUdn;
	}

	public static ExtensionDetail from(DeObject extension) {
		return from(extension, false);
	}

	public static ExtensionDetail from(DeObject extension, boolean excludePhi) {
		return from(extension, excludePhi, false);
	}

	public static ExtensionDetail from(DeObject extension, boolean excludePhi, boolean lenient) {
		if (extension == null || (!lenient && extension.getId() == null)) {
			return null;
		}
		
		ExtensionDetail detail = new ExtensionDetail();
		detail.setId(extension.getId());
		detail.setObjectId(extension.getObjectId());
		detail.setFormId(extension.getFormId());
		detail.setFormCaption(extension.getFormCaption());
		detail.setDataEntryStatus(extension.getDataEntryStatus() != null ? extension.getDataEntryStatus().name() : BaseEntity.DataEntryStatus.COMPLETE.name());
		detail.setAttrs(AttrDetail.from(extension.getAttrs(), excludePhi));	
		return detail;
	}

	private static Map<String, Object> getAttrsMap(List<AttrDetail> attrs) {
		Map<String, Object> attrsMap = new HashMap<>();
		for (AttrDetail attr : attrs) {
			if ("subForm".equals(attr.getType())) {
				List value = (List) attr.getValue();
				if (CollectionUtils.isEmpty(value)) {
					continue;
				}

				if (value.get(0) instanceof AttrDetail) {
					attrsMap.put(attr.getName(), getAttrsMap((List<AttrDetail>) value));
				} else if (value.get(0) instanceof List) {
					List<Map<String, Object>> sfAttrsMap = ((List<List<AttrDetail>>) value).stream()
							.map(ExtensionDetail::getAttrsMap).collect(Collectors.toList());
					attrsMap.put(attr.getName(), sfAttrsMap);
				}
			} else if ("fileUpload".equals(attr.getType()) || attr.getValue() instanceof List || "booleanCheckbox".equals(attr.getType())) {
				attrsMap.put(attr.getName(), attr.getValue());
			} else if ("datePicker".equals(attr.getType()) && ExporterContextHolder.getInstance().isExportOp()) {
				attrsMap.put(attr.getName(), attr.getValue());
			} else {
				Object value = attr.getDisplayValue();
				if (value == null) {
					value = attr.getValue();
				}
				attrsMap.put(attr.getName(), value);
			}
		}

		return attrsMap;
	}
	
	public static class AttrDetail implements Serializable {
		private String name;
		
		private String udn;
		
		private String caption;
		
		private Object value;
		
		private String type;

		private String displayValue;

		private String codedValue;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getUdn() {
			return udn;
		}

		public void setUdn(String udn) {
			this.udn = udn;
		}

		public String getCaption() {
			return caption;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}
		
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getDisplayValue() {
			return displayValue;
		}

		public void setDisplayValue(String displayValue) {
			this.displayValue = displayValue;
		}

		public void setCodedValue(String codedValue) {
			this.codedValue = codedValue;
		}

		@SuppressWarnings({"unchecked" })
		public static AttrDetail from(Attr attr, boolean excludePhi) {
			AttrDetail detail = new AttrDetail();
			detail.setName(attr.getName());
			detail.setUdn(attr.getUdn());
			detail.setCaption(attr.getCaption());
			detail.setType(attr.getType());
			detail.setCodedValue(attr.getCodedValue());

			if (attr.isSubForm()) {
				if (attr.isOneToOne()) {
					detail.setValue(from((List)attr.getValue(), excludePhi));
				} else {
					List<List<Attr>> sfAttrs = (List<List<Attr>>) attr.getValue();
					if (sfAttrs != null) {
						detail.setValue(sfAttrs.stream().map(sfAttr -> from(sfAttr, excludePhi)).collect(Collectors.toList()));
					}
				}
			} else if (excludePhi && attr.isPhi()) {
//				detail.setValue("###");
//				detail.setDisplayValue("###");
			} else {
				detail.setValue(attr.getValue());
				detail.setDisplayValue(attr.getDisplayValue());
			}

			return detail;
		}
		
		public static List<AttrDetail> from(List<Attr> attrs, boolean excludePhi) {
			return attrs.stream().map(attr -> from(attr, excludePhi)).collect(Collectors.toList());
		}
	}
}
