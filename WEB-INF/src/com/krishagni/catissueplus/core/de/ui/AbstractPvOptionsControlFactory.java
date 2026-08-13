package com.krishagni.catissueplus.core.de.ui;

import java.util.Map;

import org.w3c.dom.Element;

import edu.common.dynamicextensions.nutility.ParserUtil;

public abstract class AbstractPvOptionsControlFactory extends AbstractPvControlFactory {

	//
	// Used for deserialisation from XML to Java based control object
	//
	@Override
	protected void setControlTypeProps(AbstractPvControl ctrl, Element ele) {
		AbstractPvOptionsControl optionsCtrl = (AbstractPvOptionsControl) ctrl;
		optionsCtrl.setOptionsPerRow(ParserUtil.getIntValue(ele, "optionsPerRow", 1));
		optionsCtrl.setMultiValued(isMultiValued());
	}

	//
	// Used for deserialisation from JSON based Map to Java based control object
	//
	@Override
	protected void setControlTypeProps(AbstractPvControl ctrl, Map<String, Object> props) {
		AbstractPvOptionsControl optionsCtrl = (AbstractPvOptionsControl) ctrl;
		optionsCtrl.setOptionsPerRow(getInt(props, "optionsPerRow", 1));
		optionsCtrl.setMultiValued(isMultiValued());
	}

	protected abstract boolean isMultiValued();
}
