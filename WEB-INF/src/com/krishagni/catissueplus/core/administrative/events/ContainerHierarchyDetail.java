package com.krishagni.catissueplus.core.administrative.events;

import java.util.List;

public class ContainerHierarchyDetail extends StorageContainerDetail {
	private int numOfContainers;

	private List<String> names;

	public int getNumOfContainers() {
		return numOfContainers;
	}

	public void setNumOfContainers(int numOfContainers) {
		this.numOfContainers = numOfContainers;
	}

	public List<String> getNames() {
		return names;
	}

	public void setNames(List<String> names) {
		this.names = names;
	}
}
