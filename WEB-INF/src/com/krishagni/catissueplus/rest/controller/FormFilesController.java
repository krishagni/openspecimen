package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.FileDetail;
import com.krishagni.catissueplus.core.de.events.GetFileDetailOp;
import com.krishagni.catissueplus.core.de.services.FormService;

@Controller
@RequestMapping("/form-files")
public class FormFilesController {
	@Autowired
	private FormService formSvc;
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public FileDetail uploadFile(@PathVariable("file") MultipartFile file) {
		return response(formSvc.uploadFile(request(file)));
	}

	@RequestMapping(method = RequestMethod.POST, value="images")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public FileDetail uploadImage(@RequestBody Map<String, String> input) {
		return response(formSvc.uploadImage(request(input.get("dataUrl"))));
	}
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void downloadFile(
		@RequestParam(value = "fileId")
		String fileId,

		HttpServletResponse response) {

		GetFileDetailOp req = new GetFileDetailOp();
		req.setFileId(fileId);
		
		FileDetail file = response(formSvc.getFileDetail(request(req)));
		Utility.sendToClient(response, file.getFilename(), file.getContentType(), new File(file.getPath()));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{fileId:.+}")
	@ResponseStatus(HttpStatus.OK)
	public void downloadFilePathParam(
		@PathVariable("fileId")
		String fileId,

		HttpServletResponse response) {

		downloadFile(fileId, response);
	}

	private <T> RequestEvent<T> request(T payload) {
		return RequestEvent.wrap(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		return ResponseEvent.unwrap(resp);
	}
}
