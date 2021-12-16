package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.FileNotFoundException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Controller
@RequestMapping("/docs")
public class DocsController {
	private static final LogUtil logger = LogUtil.getLogger(DocsController.class);

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	void download(@RequestParam String filename, HttpServletResponse httpResp) {
		try {
			filename = Utility.cleanPath(filename);

			Resource resource = new PathMatchingResourcePatternResolver().getResource("/docs/" + filename);
			if (resource == null) {
				throw OpenSpecimenException.userError(CommonErrorCode.FILE_NOT_FOUND, filename);
			}

			File file = resource.getFile();
			if (file.isDirectory()) {
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, filename);
			}

			Utility.sendToClient(httpResp, filename, file);
		} catch (Exception e) {
			if (e instanceof FileNotFoundException) {
				throw OpenSpecimenException.userError(CommonErrorCode.FILE_NOT_FOUND, filename);
			}

			if (!(e instanceof OpenSpecimenException)) {
				logger.error("Error downloading doc/file", e);
				throw OpenSpecimenException.serverError(e);
			}
		}
	}
}