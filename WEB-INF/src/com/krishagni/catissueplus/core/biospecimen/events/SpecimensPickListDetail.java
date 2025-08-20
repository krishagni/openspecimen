package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimensPickList;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecimensPickListDetail {
	private Long id;

	private String name;

	private SpecimenListSummary cart;

	private UserSummary creator;

	private Date creationTime;

	private Long totalSpecimens;

	private Long pickedSpecimens;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SpecimenListSummary getCart() {
		return cart;
	}

	public void setCart(SpecimenListSummary cart) {
		this.cart = cart;
	}

	public UserSummary getCreator() {
		return creator;
	}

	public void setCreator(UserSummary creator) {
		this.creator = creator;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Long getTotalSpecimens() {
		return totalSpecimens;
	}

	public void setTotalSpecimens(Long totalSpecimens) {
		this.totalSpecimens = totalSpecimens;
	}

	public Long getPickedSpecimens() {
		return pickedSpecimens;
	}

	public void setPickedSpecimens(Long pickedSpecimens) {
		this.pickedSpecimens = pickedSpecimens;
	}

	public static SpecimensPickListDetail from(SpecimensPickList list) {
		return SpecimensPickListDetail.from(list, true);
	}

	public static SpecimensPickListDetail from(SpecimensPickList list, boolean includeCartDetail) {
		SpecimensPickListDetail result = new SpecimensPickListDetail();
		result.setId(list.getId());
		result.setName(list.getName());
		result.setCreator(UserSummary.from(list.getCreator()));
		result.setCreationTime(list.getCreationTime());
		if (includeCartDetail) {
			result.setCart(SpecimenListSummary.fromSpecimenList(list.getCart()));
		}

		return result;
	}

	public static List<SpecimensPickListDetail> from(Collection<SpecimensPickList> lists) {
		return Utility.nullSafeStream(lists).map(list -> SpecimensPickListDetail.from(list, false)).collect(Collectors.toList());
	}
}
