package com.hypersocket.fs;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.realm.Principal;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.VirtualFile;
import com.hypersocket.vfs.VirtualFileRepository;

public class ContentOutputStream extends OutputStream {

	OutputStream out;
	long started;
	FileResource resource;
	FileObject file;
	String childPath;
	UploadEventProcessor eventProcessor;
	long bytesIn = 0;
	String protocol;
	String virtualPath;
	VirtualFile parentFile;
	VirtualFileRepository virtualRepository;
	Principal principal;
	Session session;
	
	public ContentOutputStream(FileResource resource, 
			String childPath,
			String virtualPath,
			VirtualFileRepository virtualRepository,
			VirtualFile parentFile,
			FileObject file, 
			OutputStream out, 
			long position, 
			long started,
			UploadEventProcessor eventProcessor,
			String protocol,
			Principal principal,
			Session session)
			throws FileSystemException {
		this.out = out;
		this.started = started;
		this.resource = resource;
		this.file = file;
		this.childPath = childPath;
		this.eventProcessor = eventProcessor;
		this.protocol = protocol;
		this.virtualPath = virtualPath;
		this.parentFile = parentFile;
		this.virtualRepository = virtualRepository;
		this.principal = principal;
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
