package com.krishagni.catissueplus.core.de.services;

import edu.common.dynamicextensions.domain.nui.Container;

public interface FormDefinitionFileProcessor {
	default void exportForm(Container form, String outputDir) { }

	default boolean importForm(Container form, String inputDir) {
		return false;
	}

	default void validateForm(Container form) { }
}
