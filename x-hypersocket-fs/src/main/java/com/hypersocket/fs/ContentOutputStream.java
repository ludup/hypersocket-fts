package com.hypersocket.fs;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

public class ContentOutputStream extends OutputStream {

	OutputStream out;
	long started;
	FileResource resource;
	FileObject file;
	String childPath;
	UploadEventProcessor eventProcessor;
	long bytesIn = 0;
	public ContentOutputStream(FileResource resource, String childPath,
			FileObject file, long position,
			long started,
			UploadEventProcessor eventProcessor) throws FileSystemException {
		out = file.getContent().getOutputStream();
		this.started = started;
		this.resource = resource;
		this.file = file;
		this.childPath = childPath;
		this.eventProcessor = eventProcessor;
	}

	@Override
	public void write(int b) throws IOException {
		try {
			out.write(b);
			bytesIn++;
		} catch (IOException e) {
			eventProcessor.uploadFailed(resource, childPath, file, bytesIn, e);
			throw e;
		}
	}
	
	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		try {
			out.write(buf, off, len);
			bytesIn+=len;
		} catch (IOException e) {
			eventProcessor.uploadFailed(resource, childPath, file, bytesIn, e);
			throw e;
		}
	}
	
	@Override
	public void close() throws IOException {
		
		try {
			out.close();
			// Event callback
			eventProcessor.uploadComplete(resource, childPath, file, bytesIn, started);
		} catch(IOException e) {
			eventProcessor.uploadFailed(resource, childPath, file, bytesIn, e);	
		}
	}

}
