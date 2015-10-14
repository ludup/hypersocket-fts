package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public class RenameEvent extends FileOperationEvent {

	private static final long serialVersionUID = -5630859010723876007L;

	public static final String ATTR_TO_RESOURCE_NAME = "attr.toResourceName";
	public static final String ATTR_TO_FILE_PATH = "attr.toFilePath";
	public static final String ATTR_TO_FILENAME = "attr.toFileName";
	public static final String ATTR_TO_RESOURCE_PATH = "attr.toResourcePath";
	
	public static final String EVENT_RESOURCE_KEY = "fs.renameFile";
	
	String toChildPath;
	String toFilename;
	String toFilePath;
	
	public RenameEvent(Object source,
			Session currentSession, FileResource fromResource,
			String fromChildPath, FileResource toResource, String toChildPath, String protocol) {
		super(source, "fs.renameFile", true, currentSession, fromResource, fromChildPath, protocol);
		addAttribute(ATTR_TO_RESOURCE_NAME,toResource.getName());
		addAttribute(ATTR_TO_FILE_PATH, this.toFilePath = "/" + toResource.getName() + FileUtils.checkStartsWithSlash(sourcePath));
		addAttribute(ATTR_TO_RESOURCE_PATH, this.toChildPath = FileUtils.checkStartsWithNoSlash(toChildPath));
		addAttribute(ATTR_TO_FILENAME, this.toFilename = FileUtils.lastPathElement(sourcePath));
	}

	public RenameEvent(Object source, Exception e,
			Session currentSession, String name, String fromChildPath,
			String toResource, String toChildPath, String protocol) {
		super(source, "fs.renameFile", e, currentSession, name, fromChildPath, protocol);
		addAttribute(ATTR_TO_RESOURCE_NAME,
				toResource);
		addAttribute(ATTR_TO_FILE_PATH, toChildPath);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
	
	public String getToChildPath() {
		return toChildPath;
	}

	public String getToFilename() {
		return toFilename;
	}

	public String getToFilePath() {
		return toFilePath;
	}
	
	public boolean isUsage() {
		return true;
	}
}
