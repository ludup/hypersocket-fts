package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.session.Session;
import com.hypersocket.session.events.SessionEvent;

public abstract class FileResourceEvent extends SessionEvent {

	private static final long serialVersionUID = 2710591456847473963L;

	public static final String ATTR_RESOURCE_NAME = "attr.resourceName";
	public static final String ATTR_FILE_PATH = "attr.filePath";
	public static final String ATTR_PROTOCOL = "attr.protocol";
	
	FileResource sourceResource;
	String sourcePath;

	public FileResourceEvent(Object source, String resourceKey, boolean success,
			Session session, FileResource sourceResource, String sourcePath, String protocol) {
		super(source, resourceKey, success, session);
		addAttribute(ATTR_RESOURCE_NAME, sourceResource.getName());
		addAttribute(ATTR_FILE_PATH, sourcePath);
		addAttribute(ATTR_PROTOCOL, protocol);
	}
	
	public FileResourceEvent(Object source, String resourceKey, Throwable e,
			Session session, String sourceResource, String sourcePath, String protocol) {
		super(source, resourceKey, e, session);
		addAttribute(ATTR_RESOURCE_NAME, sourceResource);
		addAttribute(ATTR_FILE_PATH, sourcePath);
		addAttribute(ATTR_PROTOCOL, protocol);
	}
	
	public String getResourceBundle() {
		return FileResourceService.RESOURCE_BUNDLE;
	}

	public FileResource getSourceResource() {
		return sourceResource;
	}

	public String getSourcePath() {
		return sourcePath;
	}
	
	

}
