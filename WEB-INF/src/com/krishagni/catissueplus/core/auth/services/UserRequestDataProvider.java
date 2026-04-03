package com.krishagni.catissueplus.core.auth.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserRequestDataProvider {
	void setup(HttpServletRequest httpReq, HttpServletResponse httpResp);

	void teardown(HttpServletRequest httpReq, HttpServletResponse httpResp);
}
