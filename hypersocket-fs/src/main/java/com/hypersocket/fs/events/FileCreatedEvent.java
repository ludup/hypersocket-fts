package com.hypersocket.fs.events;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class FileCreatedEvent extends FileOperationEvent {

	private static final long serialVersionUID = 8848689885250702436L;

	public static String EVENT_RESOURCE_KEY = "file.created";
	
	public FileCreatedEvent(Object source, Session session,
			FileResource sourceResource, FileObject file, String sourcePath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, true, session, sourceResource, file, sourcePath, protocol);
	}

	public FileCreatedEvent(Object source, Throwable e, Session session,
			FileResource sourceResource, String sourcePath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, e, session, sourceResource, sourcePath, protocol);
	}

	public FileCreatedEvent(Object source, Throwable e, Session session, String virtualPath,
			String protocol) {
		super(source, EVENT_RESOURCE_KEY, e, session, virtualPath, protocol);
	}

}
