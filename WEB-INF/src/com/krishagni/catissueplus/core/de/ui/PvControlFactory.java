package com.krishagni.catissueplus.core.de.ui;

import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Element;

import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.factory.AbstractLookupControlFactory;
import edu.common.dynamicextensions.nutility.ParserUtil;

public class PvControlFactory extends AbstractLookupControlFactory {
	public static PvControlFactory getInstance() {
		return new PvControlFactory();
	}

	@Override
	public String getType() {
		return "pvField";
	}

	@Override
	public Control parseControl(Element ele, int row, int xPos, Properties props) {
		PvControl ctrl = (PvControl) super.parseControl(ele, row, xPos, props);
		ctrl.setAttribute(ParserUtil.getTextValue(ele, "attribute"));
		ctrl.setLeafNode(ParserUtil.getBooleanValue(ele, "leafValue"));
		ctrl.setRootNode(ParserUtil.getBooleanValue(ele, "rootValue"));
		ctrl.setHasNumericValues(ParserUtil.getBooleanValue(ele, "numericValues"));
		ctrl.setDefaultValue(ParserUtil.getTextValue(ele, "defaultValue"));
		return ctrl;
	}

	@Override
	public Control parseControl(Map<String, Object> props, int row, int xPos) {
		PvControl ctrl = new PvControl();
		super.setControlProps(ctrl, props, row, xPos);
		ctrl.setAttribute((String) props.get("attribute"));
		ctrl.setLeafNode(getBool(props, "leafValue"));
		ctrl.setRootNode(getBool(props, "rootValue"));
		ctrl.setHasNumericValues(getBool(props, "numericValues"));
		ctrl.setDefaultValue((String) props.get("defaultValue"));
		return ctrl;
	}

	@Override
	protected Control createControl() {
		return new PvControl();
	}
}