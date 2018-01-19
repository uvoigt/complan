package org.planner.ui.beans;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.planner.ui.util.JsfUtil;
import org.primefaces.PrimeFaces;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 * Verantwortlich für Download und Upload von Daten.
 * 
 * @author Uwe Voigt - IBM
 */
public class UploadBean {

	public interface DownloadHandler {
		void handleDownload(OutputStream out, String typ, Object selection) throws Exception;

		String getDownloadFileName(Object selection);
	}

	public interface UploadHandler {
		void handleUpload(InputStream in, String typ) throws Exception;
	}

	interface UploadProcessor {
		int getUploadedSize();

		void processUploaded();

		void deleteUploaded();
	}

	private static class Content implements Serializable {

		private static final long serialVersionUID = 1L;

		private String name;
		private byte[] bytes;

		private Content(String name, byte[] bytes) {
			this.name = name;
			this.bytes = bytes;
		}
	}

	private DownloadHandler downloadHandler;
	private UploadHandler uploadHandler;
	private UploadProcessor processor;

	public UploadBean(DownloadHandler downloadHandler, UploadHandler uploadHandler, UploadProcessor processor) {
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

	public void create(String typ, Object selection) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String name;
		try {
			name = downloadHandler.getDownloadFileName(selection);
			downloadHandler.handleDownload(out, typ, selection);
		} catch (Exception e) {
			FacesContext.getCurrentInstance().validationFailed();
			throw e;
		}
		Map<String, Object> map = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
		String token = UUID.randomUUID().toString();
		map.put(token, new Content(name, out.toByteArray()));
		PrimeFaces.current().ajax().addCallbackParam("token", token);
	}

	/**
	 * Da &lt;p:fileDownload&gt; im Falle des Stream-Close den <code>InputStream</code> des <code>StreamedContent</code>
	 * nicht schließt, muss das re-implementiert werden.
	 * 
	 */
	public void download(String typ, String contentType, String encoding, Object selection) throws Exception {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();

		String token = externalContext.getRequestParameterMap().get("token");
		Content content = token != null ? (Content) externalContext.getSessionMap().remove(token) : null;

		String contentDispositionValue = "attachment";

		externalContext.setResponseContentType(contentType);
		externalContext.setResponseCharacterEncoding(encoding);
		externalContext.setResponseHeader("Content-Disposition", contentDispositionValue + ";filename=\""
				+ (content != null ? content.name : downloadHandler.getDownloadFileName(selection)) + "\"");

		if (RequestContext.getCurrentInstance().isSecure()) {
			externalContext.setResponseHeader("Cache-Control", "public");
			externalContext.setResponseHeader("Pragma", "public");
		}

		OutputStream out = externalContext.getResponseOutputStream();

		try {
			if (content != null)
				out.write(content.bytes);
			else
				downloadHandler.handleDownload(out, typ, selection);
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