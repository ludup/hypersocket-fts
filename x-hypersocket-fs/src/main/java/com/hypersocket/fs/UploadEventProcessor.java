package com.hypersocket.fs;

import org.apache.commons.vfs2.FileObject;

public interface UploadEventProcessor {

	void uploadCannotStart(String mountName, String childPath, Throwable t);

	long uploadStarted(FileResource resource, String childPath, FileObject file);

	void uploadComplete(FileResource resource, String childPath,
			FileObject file, long bytesIn, long timeMillis);

	void uploadFailed(FileResource resource, String childPath, FileObject file,
			long bytesIn, Throwable t);

}
