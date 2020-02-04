package com.hypersocket.fs.events;

import java.io.InputStream;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.DownloadEventProcessor;
import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class NoDownloadEventProcessor implements DownloadEventProcessor {

	private Session session;
	
	public NoDownloadEventProcessor(Session session) {
		this.session = session;
	}
	
	@Override
	public void downloadCannotStart(FileResource resource, String childPath, FileObject file, Throwable t,
			String protocol) {
	}

	@Override
	public DownloadStartedEvent downloadStarted(FileResource resource, String childPath, FileObject file,
			InputStream in, String protocol) {
		return new DownloadStartedEvent(this, session, resource, file, childPath, in, protocol);
	}

	@Override
	public void downloadComplete(FileResource resource, String childPath, FileObject file, long bytesOut,
			long timeMillis, String protocol, Session session) {
	}

	@Override
	public void downloadFailed(FileResource resource, String childPath, FileObject file, Throwable t, String protocol,
			Session session) {
	}

}
