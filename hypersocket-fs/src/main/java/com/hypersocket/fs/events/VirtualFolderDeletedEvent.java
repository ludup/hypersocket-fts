package com.hypersocket.fs.events;

import com.hypersocket.session.Session;

public class VirtualFolderDeletedEvent extends VirtualFolderEvent {

	private static final long serialVersionUID = 8930776949278843729L;
	
	public static String EVENT_RESOURCE_KEY = "vfolder.deleted";
	
	public VirtualFolderDeletedEvent(Object source, Session session, String virtualPath) {
		super(source, EVENT_RESOURCE_KEY, true, session, virtualPath);
	}

	public VirtualFolderDeletedEvent(Object source, Throwable e, Session session, String virtualPath) {
		super(source, EVENT_RESOURCE_KEY, e, session, virtualPath);
	}

}
