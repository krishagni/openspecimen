package com.krishagni.catissueplus.core.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class MultipartFileUploadResolver extends CommonsMultipartResolver {

	protected CommonsMultipartFile createMultipartFile(FileItem fileItem) {
		try {
			String contentType = Utility.ensureFileUploadAllowed(fileItem.getName(), fileItem.getInputStream());
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
