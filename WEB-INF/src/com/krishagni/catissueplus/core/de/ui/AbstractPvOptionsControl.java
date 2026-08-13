package com.krishagni.catissueplus.core.de.ui;

import java.io.Writer;
import java.util.Map;

import edu.common.dynamicextensions.nutility.XmlUtil;

public abstract class AbstractPvOptionsControl extends AbstractPvControl {
	private static final long serialVersionUID = 1L;

	private int optionsPerRow = 1;

	public int getOptionsPerRow() {
		return optionsPerRow;
	}

	public void setOptionsPerRow(int optionsPerRow) {
		this.optionsPerRow = optionsPerRow;
	}

	//
	// Used in serialisation of control to JSON or Map based output
	//
	@Override
	protected void getPvControlProps(Map<String, Object> props) {
		props.put("optionsPerRow", optionsPerRow);
	}

	//
	// Used in serialisation of control to XML
	//
	@Override
	protected void serializePvControlProps(Writer writer) {
		XmlUtil.writeElement(writer, "optionsPerRow", optionsPerRow);
	}
}
