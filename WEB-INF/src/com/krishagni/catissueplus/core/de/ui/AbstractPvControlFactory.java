package com.krishagni.catissueplus.core.de.ui;

import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Element;

import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.factory.AbstractLookupControlFactory;
import edu.common.dynamicextensions.nutility.ParserUtil;

public abstract class AbstractPvControlFactory extends AbstractLookupControlFactory {
	//
	// Deserialise XML to Java based control object
	//
	@Override
	public Control parseControl(Element ele, int row, int xPos, Properties props) {
		AbstractPvControl ctrl = (AbstractPvControl) super.parseControl(ele, row, xPos, props);
		setPvControlProps(ctrl, ele);
		setControlTypeProps(ctrl, ele);
		return ctrl;
	}

	//
	// Deserialise JSON/Map to Java based control object
	//
	@Override
	public Control parseControl(Map<String, Object> props, int row, int xPos) {
		AbstractPvControl ctrl = (AbstractPvControl) super.parseControl(props, row, xPos);
		setPvControlProps(ctrl, props);
		setControlTypeProps(ctrl, props);
		return ctrl;
	}

	protected void setControlTypeProps(AbstractPvControl ctrl, Element ele) {
	}

	protected void setControlTypeProps(AbstractPvControl ctrl, Map<String, Object> props) {
	}

	private void setPvControlProps(AbstractPvControl ctrl, Element ele) {
		ctrl.setAttribute(ParserUtil.getTextValue(ele, "attribute"));
		ctrl.setLeafNode(ParserUtil.getBooleanValue(ele, "leafValue"));
		ctrl.setRootNode(ParserUtil.getBooleanValue(ele, "rootValue"));
		ctrl.setHasNumericValues(ParserUtil.getBooleanValue(ele, "numericValues"));
		ctrl.setDefaultValue(ParserUtil.getTextValue(ele, "defaultValue"));

		Element formPv = (Element) ele.getElementsByTagName("formPv").item(0);
		if (formPv != null) {
			ctrl.setFormPvCaption(ParserUtil.getTextValue(formPv, "caption"));
			ctrl.setFormPvOptionsFile(ParserUtil.getTextValue(formPv, "optionsFile"));
		}
	}

	private void setPvControlProps(AbstractPvControl ctrl, Map<String, Object> props) {
		ctrl.setAttribute((String) props.get("attribute"));
		ctrl.setLeafNode(getBool(props, "leafValue"));
		ctrl.setRootNode(getBool(props, "rootValue"));
		ctrl.setHasNumericValues(getBool(props, "numericValues"));
		ctrl.setDefaultValue((String) props.get("defaultValue"));
	}
}
