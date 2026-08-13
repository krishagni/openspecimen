package com.krishagni.catissueplus.core.de.ui;

import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import edu.common.dynamicextensions.domain.nui.AbstractLookupControl;
import edu.common.dynamicextensions.ndao.JdbcDaoFactory;
import edu.common.dynamicextensions.nutility.XmlUtil;

public abstract class AbstractPvControl extends AbstractLookupControl implements Serializable {
	private static final long serialVersionUID = 1L;

	private String attribute;

	private boolean leafNode;

	private boolean rootNode;

	private String defaultValue;

	private boolean hasNumericValues;

	private String formPvCaption;

	private String formPvOptionsFile;

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public boolean isLeafNode() {
		return leafNode;
	}

	public void setLeafNode(boolean leafNode) {
		this.leafNode = leafNode;
	}

	public boolean isRootNode() {
		return rootNode;
	}

	public void setRootNode(boolean rootNode) {
		this.rootNode = rootNode;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean getHasNumericValues() {
		return hasNumericValues;
	}

	public void setHasNumericValues(boolean hasNumericValues) {
		this.hasNumericValues = hasNumericValues;
	}

	public String getFormPvCaption() {
		return formPvCaption;
	}

	public void setFormPvCaption(String formPvCaption) {
		this.formPvCaption = formPvCaption;
	}

	public String getFormPvOptionsFile() {
		return formPvOptionsFile;
	}

	public void setFormPvOptionsFile(String formPvOptionsFile) {
		this.formPvOptionsFile = formPvOptionsFile;
	}

	public static String getFormPvOptionsFile(String attribute) {
		return FORM_PV_FILE_PREFIX + attribute + ".csv";
	}

	//
	// Returns PV ID given the PV display value or stringified ID as the input
	//
	@Override
	public Long fromString(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		try {
			Long pvId = Long.parseLong(value);
			Long idByValue = hasNumericValues ? getIdByValue(value) : null;
			return idByValue != null ? idByValue : pvId;
		} catch (NumberFormatException nfe) {
			return getIdByValue(value);
		}
	}

	@Override
	public String getTableName() {
		return PV_TABLE;
	}

	@Override
	public String getValueColumn() {
		return VALUE_COLUMN;
	}

	@Override
	public String getAltKeyColumn() {
		return ALT_KEY;
	}

	@Override
	public Properties getPvSourceProps() {
		Map<String, Object> filters = new HashMap<>();
		filters.put("attribute", attribute);
		filters.put("includeOnlyLeafValue", leafNode);
		filters.put("includeOnlyRootValue", rootNode);

		Properties props = new Properties();
		props.put("apiUrl", "rest/ng/permissible-values");
		props.put("searchTermName", "searchString");
		props.put("resultFormat", "{{value}}");
		props.put("filters", filters);
		props.put("useDisplayValue", "true");
		return props;
	}

	@Override
	public String getCodeColumn() {
		return CONCEPT_CODE;
	}

	@Override
	public final void serializeToXml(Writer writer, Properties props) {
		XmlUtil.writeElementStart(writer, getCtrlType());
		super.serializeToXml(writer, props);
		serializeLookupProps(writer);
		XmlUtil.writeElement(writer, "attribute", attribute);
		XmlUtil.writeElement(writer, "leafValue", leafNode);
		XmlUtil.writeElement(writer, "rootValue", rootNode);
		XmlUtil.writeElement(writer, "numericValues", hasNumericValues);
		XmlUtil.writeCDataElement(writer, "defaultValue", defaultValue);
		serializePvControlProps(writer);
		serializeFormPv(writer);
		XmlUtil.writeElementEnd(writer, getCtrlType());
	}

	//
	// Used to create JSON representation of the control
	//
	@Override
	protected void getLookupProps(Map<String, Object> props) {
		props.put("apiUrl", "rest/ng/permissible-values");
		props.put("dataType", getDataType());
		props.put("attribute", attribute);
		props.put("leafValue", leafNode);
		props.put("rootValue", rootNode);
		props.put("numericValues", hasNumericValues);
		props.put("defaultValue", defaultValue);
		getPvControlProps(props);
	}

	protected void getPvControlProps(Map<String, Object> props) {
	}

	protected void serializePvControlProps(Writer writer) {
	}

	private Long getIdByValue(String value) {
		return JdbcDaoFactory.getJdbcDao().getResultSet(
			GET_ID_BY_VALUE,
			Arrays.asList(attribute, value, value),
			(rs) -> rs.next() ? rs.getLong(1) : null
		);
	}

	private void serializeFormPv(Writer writer) {
		String caption = JdbcDaoFactory.getJdbcDao().getResultSet(
			GET_FORM_PV_CAPTION,
			Arrays.asList(attribute),
			(rs) -> rs.next() ? rs.getString(1) : null
		);

		if (StringUtils.isBlank(caption)) {
			return;
		}

		XmlUtil.writeElementStart(writer, "formPv");
		XmlUtil.writeCDataElement(writer, "caption", caption);
		XmlUtil.writeElement(writer, "optionsFile", getFormPvOptionsFile(attribute));
		XmlUtil.writeElementEnd(writer, "formPv");
	}

	private static final String PV_TABLE = "CATISSUE_PERMISSIBLE_VALUE";

	private static final String VALUE_COLUMN = "VALUE";

	private static final String ALT_KEY = "VALUE";

	private static final String CONCEPT_CODE = "CONCEPT_CODE";

	private static final String FORM_PV_FILE_PREFIX = "form_pv_";

	private static final String GET_ID_BY_VALUE =
		"select " +
		"  identifier " +
		"from " +
		"  catissue_permissible_value pv " +
		"where " +
		"  pv.public_id = ? and " +
		"  (pv.value = ? or pv.concept_code = ?)";

	private static final String GET_FORM_PV_CAPTION =
		"select " +
		"  long_name " +
		"from " +
		"  catissue_cde " +
		"where " +
		"  public_id = ? and " +
		"  form_id is not null and " +
		"  deleted_on is null";
}
