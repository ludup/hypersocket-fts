package com.hypersocket.fs;

import org.apache.commons.vfs2.FileObject;

public interface DownloadEventProcessor {

	void downloadCannotStart(FileResource resource, String childPath, FileObject file, Throwable t);
	
	long downloadStarted(FileResource resource, String childPath, FileObject file);
	
	void downloadComplete(FileResource resource, String childPath, FileObject file, long bytesOut, long timeMillis);
	
	void downloadFailed(FileResource resource, String childPath, FileObject file, Throwable t);
}
