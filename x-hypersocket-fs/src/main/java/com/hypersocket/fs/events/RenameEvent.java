package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class RenameEvent extends FileResourceEvent {

	private static final long serialVersionUID = -5630859010723876007L;

	public static final String ATTR_TO_RESOURCE_NAME = "attr.toResourceName";
	public static final String ATTR_TO_FILE_PATH = "attr.toFilePath";
	
	public static final String EVENT_RESOURCE_KEY = "fs.renameFile";
	
	public RenameEvent(Object source,
			Session currentSession, FileResource fromResource,
			String fromChildPath, FileResource toResource, String toChildPath) {
		super(source, "fs.renameFile", true, currentSession, fromResource, fromChildPath);
		addAttribute(ATTR_TO_RESOURCE_NAME,
				toResource.getName());
		addAttribute(ATTR_TO_FILE_PATH, toChildPath);
	}

	public RenameEvent(Object source, Exception e,
			Session currentSession, String name, String fromChildPath,
			String toResource, String toChildPath) {
		super(source, "fs.renameFile", e, currentSession, name, fromChildPath);
		addAttribute(ATTR_TO_RESOURCE_NAME,
				toResource);
		addAttribute(ATTR_TO_FILE_PATH, toChildPath);
	}

}
