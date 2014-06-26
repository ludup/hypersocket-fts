package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class FileResourceCreatedEvent extends FileResourceEvent {

	private static final long serialVersionUID = 5853784191135656637L;

	public static String EVENT_RESOURCE_KEY = "fileResource.created";
	
	public FileResourceCreatedEvent(Object source, 
			Session session, FileResource resource) {
		super(source, EVENT_RESOURCE_KEY, true, session, resource);
	}

	public FileResourceCreatedEvent(Object source,
			Throwable e, Session session, FileResource resource) {
		super(source, EVENT_RESOURCE_KEY, e, session, resource);
	}

}
