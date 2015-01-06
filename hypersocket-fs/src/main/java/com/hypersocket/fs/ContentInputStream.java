package com.hypersocket.fs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.session.Session;

public class ContentInputStream extends InputStream {

	static Logger log = LoggerFactory.getLogger(ContentInputStream.class);

	long remaining;
	long totalBytesOut = 0;
	long timeStarted;
	DownloadEventProcessor eventProcessor;
	FileResource resource;
	String childPath;
	FileObject file;
	InputStream in;
	String protocol;
	Session session;

	public ContentInputStream(FileResource resource, String childPath,
			FileObject file, long start, long length,
			DownloadEventProcessor eventProcessor, long timeStarted,
			String protocol,
			Session session) throws IOException {
		this.eventProcessor = eventProcessor;
		this.resource = resource;
		this.timeStarted = timeStarted;
		this.childPath = childPath;
		this.file = file;
		this.protocol = protocol;
		this.session = session;
		this.in = new BufferedInputStream(file.getContent().getInputStream());
		this.remaining = in.available();
		if (start > 0) {
			if (log.isDebugEnabled()) {
				log.debug("content range will start at position " + start
						+ " with " + remaining + " available");
			}
			long actual = in.skip(start);
			if (actual != start) {
				throw new IOException(
						"Could not skip the required number of bytes");
			}
		}

		remaining = length;
	}

	@Override
	public int read() throws IOException {
		if (remaining == 0) {
			return -1;
		}
		int b = in.read();
		if (b > -1) {
			remaining--;
			totalBytesOut++;
		}
		if (b == -1) {
			internalClose();
		}
		return b;
	}

	public int read(byte[] buf, int off, int len) throws IOException {
		if (remaining == 0) {
			return -1;
		}
		int r = in.read(buf, off, len);
		if (r > -1) {
			remaining -= r;
			totalBytesOut += r;
		}
		if (r == -1) {
			internalClose();
		}
		return r;
	}

	public int available() {
		return (int) remaining;
	}
	
	public void close() {
		internalClose();
	}
	
	private synchronized void internalClose() {
		if(in!=null) {
			IOUtils.closeQuietly(in);
			eventProcessor.downloadComplete(resource, childPath, file,
					totalBytesOut, System.currentTimeMillis() - timeStarted,
					protocol, session);
			in = null;
			remaining = 0;
		}
	}
}