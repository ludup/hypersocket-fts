package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class DownloadStartedEvent extends FileResourceEvent {

	private static final long serialVersionUID = 522157670150342226L;

	public static final String EVENT_RESOURCE_KEY = "fs.downloadStarted";
	
	public DownloadStartedEvent(Object source, Session session, FileResource sourceResource,
			String sourcePath) {
		super(source, "fs.downloadStarted", true, session, sourceResource, sourcePath);
	}

	public DownloadStartedEvent(Object source,
			Throwable t, Session currentSession, String mountName,
			String childPath) {
		super(source, "fs.downloadStarted", t, currentSession, mountName, childPath);
	}

}
