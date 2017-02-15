package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public class RenameEvent extends FileOperationEvent {

	private static final long serialVersionUID = -5630859010723876007L;

	public static final String ATTR_TO_RESOURCE_NAME = "attr.toResourceName";
	public static final String ATTR_TO_VIRTUAL_PATH = "attr.toVirtualPath";
	public static final String ATTR_TO_FILENAME = "attr.toFileName";
	
	public static final String EVENT_RESOURCE_KEY = "fs.renameFile";
	
	public RenameEvent(Object source,
			Session currentSession, FileResource fromResource,
			String fromChildPath, FileResource toResource, String toChildPath, String protocol) {
		super(source, "fs.renameFile", true, currentSession, fromResource, fromChildPath, protocol);
		addAttribute(ATTR_TO_RESOURCE_NAME,toResource.getName());
		addAttribute(ATTR_TO_VIRTUAL_PATH,  FileUtils.checkEndsWithSlash(toResource.getVirtualPath())
				+ FileUtils.checkStartsWithNoSlash(toChildPath));
		addAttribute(ATTR_TO_FILENAME, FileUtils.lastPathElement(toChildPath));
	}

	public RenameEvent(Object source, Exception e,
			Session currentSession, 
			String fromChildPath,
			String toChildPath, 
			String protocol) {
		super(source, "fs.renameFile", e, currentSession, fromChildPath, protocol);
		addAttribute(ATTR_TO_VIRTUAL_PATH, toChildPath);
		addAttribute(ATTR_TO_FILENAME, FileUtils.lastPathElement(toChildPath));
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
	
	public boolean isUsage() {
		return true;
	}
}
