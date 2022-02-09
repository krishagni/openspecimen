package com.krishagni.catissueplus.core.common.events;

import java.util.ArrayList;
import java.util.List;

public class DependentEntityDetail {
	private String name;
	
	private int count;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public static DependentEntityDetail from(String name, int count) {
		DependentEntityDetail detail = new DependentEntityDetail();
		detail.setName(name);
		detail.setCount(count);
		
		return detail;
	}
	
	public static List<DependentEntityDetail> singletonList(String name, int count) {
		return listBuilder()
				.add(name, count)
				.build();
	}

	public static DependentEntityDetailListBuilder listBuilder() {
		return new DependentEntityDetailListBuilder();
	}
	
	public static class DependentEntityDetailListBuilder {
		private List<DependentEntityDetail> dependentEntities = new ArrayList<>();
		
		public DependentEntityDetailListBuilder add(String name, Integer count) {
			if (count != null && count > 0) {
				dependentEntities.add(DependentEntityDetail.from(name, count));
			}
			
			return this;
		}
		
		public List<DependentEntityDetail> build() {
			return dependentEntities;
		}
	}
}
