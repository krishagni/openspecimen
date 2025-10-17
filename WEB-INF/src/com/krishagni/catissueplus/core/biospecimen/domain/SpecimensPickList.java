package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashSet;
import java.util.Set;

public class SpecimensPickList extends BaseEntity {
	private String name;

	private SpecimenList cart;

	private Boolean transferToBox;

	private Set<PickedSpecimen> pickedSpecimens = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SpecimenList getCart() {
		return cart;
	}

	public void setCart(SpecimenList cart) {
		this.cart = cart;
	}

	public Boolean getTransferToBox() {
		return transferToBox;
	}

	public void setTransferToBox(Boolean transferToBox) {
		this.transferToBox = transferToBox;
	}

	public Set<PickedSpecimen>  getPickedSpecimens() {
		return pickedSpecimens;
	}

	public void setPickedSpecimens(Set<PickedSpecimen> pickedSpecimens) {
		this.pickedSpecimens = pickedSpecimens;
	}

	public String getDescription() {
		return "#" + getId() + " " + getName();
	}
}
