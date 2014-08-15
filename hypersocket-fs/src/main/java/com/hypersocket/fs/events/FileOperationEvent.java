package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceServiceImpl;
import com.hypersocket.resource.ResourceSessionEvent;
import com.hypersocket.session.Session;

public abstract class FileOperationEvent extends ResourceSessionEvent {

	private static final long serialVersionUID = 2710591456847473963L;

	public static final String ATTR_FILE_PATH = "attr.filePath";
	public static final String ATTR_PROTOCOL = "attr.protocol";
	
	FileResource sourceResource;
	String sourcePath;

	public FileOperationEvent(Object source, String resourceKey, boolean success,
			Session session, FileResource sourceResource, String sourcePath, String protocol) {
		super(source, resourceKey, success, session, sourceResource);
		addAttribute(ATTR_FILE_PATH, sourcePath);
		addAttribute(ATTR_PROTOCOL, protocol);
	}
	
	public FileOperationEvent(Object source, String resourceKey, Throwable e,
			Session session, String sourceResource, String sourcePath, String protocol) {
		super(source, resourceKey, sourceResource, e, session);
		addAttribute(ATTR_FILE_PATH, sourcePath);
		addAttribute(ATTR_PROTOCOL, protocol);
	}
	
	public String getResourceBundle() {
		return FileResourceServiceImpl.RESOURCE_BUNDLE;
	}

	public FileResource getSourceResource() {
		return sourceResource;
	}

	public String getSourcePath() {
		return sourcePath;
	}
	
	

}
