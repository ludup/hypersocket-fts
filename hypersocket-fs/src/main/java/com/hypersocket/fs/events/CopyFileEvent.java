package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public class CopyFileEvent extends FileOperationEvent {

	private static final long serialVersionUID = 4385306086965414518L;

	public static final String ATTR_TO_FILE_URL = "attr.toFileUrl";
	public static final String ATTR_TO_VIRTUAL_PATH = "attr.toVirtualPath";
	public static final String ATTR_TO_FILENAME = "attr.toFileName";
	
	public static final String EVENT_RESOURCE_KEY = "fs.copyFile";
	
	public CopyFileEvent(Object source,
			Session currentSession, FileResource fromResource,
			String fromChildPath, FileResource toResource, String toChildPath, String protocol) {
		super(source, "fs.copyFile", true, currentSession, fromResource, fromChildPath, protocol);
		addAttribute(ATTR_TO_VIRTUAL_PATH, toResource.getVirtualPath() + toChildPath);
		addAttribute(ATTR_TO_FILE_URL, toResource.getUrl() + toChildPath);
		addAttribute(ATTR_TO_FILENAME, FileUtils.lastPathElement(toChildPath));
	}
	
	public CopyFileEvent(Object source, Exception e,
			Session currentSession, String fromVirtualPath,
			String toVirtualPath, String protocol) {
		super(source, "fs.copyFile", e, currentSession, fromVirtualPath, protocol);
		addAttribute(ATTR_TO_VIRTUAL_PATH, toVirtualPath);
		addAttribute(ATTR_TO_FILENAME, FileUtils.lastPathElement(toVirtualPath));
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}

	public boolean isUsage() {
		return true;
	}
}
