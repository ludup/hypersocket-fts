package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class CreateFolderEvent extends FileOperationEvent {

	private static final long serialVersionUID = 7356710650649275059L;

	public static final String EVENT_RESOURCE_KEY = "fs.createFolder";
	
	public CreateFolderEvent(Object source, boolean success,
			Session currentSession, FileResource resource, FileObject file, String childPath, String protocol) {
		super(source, "fs.createFolder", success, currentSession, resource, file, childPath, protocol);
	}

	public CreateFolderEvent(Object source, Throwable t,
			Session currentSession, String childPath, String protocol) {
		super(source, "fs.createFolder", t, currentSession, childPath, protocol);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
	
	public boolean isUsage() {
		return true;
	}
}
