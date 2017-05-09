package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class CreateFileEvent extends FileOperationEvent {

	private static final long serialVersionUID = 7356710650649275059L;

	public static final String EVENT_RESOURCE_KEY = "fs.createFile";
	
	public CreateFileEvent(Object source, boolean success,
			Session currentSession, FileResource resource, FileObject file, String childPath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, success, currentSession, resource, file, childPath, protocol);
	}

	public CreateFileEvent(Object source, Throwable t,
			Session currentSession, String childPath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, t, currentSession, childPath, protocol);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
	
	public boolean isUsage() {
		return true;
	}
}
