/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cekrlic.jlesscss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Filter to process .css requests which are actually LESS files, doing on the
 * server side what less.js normally does on the client side.
 */
@WebFilter(
		filterName = "LessCSSFilter",
		urlPatterns = "*.less"
)
public class LessCSSFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(LessCSSFilter.class);
	private static final String[] DEFAULT_PATTERNS = new String[]{
			"EEE, dd MMM yyyy HH:mm:ss Z", // PATTERN_RFC1123
			"EEE, dd MMM yyyy HH:mm:ss zzz", // PATTERN_RFC1123
			"EEE, dd-MMM-yy HH:mm:ss zzz", // PATTERN_RFC1036
			"EEE MMM d HH:mm:ss yyyy", // PATTERN_ASCTIME
	};
	static final String COOKIE_NAME = "i";

	private Compiler compiler;

	public void init(FilterConfig filterConfig) {
		compiler = Compiler.getCompiler();
	}

	@Override
	public void destroy() {

	}

	/**
	 * Parse the LESS file, do some caching to make sure we don't compile needlessly.
	 * @param req The request
	 * @param res The response
	 * @param chain The filterChain
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/css; charset=UTF-8");

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		long ifModifiedSince = request.getDateHeader("If-Modified-Since");
		if (ifModifiedSince > 0) {
			request = new LastModifiedHttpServletRequestWrapper(request);
		}

		// Capture the resource file
		ResourceResponseWrapper wrapper = new ResourceResponseWrapper(response);
		chain.doFilter(request, wrapper);

		String ct = response.getContentType().toLowerCase().trim();

		if(!ct.startsWith("text/css") && !ct.startsWith("text/less")) {
			if(ct.startsWith("application/octet-stream")) {
				log.debug("File types not defined for *.less, returning application/octet-stream, but expected text/less. Please configure your app server.");
				res.setContentType("text/css; charset=UTF-8");
			} else {
				res.getOutputStream().write(wrapper.toByteArray());
			}
		}

		// Stop processing if not all good
		if (wrapper.getStatus() != HttpServletResponse.SC_OK) {
			return;
		}

		res.setCharacterEncoding("UTF-8");
		log.info("Processing request for: {}", request.getServletPath());

		long lastModified;
		try {
			lastModified = LessCSSFilter.parseDate(response.getHeader("Last-Modified")).getTime();
		} catch (ParseException e) {
			throw new ServletException(e);
		}

		if (ifModifiedSince > 0) {
			Cookie includes = null;
			for(Cookie c: request.getCookies()) {
				if(COOKIE_NAME.equalsIgnoreCase(c.getName())) {
					includes = c;
					break;
				}
			}

			if(includes != null) {
				// Check included files
				for(String path: decrypt(includes.getValue()).split(",")) {
					log.info("Checking last modified for: {}", path);
					LastModifiedResponseWrapper lmrw = new LastModifiedResponseWrapper(response);
					RequestDispatcher rd = request.getRequestDispatcher(path);
					ResourceResponseWrapper resp = new ResourceResponseWrapper(response);
					rd.include(request, resp);
					try {
						lastModified = parseDate(lmrw.getHeader("Last-Modified")).getTime();
					} catch (ParseException e) {
						throw new ServletException(e);
					}

				}
			}


			if(lastModified <= ifModifiedSince) {
				log.debug("Last-Modified request for {} > returning NOT_MODIFIED", request.getServletPath());
				response.setContentLength(0);
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			} else {
				log.debug("Something modified after {}, proceeding with full response.", ifModifiedSince);
			}
		}

		log.debug("Setting Last-Modified to: {} for {}.", new java.util.Date(lastModified), request.getServletPath());
		response.setDateHeader("Last-Modified", lastModified);

		List<String> files = new ArrayList<>(16);
		HtmlImporter importer = new HtmlImporter(request, response);
		importer.setFileImportedCallback(new Importer.ListFileImporterCallback(files));
		Source source = new MemorySource(request.getServletPath(), wrapper.toString());

		String result = compiler.compile(source, importer);

		// Write processed result to response
		final byte[] outbytes = result.getBytes(wrapper.getCharacterEncoding());

		Cookie c = new Cookie(COOKIE_NAME, encrypt(join(files, ",")));
		c.setHttpOnly(true);
		c.setPath(request.getRequestURI());
		c.setMaxAge(24 * 3600 * 365);
		response.addCookie(c);
		response.setContentLength(outbytes.length);
		response.getOutputStream().write(outbytes);
	}

	private String encrypt(String join) {
		try {
			return javax.xml.bind.DatatypeConverter.printBase64Binary(join.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return javax.xml.bind.DatatypeConverter.printBase64Binary(join.getBytes());
		}
	}

	private String decrypt(String value) {
		try {
			return new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(value), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(value));
		}
	}

	private String join(List<String> files, String separator) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(String s: files) {
			if(first) {
				first = false;
			} else {
				sb.append(separator);
			}
			sb.append(s);
		}
		return sb.toString();
	}


	static class LastModifiedResponseWrapper extends ResourceResponseWrapper {
		LastModifiedResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void setDateHeader(String name, long date) {
			if ("Last-Modified".equalsIgnoreCase(name)) {
				String lm = super.getHeader("Last-Modified");
				long lastModified;
				try {
					lastModified = lm != null ? parseDate(lm).getTime() : System.currentTimeMillis();
				} catch (ParseException e) {
					throw new RuntimeException("Cannot parse '" + lm + "' at position " + e.getErrorOffset(), e);
				}
				if (date > lastModified) {
					super.setDateHeader(name, date);
				}

			} else {
				super.setDateHeader(name, date);
			}
		}

		@Override
		public void setHeader(String name, String value) {
			if ("Last-Modified".equalsIgnoreCase(name)) {
				try {
					setDateHeader(name, parseDate(value).getTime());
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
			} else {
				super.setHeader(name, value);
			}
		}
	}

	/**
	 * Parse any of the wildly-used date formats by the HTTP servers
	 * @param d The date
	 * @return The parsed date
	 * @throws ParseException Thrown if the date cannot be parsed in any way.
	 */
	public static Date parseDate(String d) throws ParseException {
		ParseException first = null;
		for (String p : DEFAULT_PATTERNS) {
			try {
				return new SimpleDateFormat(p, Locale.US).parse(d);
			} catch (ParseException pe) {
				if (first == null) {
					first = pe;
				}
			}
		}

		throw first;
	}

	/**
	 * Get the last modified date from the included files.
	 */
	private static class LastModifiedHttpServletRequestWrapper extends HttpServletRequestWrapper {
		public LastModifiedHttpServletRequestWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public long getDateHeader(String name) {
			if ("If-Modified-Since".equalsIgnoreCase(name)) {
				return -1;
			} else {
				return super.getDateHeader(name);
			}
		}

		@Override
		public String getHeader(String name) {
			if ("If-Modified-Since".equalsIgnoreCase(name)) {
				return null;
			} else {
				return super.getHeader(name);
			}
		}
	}
}