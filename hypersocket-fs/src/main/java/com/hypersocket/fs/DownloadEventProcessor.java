package com.hypersocket.fs;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.session.Session;

public interface DownloadEventProcessor {

	void downloadCannotStart(FileResource resource, String childPath,
			FileObject file, Throwable t, String protocol);

	long downloadStarted(FileResource resource, String childPath,
			FileObject file, String protocol);

	void downloadComplete(FileResource resource, String childPath,
			FileObject file, long bytesOut, long timeMillis, String protocol, Session session);

	void downloadFailed(FileResource resource, String childPath,
			FileObject file, Throwable t, String protocol, Session session);
}
