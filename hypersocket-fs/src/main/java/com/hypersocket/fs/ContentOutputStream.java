package com.hypersocket.fs;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.session.Session;
import com.hypersocket.util.FileUtils;

public class ContentOutputStream extends OutputStream {

	OutputStream out;
	long started;
	FileResource resource;
	FileObject file;
	String childPath;
	UploadEventProcessor eventProcessor;
	long bytesIn = 0;
	String protocol;
	
	public ContentOutputStream(FileResource resource, String childPath,
			FileObject file, long position, long started,
			UploadEventProcessor eventProcessor, String protocol)
			throws FileSystemException {
		out = file.getContent().getOutputStream();
		this.started = started;
		this.resource = resource;
		this.file = file;
		this.childPath = childPath;
		this.eventProcessor = eventProcessor;
		this.protocol = protocol;
	}

	@Override
	public synchronized void write(int b) throws IOException {
		try {
			out.write(b);
			bytesIn++;
		} catch (IOException e) {
			FileUtils.closeQuietly(out);
			eventProcessor.uploadFailed(resource, childPath, file, bytesIn, e,
					protocol);
			out = null;
			throw e;
		}
	}

	@Override
	public synchronized void write(byte[] buf, int off, int len)
			throws IOException {
		try {
			out.write(buf, off, len);
			bytesIn += len;
		} catch (IOException e) {
			FileUtils.closeQuietly(out);
			eventProcessor.uploadFailed(resource, childPath, file, bytesIn, e,
					protocol);
			out = null;
			throw e;
		}
	}

	@Override
	public synchronized void close() throws IOException {

		if (out != null) {
			FileUtils.closeQuietly(out);
			out = null;
			// Event callback
			eventProcessor.uploadComplete(resource, childPath, file,
					bytesIn, started, protocol);
		}
	}

}
