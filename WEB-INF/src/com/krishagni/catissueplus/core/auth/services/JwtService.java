package com.krishagni.catissueplus.core.auth.services;

import com.krishagni.catissueplus.core.administrative.domain.User;

public interface JwtService {
	User resolveUser(String token);
}
