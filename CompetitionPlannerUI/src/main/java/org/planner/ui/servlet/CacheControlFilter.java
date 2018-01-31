package org.planner.ui.servlet;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter(urlPatterns = "*")
public class CacheControlFilter implements javax.servlet.Filter {

	private Pattern pattern = Pattern.compile("\\.png|\\.gif|\\.js|\\.css");

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String uri = ((HttpServletRequest) request).getRequestURI();
		if (pattern.matcher(uri).find()) {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			// ein Jahr
			httpResponse.setHeader("Cache-Control", "public, max-age=31536000");
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}
}
