package com.hypersocket.fs;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.VirtualFile;
import com.hypersocket.vfs.VirtualFileRepository;
import com.hypersocket.vfs.VirtualFileServiceImpl;

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
			String protocol)
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
			reconcileFile();
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
			reconcileFile();
			eventProcessor.uploadComplete(resource, childPath, file,
					bytesIn, started, protocol);
			
		}
	}

	private void reconcileFile() throws FileSystemException {
		VirtualFile existingFile = virtualRepository.getVirtualFile(virtualPath);
		
		String displayName = FileUtils.lastPathElement(virtualPath);
		if(existingFile!=null) {
			displayName = String.format("%s (%s)", displayName, parentFile.getMount().getName());
		}
		virtualRepository.reconcileFile(displayName, file, resource, parentFile);
	}
}
