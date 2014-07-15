package com.hypersocket.fs.events;

import java.util.Map;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class FileResourceUpdatedEvent extends FileResourceEvent {

	private static final long serialVersionUID = -8887085414639578981L;

	public static String EVENT_RESOURCE_KEY = "fileResource.updated";
	
	public FileResourceUpdatedEvent(Object source, 
			Session session, FileResource resource) {
		super(source, EVENT_RESOURCE_KEY, true, session, resource);
	}

	public FileResourceUpdatedEvent(Object source,
			Throwable e, Session session, FileResource resource) {
		super(source, EVENT_RESOURCE_KEY, e, session, resource);
	}

}
