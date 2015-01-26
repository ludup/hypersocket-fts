package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class UploadStartedEvent extends FileOperationEvent {

	private static final long serialVersionUID = -6902976068663605328L;

	public static final String EVENT_RESOURCE_KEY = "fs.uploadStarted";
	
	public UploadStartedEvent(Object source, Session session, FileResource sourceResource,
			String sourcePath, String protocol) {
		super(source, "fs.uploadStarted", true, session, sourceResource, sourcePath, protocol);
	}

	public UploadStartedEvent(Object source, Throwable t,
			Session currentSession, String mountName, String childPath, String protocol) {
		super(source, "fs.uploadStarted", t, currentSession, mountName, childPath, protocol);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
