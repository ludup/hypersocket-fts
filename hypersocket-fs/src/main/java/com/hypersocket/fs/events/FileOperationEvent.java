package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceServiceImpl;
import com.hypersocket.resource.ResourceSessionEvent;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public abstract class FileOperationEvent extends ResourceSessionEvent {

	private static final long serialVersionUID = 2710591456847473963L;

	public static final String EVENT_RESOURCE_KEY = "fileOperation.event";
	
	public static final String ATTR_FILE_URL = "attr.fileUrl";
	public static final String ATTR_FILE_NAME = "attr.fileName";
	public static final String ATTR_VIRTUAL_PATH = "attr.virtualPath";
	public static final String ATTR_PROTOCOL = "attr.protocol";
	
	FileResource sourceResource;
	String sourcePath;	
	String filename;
	
	public FileOperationEvent(Object source, String resourceKey, boolean success,
			Session session, FileResource sourceResource, String sourcePath, String protocol) {
		super(source, resourceKey, success, session, sourceResource);
		this.sourcePath = sourcePath;
		this.sourceResource = sourceResource;
		addAttribute(ATTR_VIRTUAL_PATH, sourceResource.getVirtualPath() + sourcePath);
		addAttribute(ATTR_FILE_URL, sourceResource.getUrl() + sourcePath);
		addAttribute(ATTR_FILE_NAME, filename = FileUtils.lastPathElement(sourcePath));
		addAttribute(ATTR_PROTOCOL, protocol);
	}
	
	public FileOperationEvent(Object source, String resourceKey, Throwable e,
			Session session, FileResource sourceResource, String sourcePath, String protocol) {
		super(source, resourceKey, sourceResource.getName(), e, session);
		this.sourcePath = sourcePath;
		this.sourceResource = sourceResource;
		addAttribute(ATTR_VIRTUAL_PATH, sourceResource.getVirtualPath() + sourcePath);
		addAttribute(ATTR_FILE_URL, sourceResource.getUrl() + sourcePath);
		addAttribute(ATTR_FILE_NAME, filename = FileUtils.lastPathElement(sourcePath));
		addAttribute(ATTR_PROTOCOL, protocol);
	}
	
	public FileOperationEvent(Object source, String resourceKey, Throwable e,
			Session session, String virtualPath, String protocol) {
		super(source, resourceKey, e, session);
		addAttribute(ATTR_VIRTUAL_PATH, virtualPath);
		addAttribute(ATTR_FILE_NAME, filename = FileUtils.lastPathElement(virtualPath));
		addAttribute(ATTR_PROTOCOL, protocol);
	}
	
	public String getResourceBundle() {
		return FileResourceServiceImpl.RESOURCE_BUNDLE;
	}

	public FileResource getSourceResource() {
		return getSourceResource();
	}
	
	public String getChildPath() {
		return sourcePath;
	}

	public String getSourcePath() {
		return sourcePath;
	}
	
	public String getFilename() {
		return filename;
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}

	@Override
	public boolean isUsage() {
		return false;
	}
}
