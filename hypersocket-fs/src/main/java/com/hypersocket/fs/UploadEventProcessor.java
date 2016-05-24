package com.hypersocket.fs;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.events.UploadStartedEvent;

public interface UploadEventProcessor {

	void uploadCannotStart(String virtualPath, Throwable t, String protocol);

	UploadStartedEvent uploadStarted(FileResource resource, String virtualPath, FileObject file, String protocol);
	
	void uploadComplete(FileResource resource, String virtualPath,
			FileObject file, long bytesIn, long timeMillis, String protocol);

	void uploadFailed(FileResource resource, String virtualPath, FileObject file,
			long bytesIn, Throwable t, String protocol);

	

}
