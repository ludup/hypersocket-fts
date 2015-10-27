package com.hypersocket.fs;

import java.io.InputStream;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.events.DownloadStartedEvent;
import com.hypersocket.session.Session;

public interface DownloadEventProcessor {

	void downloadCannotStart(FileResource resource, String childPath,
			FileObject file, Throwable t, String protocol);

	DownloadStartedEvent downloadStarted(FileResource resource, String childPath,
			FileObject file, InputStream in, String protocol);

	void downloadComplete(FileResource resource, String childPath,
			FileObject file, long bytesOut, long timeMillis, String protocol, Session session);

	void downloadFailed(FileResource resource, String childPath,
			FileObject file, Throwable t, String protocol, Session session);
}
