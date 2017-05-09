package com.hypersocket.fs.events;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class FolderUpdatedEvent extends FileOperationEvent {

	private static final long serialVersionUID = -7377036049478747109L;

	public static String EVENT_RESOURCE_KEY = "folder.updated";
	
	public FolderUpdatedEvent(Object source, Session session,
			FileResource sourceResource, FileObject file, String sourcePath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, true, session, sourceResource, file, sourcePath, protocol);
	}

	public FolderUpdatedEvent(Object source, Throwable e, Session session,
			FileResource sourceResource, String sourcePath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, e, session, sourceResource, sourcePath, protocol);
	}

	public FolderUpdatedEvent(Object source, Throwable e, Session session, String virtualPath,
			String protocol) {
		super(source, EVENT_RESOURCE_KEY, e, session, virtualPath, protocol);
	}

}
