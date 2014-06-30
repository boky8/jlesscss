package com.cekrlic.jlesscss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Load data from URI.
 * @author boky
 * @created 30.6.2014 12:34
 */
public class UriSource implements Source {
	private static final Logger log = LoggerFactory.getLogger(UriSource.class);

	private final URI uri;
	private final String content;
	private final String fileName;

	public UriSource(URI uri) throws IOException {
		this(uri.toURL());
	}
	public UriSource(URL url) throws IOException {
		assert url != null : "Please provide the URL!";
		fileName = url.toString();
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}

		URLConnection conn = url.openConnection();
		try(InputStream in = conn.getInputStream()) {
			ReadableByteChannel rbc = Channels.newChannel(in);
			byte[] buf = new byte[conn.getContentLength()];
			ByteBuffer dst = ByteBuffer.wrap(buf);
			int read;
			do {
				read = rbc.read(dst);
			} while(read != -1 && dst.remaining() > 0);

			// Assume UTF-8
			content = new String(buf, "UTF-8");
		}

	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public Importer getImporter() {
		return new UriFileImporter(uri);
	}
}
