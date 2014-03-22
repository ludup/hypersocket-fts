package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class CreateFolderEvent extends FileResourceEvent {

	private static final long serialVersionUID = 7356710650649275059L;

	public static final String EVENT_RESOURCE_KEY = "fs.createFolder";
	
	public CreateFolderEvent(Object source, boolean success,
			Session currentSession, FileResource resource, String childPath, String protocol) {
		super(source, "fs.createFolder", success, currentSession, resource, childPath, protocol);
	}

	public CreateFolderEvent(Object source, Throwable t,
			Session currentSession, String mountName, String childPath, String protocol) {
		super(source, "fs.createFolder", t, currentSession, mountName, childPath, protocol);
	}

}
