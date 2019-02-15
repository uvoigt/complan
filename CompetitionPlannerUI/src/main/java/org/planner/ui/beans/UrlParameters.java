package org.planner.ui.beans;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * Diese Klasse dient dem Kodieren von Statusinformationen als URL-Parameter. Falls ein Reload ausgeführt wird, können
 * diese Informationen zum erneuten Herstellen des Status verwendet werden.<br>
 * Sie beinhalten:
 * <ol>
 * <li>den mainContent-Pfad</li>
 * <li>die ID des aktuell bearbeiteten Objekts</li>
 * <li>ggf. weitere Objekte</li>
 * </ol>
 */
@Named
@RequestScoped
public class UrlParameters {
	private enum Type {
		aString, aLong;

		private static Type byNum(int num) {
			for (Type t : values()) {
				if (num == t.ordinal())
					return t;
			}
			throw new IllegalArgumentException(Integer.toString(num));
		}
	}

	private Object[] content;

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

	@PostConstruct
	public void init() {
		String string = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
				.getQueryString();
		if (string == null)
			return;
		try {
			byte[] buf = decodeBytes(string);
			xor(buf);
			content = new Object[buf[1]];
			MutableInt offset = new MutableInt(2);
			for (int i = 0; i < content.length && offset.intValue() < buf.length; i++) {
				Object object = getObject(buf, offset);
				set(i, object);
			}
		} catch (Exception e) {
			// unpassendes Token nach Server-Restart
		}
	}

	public String getEncoded() {
		// 13, die maximale Länge eines Longs mit radix 36
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// offset in RAND, bleibt unkodiert
		if (content != null) {
			out.write((int) (Math.random() * 255));
			out.write(content.length);
			for (Object object : content) {
				byte[] bytes = getBytes(object);
				out.write(bytes, 0, bytes.length);
			}
		}
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

	/*
	 * Offset 0: Länge ohne Typ Offset 1: Typ Offset 2: Inhalt
	 */
	private byte[] getBytes(Object object) {
		if (object == null)
			return new byte[] { 0 };
		Type type;
		String s;
		if (object instanceof Long) {
			s = Long.toString((Long) object, Character.MAX_RADIX);
			type = Type.aLong;
		} else if (object instanceof String) {
			s = (String) object;
			type = Type.aString;
		} else
			throw new IllegalArgumentException("Unsupported type: " + object.getClass());

		byte[] bs = s.getBytes(Charset.forName("UTF-8"));
		byte[] result = new byte[bs.length + 2];
		result[0] = (byte) bs.length;
		result[1] = (byte) type.ordinal();
		System.arraycopy(bs, 0, result, 2, bs.length);
		return result;
	}

	private Object getObject(byte[] buf, MutableInt offset) {
		int length = buf[offset.intValue()];
		if (length == 0)
			return null;
		Type type = Type.byNum(buf[offset.intValue() + 1]);
		String string = new String(buf, offset.intValue() + 2, length, Charset.forName("UTF-8"));
		offset.add(length + 2);
		if (type == Type.aLong)
			return Long.parseLong(string, Character.MAX_RADIX);
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
		return (String) get(0);
	}

	public void setMainContent(String mainContent) {
		set(0, mainContent);
	}

	public Long getId() {
		return (Long) get(1);
	}

	public void setId(Long id) {
		set(1, id);
	}

	public Object get(int offset) {
		return content != null && content.length > offset ? content[offset] : null;
	}

	public void set(int offset, Object value) {
		if (content == null)
			content = new Object[offset + 1];
		if (content.length <= offset) {
			int size = content.length;
			System.arraycopy(content, 0, content = new Object[offset + 1], 0, size);
		}
		content[offset] = value;
	}
}
