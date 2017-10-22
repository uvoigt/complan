package org.planner.ui.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

@WebFilter("*")
public class SubstituteFilter implements Filter {

	private class Wrapper extends HttpServletResponseWrapper {
		private class SavingStream extends ServletOutputStream {

			@Override
			public void write(int b) throws IOException {
				bo.write(b);
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(WriteListener writeListener) {
			}
		}

		private SavingStream out;
		private PrintWriter pw;
		private ByteArrayOutputStream bo = new ByteArrayOutputStream();

		private Wrapper(HttpServletResponse response) throws IOException {
			super(response);
			out = new SavingStream();
			pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return out;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return pw;
		}
	}

	private Pattern pattern = Pattern.compile("\\.js");

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String uri = ((HttpServletRequest) request).getRequestURI();
		if (pattern.matcher(uri).find()) {

			ServletOutputStream out = response.getOutputStream();
			Wrapper wrapper = new Wrapper((HttpServletResponse) response);
			chain.doFilter(request, wrapper);

			String result = substitute(new String(wrapper.bo.toByteArray(), Charset.forName("UTF-8")));

			byte[] content = result.getBytes(Charset.forName("UTF-8"));
			response.setContentLengthLong(content.length);
			out.write(content);
		} else {
			chain.doFilter(request, response);
		}
	}

	private String substitute(String content) throws ServletException {
		ResourceBundle bundle = ResourceBundle.getBundle("MessagesBundle"); // Locale
		StringBuilder result = null;
		int offset = 0;
		while (true) {
			String prefix = "{msg:";
			int index = content.indexOf(prefix, offset);
			if (index == -1)
				break;
			int endIndex = content.indexOf("}", index);
			if (endIndex == -1)
				throw new ServletException("Illegal substitute pattern at index " + index);
			String[] pattern = content.substring(index + prefix.length(), endIndex).split(",");
			if (pattern.length == 0)
				throw new ServletException("Empty substitute pattern at index " + index);
			String substitue = bundle.getString(pattern[0]);
			if (pattern.length > 1) {
				Object[] arguments = new Object[pattern.length - 1];
				for (int i = 0; i < arguments.length; i++) {
					arguments[i] = "+" + pattern[i + 1] + "+";
				}
				substitue = MessageFormat.format(substitue, arguments);
			}
			if (result == null)
				result = new StringBuilder(content.length() + substitue.length());
			result.append(content.substring(offset, index));
			result.append(substitue);
			offset = endIndex + 1;
		}
		if (result == null)
			return content;
		result.append(content.substring(offset));
		return result.toString();
	}

	@Override
	public void destroy() {
	}
}
