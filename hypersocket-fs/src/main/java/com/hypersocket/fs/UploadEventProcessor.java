package com.hypersocket.fs;

import org.apache.commons.vfs2.FileObject;

public interface UploadEventProcessor {

	void uploadCannotStart(String mountName, String childPath, Throwable t,
			String protocol);

	long uploadStarted(FileResource resource, String childPath,
			FileObject file, String protocol);

	void uploadComplete(FileResource resource, String childPath,
			FileObject file, long bytesIn, long timeMillis, String protocol);

	void uploadFailed(FileResource resource, String childPath, FileObject file,
			long bytesIn, Throwable t, String protocol);

}
