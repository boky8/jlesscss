package com.cekrlic.jlesscss;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Import files by callling {@link }HttpServletRequest#include}.
 * @author boky
 * @created 27.6.2014 11:39
 */
public class HtmlImporter implements Importer {
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private FileImportedCallback callback;

	public HtmlImporter(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}

	@Override
	public void setFileImportedCallback(FileImportedCallback callback) {
		this.callback = callback;
	}

	public Source importFile(String path) throws IOException {
		RequestDispatcher rd = request.getRequestDispatcher(path);
		ResourceResponseWrapper resp = new ResourceResponseWrapper(response);
		try {
			rd.include(request, resp);
		} catch (ServletException e) {
			throw new IOException(e);
		}

		if(callback!=null) { callback.fileImported(path); }
		return new MemorySource(path, resp.toString());

	}
}
