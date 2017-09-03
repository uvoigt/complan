package org.planner.ui.beans;

import java.io.InputStream;
import java.io.OutputStream;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.planner.ui.util.JsfUtil;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 * Verantwortlich für Download und Upload von Daten.
 * 
 * @author Uwe Voigt - IBM
 */
public class UploadBean {

	interface DownloadHandler {
		void handleDownload(OutputStream out, String typ) throws Exception;

		String getDownloadFileName();
	}

	interface UploadHandler {
		void handleUpload(InputStream in, String typ) throws Exception;
	}

	interface UploadProcessor {
		int getUploadedSize();

		void processUploaded();

		void deleteUploaded();
	}

	private DownloadHandler downloadHandler;
	private UploadHandler uploadHandler;
	private UploadProcessor processor;

	UploadBean(DownloadHandler downloadHandler, UploadHandler uploadHandler, UploadProcessor processor) {
		this.downloadHandler = downloadHandler;
		this.uploadHandler = uploadHandler;
		this.processor = processor;
	}

	public int getUploadedSize() {
		return processor != null ? processor.getUploadedSize() : 0;
	}

	public void deleteUploaded() {
		if (processor != null)
			processor.deleteUploaded();
	}

	public void processUploaded() {
		if (processor != null)
			processor.processUploaded();
	}

	/**
	 * Da &lt;p:fileDownload&gt; im Falle des Stream-Close den
	 * <code>InputStream</code> des <code>StreamedContent</code> nicht schließt,
	 * muss das re-implementiert werden.
	 * 
	 * @param typ
	 * @return
	 * @throws Exception
	 */
	public void download(String typ) throws Exception {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();

		String contentDispositionValue = "attachment";

		externalContext.setResponseContentType("application/csv");
		externalContext.setResponseCharacterEncoding("iso-8859-1");
		externalContext.setResponseHeader("Content-Disposition",
				contentDispositionValue + ";filename=\"" + downloadHandler.getDownloadFileName() + "\"");

		if (RequestContext.getCurrentInstance().isSecure()) {
			externalContext.setResponseHeader("Cache-Control", "public");
			externalContext.setResponseHeader("Pragma", "public");
		}

		OutputStream out = externalContext.getResponseOutputStream();

		try {

			downloadHandler.handleDownload(out, typ);
			externalContext.setResponseStatus(HttpServletResponse.SC_OK);
			externalContext.responseFlushBuffer();

		} catch (Exception e) {
			externalContext.setResponseStatus(HttpServletResponse.SC_NO_CONTENT);
		} finally {
			facesContext.responseComplete();
		}
	}

	/**
	 * Upload-Listener für &lt;p:fileUpload&gt;
	 * 
	 * @param event
	 */
	public void upload(FileUploadEvent event) {
		UploadedFile file = event.getFile();
		String typ = (String) JsfUtil.getContextVariable("typ");
		String contentType = file.getContentType();
		try {
			if ("application/vnd.ms-excel".equals(contentType) || "application/csv".equals(contentType)) {
				uploadAndCloseStream(file.getInputstream(), typ);
			} else {
				throw new IllegalArgumentException(contentType);
			}
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					// TODO messageBundle
					new FacesMessage(null, "Upload fehlgeschlagen: " + e.getMessage()));
		} finally {
		}
	}

	private void uploadAndCloseStream(InputStream in, String typ) throws Exception {
		try {
			uploadXml(in, typ);
		} finally {
			in.close();
		}
	}

	private void uploadXml(InputStream in, String typ) throws Exception {
		uploadHandler.handleUpload(in, typ);
	}
}