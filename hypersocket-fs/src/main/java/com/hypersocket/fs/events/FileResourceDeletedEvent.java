package com.hypersocket.fs.events;

import java.util.Map;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class FileResourceDeletedEvent extends FileResourceEvent {

	private static final long serialVersionUID = -3259047195122700173L;

	public static String EVENT_RESOURCE_KEY = "fileResource.deleted";
	
	public FileResourceDeletedEvent(Object source, 
			Session session, FileResource resource) {
		super(source, EVENT_RESOURCE_KEY, true, session, resource);
	}

	public FileResourceDeletedEvent(Object source,
			Throwable e, Session session, FileResource resource) {
		super(source, EVENT_RESOURCE_KEY, e, session, resource);
	}

}
