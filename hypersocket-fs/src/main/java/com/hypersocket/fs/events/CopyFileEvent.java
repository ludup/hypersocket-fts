package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public class CopyFileEvent extends FileOperationEvent {

	private static final long serialVersionUID = 4385306086965414518L;

	public static final String ATTR_TO_RESOURCE_NAME = "attr.toResourceName";
	public static final String ATTR_TO_FILE_PATH = "attr.toFilePath";
	public static final String ATTR_TO_FILENAME = "attr.toFileName";
	public static final String ATTR_TO_RESOURCE_PATH = "attr.toResourcePath";
	
	public static final String EVENT_RESOURCE_KEY = "fs.copyFile";
	
	String toChildPath;
	String toFilename;
	String toFilePath;
	
	public CopyFileEvent(Object source,
			Session currentSession, FileResource fromResource,
			String fromChildPath, FileResource toResource, String toChildPath, String protocol) {
		super(source, "fs.copyFile", true, currentSession, fromResource, fromChildPath, protocol);
		addAttribute(ATTR_TO_RESOURCE_NAME, toResource.getName());
		addAttribute(ATTR_TO_FILE_PATH, this.toFilePath = "/" + toResource.getName() + FileUtils.checkStartsWithSlash(sourcePath));
		addAttribute(ATTR_TO_RESOURCE_PATH, this.toChildPath = FileUtils.checkStartsWithNoSlash(toChildPath));
		addAttribute(ATTR_TO_FILENAME, this.toFilename = FileUtils.lastPathElement(sourcePath));
	}
	
	public CopyFileEvent(Object source, Exception e,
			Session currentSession, String name, String fromChildPath,
			String toResource, String toChildPath, String protocol) {
		super(source, "fs.copyFile", e, currentSession, name, fromChildPath, protocol);
		addAttribute(ATTR_TO_RESOURCE_NAME,
				toResource);
		addAttribute(ATTR_TO_FILE_PATH, toFilePath = "/" + name + FileUtils.checkStartsWithSlash(sourcePath));
		addAttribute(ATTR_TO_RESOURCE_PATH, toChildPath = FileUtils.checkStartsWithNoSlash(toChildPath));
		addAttribute(ATTR_TO_FILENAME, toFilename = FileUtils.lastPathElement(sourcePath));
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

}
