package com.krishagni.catissueplus.core.de.services;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import edu.common.dynamicextensions.domain.nui.Container;

public class FormDefinitionFileProcessors {
	private static final FormDefinitionFileProcessors instance = new FormDefinitionFileProcessors();

	private final List<FormDefinitionFileProcessor> processors = new CopyOnWriteArrayList<>();

	private FormDefinitionFileProcessors() {
	}

	public static FormDefinitionFileProcessors getInstance() {
		return instance;
	}

	public void addProcessor(FormDefinitionFileProcessor processor) {
		if (!processors.contains(processor)) {
			processors.add(processor);
		}
	}

	public void removeProcessor(FormDefinitionFileProcessor processor) {
		processors.remove(processor);
	}

	public void exportForm(Container form, String outputDir) {
		processors.forEach(processor -> processor.exportForm(form, outputDir));
	}

	public boolean importForm(Container form, String inputDir) {
		boolean changed = false;
		for (FormDefinitionFileProcessor processor : processors) {
			changed |= processor.importForm(form, inputDir);
		}

		return changed;
	}

	public void validateForm(Container form) {
		processors.forEach(processor -> processor.validateForm(form));
	}
}
