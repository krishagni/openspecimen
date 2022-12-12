package com.krishagni.catissueplus.core.common.events;

import java.io.File;

public class ExportedFileDetail {
	private String name;
	
	private File file;

	private boolean completed;
	
	public ExportedFileDetail(String name, File file) {
		this.name = name;
		this.file = file;
		this.completed = true;
	}

	public ExportedFileDetail(boolean completed) {
		this.completed = completed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
}
