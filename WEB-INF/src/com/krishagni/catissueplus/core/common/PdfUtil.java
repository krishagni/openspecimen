package com.krishagni.catissueplus.core.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.TemplateService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

@Configurable
public class PdfUtil {

	private static final PdfUtil instance = new PdfUtil();

	@Autowired
	private TemplateService tmplSvc;

	public static PdfUtil getInstance() {
		return instance;
	}
	
	public String getText(InputStream in) {
		PdfReader reader = null;
		try {
			StringBuffer pdfText = new StringBuffer();
			reader = new PdfReader(in);
			int noOfPages = reader.getNumberOfPages();
			for (int i = 1; i <= noOfPages; i++) {
				pdfText.append(PdfTextExtractor.getTextFromPage(reader, i));
			}
			return pdfText.toString();
			
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			if (reader != null) {
				reader.close();
			}	
		}
	}

	public void toPdf(String template, Map<String, Object> model, File outputPdf) {
		File tmpDir = new File(ConfigUtil.getInstance().getDataDir(), "tmp");
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}

		File html = new File(tmpDir, UUID.randomUUID().toString() + ".html");
		tmplSvc.render(template, model, html);

		try (FileOutputStream out = new FileOutputStream(outputPdf)) {
			PdfRendererBuilder builder = new PdfRendererBuilder()
				.useFastMode()
				.withFile(html)
				.toStream(out);

			File fontsDir = new File(ConfigUtil.getInstance().getDataDir(), "fonts");
			if (fontsDir.exists()) {
				for (File fontsFile : Objects.requireNonNull(fontsDir.listFiles())) {
					builder.useFont(fontsFile, FilenameUtils.getBaseName(fontsFile.getName()));
				}
			}

			builder.run();
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			html.delete();
		}
	}
}