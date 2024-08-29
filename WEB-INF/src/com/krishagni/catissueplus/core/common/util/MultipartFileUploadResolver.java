package com.krishagni.catissueplus.core.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class MultipartFileUploadResolver extends CommonsMultipartResolver {

	protected CommonsMultipartFile createMultipartFile(FileItem fileItem) {
		try {
			final String contentType = Utility.getContentType(fileItem.getInputStream());
			String detectedFileType = Utility.getFileType(contentType).substring(1).toLowerCase();
			String allowedTypesStr = ConfigUtil.getInstance().getStrSetting("common", "allowed_file_types", "");
			Set<String> allowedTypes = Arrays.stream(allowedTypesStr.split(","))
				.map(type -> type.trim().toLowerCase())
				.collect(Collectors.toSet());
			if (!allowedTypes.contains(detectedFileType)) {
				throw OpenSpecimenException.userError(
					CommonErrorCode.INVALID_INPUT,
					"File with content type '" + contentType + "' is not allowed.");
			}

			String fileType = FilenameUtils.getExtension(fileItem.getName());
			if (StringUtils.isBlank(fileType)) {
				throw OpenSpecimenException.userError(
					CommonErrorCode.INVALID_INPUT,
					"File without extensions are not allowed. Filename:  " + fileItem.getName());
			}

			if (!allowedTypes.contains(fileType.toLowerCase())) {
				throw OpenSpecimenException.userError(
					CommonErrorCode.INVALID_INPUT,
					"File with extension '" + fileType + "' is not allowed.");
			}

			return super.createMultipartFile(new FileItemWrapper(fileItem, contentType));
		} catch (IOException e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private static class FileItemWrapper implements FileItem {

		private final FileItem item;

		private final String contentType;

		public FileItemWrapper(FileItem item, String contentType) {
			this.item = item;
			this.contentType = contentType;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return item.getInputStream();
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public String getName() {
			return item.getName();
		}

		@Override
		public boolean isInMemory() {
			return item.isInMemory();
		}

		@Override
		public long getSize() {
			return item.getSize();
		}

		@Override
		public byte[] get() {
			return item.get();
		}

		@Override
		public String getString(String encoding) throws UnsupportedEncodingException {
			return item.getString(encoding);
		}

		@Override
		public String getString() {
			return item.getString();
		}

		@Override
		public void write(File file) throws Exception {
			item.write(file);
		}

		@Override
		public void delete() {
			item.delete();
		}

		@Override
		public String getFieldName() {
			return item.getFieldName();
		}

		@Override
		public void setFieldName(String name) {
			item.setFieldName(name);
		}

		@Override
		public boolean isFormField() {
			return item.isFormField();
		}

		@Override
		public void setFormField(boolean state) {
			item.setFormField(state);
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return item.getOutputStream();
		}

		@Override
		public FileItemHeaders getHeaders() {
			return item.getHeaders();
		}

		@Override
		public void setHeaders(FileItemHeaders headers) {
			item.setHeaders(headers);
		}
	}
}
