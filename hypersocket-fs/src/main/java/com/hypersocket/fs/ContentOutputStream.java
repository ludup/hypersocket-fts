package com.hypersocket.fs;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public class ContentOutputStream extends OutputStream {

	private OutputStream out;
	private long started;
	private FileResource resource;
	private FileObject file;
	private String childPath;
	private UploadEventProcessor eventProcessor;
	private long bytesIn = 0;
	private String protocol;
	private Session session;
	
	public ContentOutputStream(FileResource resource, 
			String childPath,
			FileObject file,
			OutputStream out,
			long position,
			long started, 
			UploadEventProcessor eventProcessor, 
			String protocol, 
			Session session)
			throws FileSystemException {
		this.out = out;
		this.started = started;
		this.resource = resource;
		this.file = file;
		this.childPath = childPath;
		this.eventProcessor = eventProcessor;
		this.protocol = protocol;
		this.session = session;
	}

	@Override
	public synchronized void write(int b) throws IOException {
		try {
			out.write(b);
			bytesIn++;
			session.touch();
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
			session.touch();
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
			eventProcessor.uploadComplete(resource, childPath, file,
					bytesIn, started, protocol);
			
		}
	}

}
