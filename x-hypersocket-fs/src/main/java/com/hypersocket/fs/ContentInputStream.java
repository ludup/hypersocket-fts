package com.hypersocket.fs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

	public ContentInputStream(FileResource resource, String childPath,
			FileObject file, long start, long length,
			DownloadEventProcessor eventProcessor, long timeStarted) throws IOException {
		this.eventProcessor = eventProcessor;
		this.resource = resource;
		this.timeStarted = timeStarted;
		this.childPath = childPath;
		this.file = file;
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
		if(b == -1) {
			eventProcessor.downloadComplete(resource, childPath, file, totalBytesOut, System.currentTimeMillis() - timeStarted);
			remaining = 0;
		}
		return b;
	}
	
	public int read(byte[] buf, int off, int len) throws IOException {
		if(remaining == 0) {
			return -1;
		}
		int r = in.read(buf, off, len);
		if(r > -1) {
			remaining -= r;
			totalBytesOut+= r;
		}
		if(r == -1) {
			eventProcessor.downloadComplete(resource, childPath, file, totalBytesOut, System.currentTimeMillis() - timeStarted);
			remaining = 0;
		}
		return r;
	}

	public int available() {
		return (int) remaining;
	}
}