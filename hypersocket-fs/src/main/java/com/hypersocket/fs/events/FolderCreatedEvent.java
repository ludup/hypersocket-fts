package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class FolderCreatedEvent extends FileOperationEvent {

	private static final long serialVersionUID = 6019486301079966470L;

	public static String EVENT_RESOURCE_KEY = "folder.created";
	
	public FolderCreatedEvent(Object source, Session session,
			FileResource sourceResource, String sourcePath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, true, session, sourceResource, sourcePath, protocol);
	}

	public FolderCreatedEvent(Object source, Throwable e, Session session,
			FileResource sourceResource, String sourcePath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, e, session, sourceResource, sourcePath, protocol);
	}

	public FolderCreatedEvent(Object source, Throwable e, Session session, String virtualPath,
			String protocol) {
		super(source, EVENT_RESOURCE_KEY, e, session, virtualPath, protocol);
	}

}
