/*
 * Copyright 2012, Emanuel Rabina (http://www.ultraq.net.nz/)
 * 
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

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;

/**
 * Response wrapper to capture resources.
 *
 * @author Emanuel Rabina
 */
public class ResourceResponseWrapper extends HttpServletResponseWrapper {
	private static final Logger log = LoggerFactory.getLogger(ResourceResponseWrapper.class);

	final Object guard = new Object();
	final ByteArrayOutputStream bos = new ByteArrayOutputStream();

	PrintWriter pw = null;
	ServletOutputStream os = new PassThroughServletOutputStream(bos);

	static class PassThroughServletOutputStream extends ServletOutputStream {
		final OutputStream bos;

		PassThroughServletOutputStream(final OutputStream os) {
			this.bos = os;
		}

		public void write(int b) throws IOException {
			bos.write(b);
		}

		public void write(byte[] b) throws IOException {
			bos.write(b);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			bos.write(b, off, len);
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			try {
				writeListener.onWritePossible();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}


	/**
	 * Constructor, set the original response.
	 *
	 * @param response
	 */
	public ResourceResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public void setContentType(String type) {
		super.setContentType(type);
		log.debug("Setting content type to: {}", type);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return os;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		synchronized (guard) {
			if (pw == null) {
				OutputStreamWriter osw = new OutputStreamWriter(os, getCharacterEncoding());
				pw = new PrintWriter(osw, true);
			}
		}
		return pw;
	}

	public byte[] toByteArray() {
		try {
			synchronized (guard) {
				if (pw != null) {
					pw.flush();
				}
				os.flush();
			}
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Could not flush stream: " + e.toString(), e);
		}
	}

	public String toString() {
		try {
			return new String(toByteArray(), getCharacterEncoding());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Encoding not supported: " + e.toString(), e);
		}
	}

}