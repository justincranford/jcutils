package com.github.justincranford.jcutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

@SuppressWarnings({"rawtypes","unchecked","unused"})
public class HttpUtil {
	private static final Logger LOG = Logger.getLogger(HttpUtil.class.getName());

	public static final String HTTP_HEADER_SOAPACTION          = "soapaction";
	public static final String HTTP_CONTENT_TYPE               = "Content-Type";
	public static final String HTTP_CONTENT_LENGTH             = "Content-Length";
	public static final String HTTP_CONTENT_TYPE_TEXT_XML_UTF8 = "text/xml; charset=UTF-8";
	public static final String DEFAULT_CHARACTER_ENCODING      = "ISO-8859-1";	// HTTP/1.1 default is ISO-8859-1 (aka Latin-1 or US-ASCII), HTTP/2.0 default is UTF-8
	public static final String CONTENT_TYPE_CHARSET_SUBSTRING  = "charset=";	// EX => "Content-Type: text/xml; charset=utf-8"

	private HttpUtil() {
		// prevent class instantiation by making constructor private
	}

	public static byte[] readBytesFromInputStream(final InputStream is) throws IOException {
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(8192)) {
			int nRead;
			final byte[] data = new byte[8192];
			while (-1 != (nRead = is.read(data, 0, data.length))) {
				baos.write(data, 0, nRead);
			}
			baos.flush();
			return baos.toByteArray();
		// } catch (IOException ex) { // NOSONAR This block of commented-out lines of code should be removed.
			// throw new Exception("Error reading the request payload", ex);
		}
	}

	public static void logRequest(final Level level, final HttpServletRequest httpServletRequest) { // NOSONAR Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.
		final StringBuilder sb = new StringBuilder("Request URI: ").append(httpServletRequest.getRequestURI()).append("\n");
		sb.append("Request Method: ").append(httpServletRequest.getMethod()).append("\n");
		final Enumeration headerNameEnum = httpServletRequest.getHeaderNames();
		while (headerNameEnum.hasMoreElements()) {
			final String key = (String) headerNameEnum.nextElement();
			sb.append("Header: ").append(key).append("=").append(httpServletRequest.getHeader(key)).append("\n");
		}
		final TreeMap parameterMap = new TreeMap(httpServletRequest.getParameterMap());
		for (final Object key : parameterMap.keySet()) { // NOSONAR Iterate over the "entrySet" instead of the "keySet".
			sb.append("Parameter: ").append(key).append("=").append(parameterMap.get(key)).append("\n");
		}
		LOG.log(level, sb.append("\n").toString());
		final String contentType = httpServletRequest.getHeader(HTTP_CONTENT_TYPE);
		final String characterEncoding = parseCharacterEncoding(contentType);
		try (InputStream is = httpServletRequest.getInputStream()) {
			if (null == is) {
				LOG.log(Level.INFO, "Body not found, character encoding " + characterEncoding + ", content type " + contentType + "\n"); // NOSONAR
			} else {
				final byte[] sourceBytes = HttpUtil.readBytesFromInputStream(is);
				if ((null == contentType) || (!contentType.startsWith("text/xml") && !contentType.startsWith("application/xml"))) {
					LOG.log(Level.INFO, "Body[" + sourceBytes.length + "] character encoding " + characterEncoding + ", content type " + contentType); // NOSONAR
				} else {
					try { // NOSONAR Extract this nested try block into a separate method.
						if (null == characterEncoding) {
							LOG.log(Level.INFO, "Body[" + sourceBytes.length + "] character encoding " + characterEncoding + ", content type " + contentType + "\n" + new String(sourceBytes));
						} else {
							LOG.log(Level.INFO, "Body[" + sourceBytes.length + "] character encoding " + characterEncoding + ", content type " + contentType + "\n" + new String(sourceBytes, characterEncoding));
						}
					} catch (Exception e) {
						LOG.log(Level.INFO, "Body[" + sourceBytes.length + "] character encoding " + characterEncoding + ", content type " + contentType + ", decoding error", e);
					}
				}
			}
		} catch (Exception ex) {
			LOG.log(Level.WARNING, "Unable to read request body " + characterEncoding + ", content type " + contentType, ex);
		}
	}

	public static void logResponse(final Level level, final MyHttpServletResponseWrapper httpServletResponseWrapper) {
		final StringBuilder sb = new StringBuilder("Response Code: ");
		if (Integer.MIN_VALUE != httpServletResponseWrapper.status) {
			sb.append(httpServletResponseWrapper.status).append(" [").append(httpServletResponseWrapper.message).append("]").append("\n");
		} else {
			sb.append("Not set").append("\n");
		}
		for (final Entry<String, List<String>> entry : httpServletResponseWrapper.httpServletResponseHeaders.entrySet()) {
			sb.append("Header: ").append(entry.getKey()).append("=").append("{");
			for (final String value : entry.getValue()) {
				sb.append(value).append(",");
			}
			sb.append("}").append("\n");
		}
		LOG.log(level, sb.append("\n").toString());
	}

	public static String parseCharacterEncoding(final String contentType) {
		if (null == contentType) {
			LOG.log(Level.WARNING, "Missing HTTP response header " + HTTP_CONTENT_TYPE);
			return null;
		}

		final int charsetOffset = contentType.toLowerCase().lastIndexOf(CONTENT_TYPE_CHARSET_SUBSTRING);
		if (-1 == charsetOffset) {
			return DEFAULT_CHARACTER_ENCODING;
		}
		String contentEncodingAttribute = contentType.substring(charsetOffset + CONTENT_TYPE_CHARSET_SUBSTRING.length());

		final int semicolonOffset = contentEncodingAttribute.indexOf(';');
		if (-1 != semicolonOffset) {
			contentEncodingAttribute = contentEncodingAttribute.substring(0, semicolonOffset);
		}

		try {
			if (Charset.isSupported(contentEncodingAttribute)) {
				return contentEncodingAttribute; // valid charset in Content-Type header
			}
		} catch (IllegalCharsetNameException e) { // NOSONAR Either log or rethrow this exception.
			// fall through to log message
		}
		LOG.log(Level.WARNING, "Unsupported character encoding " + HTTP_CONTENT_TYPE + "=" + contentType);
		return null;
	}

	public static class MyHttpServletRequestWrapper extends HttpServletRequestWrapper {
		private final byte[] bytes;

		public MyHttpServletRequestWrapper(HttpServletRequest req) throws IOException {
			super(req);
			try (final InputStream is = req.getInputStream()) { // ASSUMPTION: CSR-ES compiles and runs on Java 7+
				if (null == is) {
					this.bytes = null;
				} else {
					this.bytes = HttpUtil.readBytesFromInputStream(is);
				}
			}
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.bytes);
			return new ServletInputStream() {
				@Override
				public int read() throws IOException {
					return byteArrayInputStream.read();
				}
				@Override
				public boolean isFinished() {
					return byteArrayInputStream.available() == 0;
				}
				@Override
				public boolean isReady() {
					return true;
				}
				@Override
				public void setReadListener(ReadListener listener) {
					throw new RuntimeException("Not implemented");	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
				}
			};
		}

		public byte[] getInputStreamBytes() {
			return (null == this.bytes) ? null : Arrays.copyOf(this.bytes, this.bytes.length);
		}

		public int getInputStreamLength() {
			return (null == this.bytes) ? Integer.MIN_VALUE : this.bytes.length;
		}
	}

	@SuppressWarnings({ "hiding" })
	public static class MyHttpServletResponseWrapper extends HttpServletResponseWrapper {
		protected int status = Integer.MIN_VALUE;
		protected String message = null;
		protected HashMap<String, List<String>> httpServletResponseHeaders = new HashMap<>();

		public MyHttpServletResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void addHeader(String name, String value) {
			super.addHeader(name, value);
			List<String> values = this.httpServletResponseHeaders.get(name);
			if (null == values) {
				values = new ArrayList<>();
			}
			this.httpServletResponseHeaders.put(name, values);
			values.add(value);
		}

		@Override
		public void setHeader(String name, String value) {
			super.setHeader(name, value);
			final ArrayList<String> values = new ArrayList<>();
			values.add(value);
			this.httpServletResponseHeaders.put(name, values);
		}

		@Override
		public void setStatus(int status) {
			super.setStatus(status);
			this.status = status;
		}

		@Deprecated
		@Override
		public void setStatus(int status, String message) {	// NOSONAR Add the missing @deprecated Javadoc tag.
			super.setStatus(status, message);
			this.status = status;
			this.message = message;
		}
	}
}