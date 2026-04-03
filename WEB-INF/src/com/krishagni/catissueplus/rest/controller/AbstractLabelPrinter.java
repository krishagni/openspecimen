package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public abstract class AbstractLabelPrinter {

	@RequestMapping(method = RequestMethod.GET, value = "/output-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getLabelPrintFiles(
		@RequestParam(value = "jobId")
		Long jobId,

		@RequestParam(value = "filename", required = false, defaultValue = "")
		String filename,

		HttpServletResponse httpResp) {

		File printJobDir = new File(ConfigUtil.getInstance().getDataDir(), "print-jobs");
		File result      = new File(printJobDir, AuthUtil.getCurrentUser().getId() + "_" + jobId + ".csv");
		if (!result.exists()) {
			throw OpenSpecimenException.userError(CommonErrorCode.FILE_NOT_FOUND, result.getName());
		}

		filename = Utility.stripWs(filename);
		if (StringUtils.isBlank(filename)) {
			filename = result.getName();
		}

		httpResp.setContentType("application/csv");
		httpResp.setHeader("Content-Disposition", "attachment; filename=" + filename);

		InputStream in = null;
		try {
			in = new FileInputStream(result);
			IOUtils.copy(in, httpResp.getOutputStream());
		} catch (IOException e) {
			throw OpenSpecimenException.userError(CommonErrorCode.FILE_SEND_ERROR, e.getMessage());
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}
