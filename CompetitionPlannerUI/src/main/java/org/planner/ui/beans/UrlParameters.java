package org.planner.ui.beans;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableInt;

/**
 * Diese Klasse dient dem Kodieren von Statusinformationen als URL-Parameter. Falls ein Reload ausgeführt wird, können
 * diese Informationen zum erneuten Herstellen des Status verwendet werden.<br>
 * Sie beinhalten:
 * <ol>
 * <li>den mainContent-Pfad</li>
 * <li>die ID des aktuell bearbeiteten Objekts</li>
 * <li>die Version des aktuell bearbeiteten Objekts</li>
 * </ol>
 */
@Named
@RequestScoped
public class UrlParameters {

	private Object[] content = new Object[3];
	private Class<?>[] types = { String.class, Long.class, Integer.class };
	private boolean initialized;

	private static final byte[] RAND;

	static {
		byte[] buf = new byte[735];
		try {
			IOUtils.read(UrlParameters.class.getResourceAsStream("/obfuscate.bin"), buf);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		RAND = buf;
	}

	public void init() {
		if (initialized)
			return;
		initialized = true;
		String string = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("i");
		if (string == null)
			return;
		byte[] buf = decodeBytes(string);
		int start = 0;
		start++;
		xor(buf);
		try {
			MutableInt offset = new MutableInt(start);
			for (int i = 0; i < content.length && offset.intValue() < buf.length; i++) {
				Object object = getObject(buf, offset, types[i]);
				content[i] = object;
			}
		} catch (Exception e) {
			// unpassendes Token nach Server-Restart
		}
	}

	public String getEncoded() {
		// // 13, die maximale Länge eines Longs mit radix 36
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// // offset in RAND, bleibt unkodiert
		out.write((int) (Math.random() * 255));
		for (Object object : content) {
			byte[] bytes = getBytes(object);
			out.write(bytes.length);
			out.write(bytes, 0, bytes.length);
		}
		// auffüllen
		while ((out.size() % 3) > 0)
			out.write((int) (Math.random() * 255));
		byte[] buf = out.toByteArray();
		xor(buf);
		return encodeBytes(buf);
	}

	private String encodeBytes(byte[] bytes) {
		return DatatypeConverter.printHexBinary(bytes);
	}

	private byte[] decodeBytes(String string) {
		return DatatypeConverter.parseHexBinary(string);
	}

	private byte[] getBytes(Object object) {
		if (object instanceof Long)
			object = Long.toString((Long) object, Character.MAX_RADIX);
		else if (object instanceof Integer)
			object = Integer.toString((Integer) object, Character.MAX_RADIX);

		if (object instanceof String)
			return ((String) object).getBytes(Charset.forName("UTF-8"));
		return new byte[0];
	}

	private Object getObject(byte[] buf, MutableInt offset, Class<?> type) {
		int length = buf[offset.intValue()];
		if (length == 0)
			return null;
		String string = new String(buf, offset.intValue() + 1, length, Charset.forName("UTF-8"));
		offset.add(length + 1);
		if (Long.class.equals(type))
			return Long.parseLong(string, Character.MAX_RADIX);
		if (Integer.class.equals(type))
			return Integer.parseInt(string, Character.MAX_RADIX);
		return string;
	}

	private void xor(byte[] buf) {
		for (int i = 1, j = buf[0] & 255; i < buf.length; i++, j++) {
			if (j >= RAND.length)
				j = 0;
			buf[i] = (byte) (buf[i] ^ RAND[j]);
		}
	}

	public String getMainContent() {
		return (String) content[0];
	}

	public void setMainContent(String mainContent) {
		content[0] = mainContent;
	}

	public Long getId() {
		return (Long) content[1];
	}

	public void setId(Long id) {
		content[1] = id;
	}

	public Integer getVersion() {
		return (Integer) content[2];
	}

	public void setId(Integer version) {
		content[2] = version;
	}
}
