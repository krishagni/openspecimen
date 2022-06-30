
package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Date;

import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class Password implements Comparable<Password>{

	private Long id;

	private String password;

	private User updatedBy;

	private Date updationDate;

	private User user;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public User getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Date getUpdationDate() {
		return updationDate;
	}

	public User getUser() {
		return user;
	}

	public void setUpdationDate(Date updationDate) {
		this.updationDate = updationDate;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public int compareTo(Password passwd) {
		return passwd.getId().compareTo(this.getId());
	}

	public int daysBeforeExpiry() {
		int passwdExpiryDays = ConfigUtil.getInstance().getIntSetting("auth", "password_expiry_days", 0);
		if (passwdExpiryDays == 0) {
			return -1;
		}

		Calendar cal = Calendar.getInstance();
		Date presentDate = Utility.chopTime(cal.getTime());

		cal.setTime(getUpdationDate());
		cal.add(Calendar.DATE, passwdExpiryDays);
		Date passwdExpiryDate = Utility.getEndOfDay(cal.getTime());
		return Utility.daysBetween(presentDate, passwdExpiryDate);
	}
}
