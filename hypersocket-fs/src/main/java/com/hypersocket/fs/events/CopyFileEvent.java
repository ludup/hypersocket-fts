package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class CopyFileEvent extends FileOperationEvent {

	private static final long serialVersionUID = 4385306086965414518L;

	public static final String ATTR_TO_RESOURCE_NAME = "attr.toResourceName";
	public static final String ATTR_TO_FILE_PATH = "attr.toFilePath";
	
	public static final String EVENT_RESOURCE_KEY = "fs.copyFile";
	
	public CopyFileEvent(Object source,
			Session currentSession, FileResource fromResource,
			String fromChildPath, FileResource toResource, String toChildPath, String protocol) {
		super(source, "fs.copyFile", true, currentSession, fromResource, fromChildPath, protocol);
		addAttribute(ATTR_TO_RESOURCE_NAME,
				toResource.getName());
		addAttribute(ATTR_TO_FILE_PATH, toChildPath);
	}
	
	public CopyFileEvent(Object source, Exception e,
			Session currentSession, String name, String fromChildPath,
			String toResource, String toChildPath, String protocol) {
		super(source, "fs.copyFile", e, currentSession, name, fromChildPath, protocol);
		addAttribute(ATTR_TO_RESOURCE_NAME,
				toResource);
		addAttribute(ATTR_TO_FILE_PATH, toChildPath);
	}


}
