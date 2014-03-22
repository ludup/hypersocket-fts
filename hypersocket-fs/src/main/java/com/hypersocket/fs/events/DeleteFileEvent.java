package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class DeleteFileEvent extends FileResourceEvent {

	private static final long serialVersionUID = -2933354011349261002L;

	public static final String EVENT_RESOURCE_KEY = "fs.deleteFile";
	
	public DeleteFileEvent(Object source, boolean success,
			Session currentSession, FileResource resource, String childPath, String protocol) {
		super(source, "fs.deleteFile", success, currentSession, resource, childPath, protocol);
	}

	public DeleteFileEvent(Object source,
			Throwable ex, Session currentSession, String name,
			String childPath, String protocol) {
		super(source, "fs.deleteFile", ex, currentSession, name, childPath, protocol);
	}

}
