package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class UploadStartedEvent extends FileResourceEvent {

	private static final long serialVersionUID = -6902976068663605328L;

	public static final String EVENT_RESOURCE_KEY = "fs.uploadStarted";
	
	public UploadStartedEvent(Object source, Session session, FileResource sourceResource,
			String sourcePath) {
		super(source, "fs.uploadStarted", true, session, sourceResource, sourcePath);
	}

	public UploadStartedEvent(Object source, Throwable t,
			Session currentSession, String mountName, String childPath) {
		super(source, "fs.uploadStarted", t, currentSession, mountName, childPath);
	}

}
