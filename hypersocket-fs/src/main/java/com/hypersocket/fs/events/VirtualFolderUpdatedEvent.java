package com.hypersocket.fs.events;

import com.hypersocket.session.Session;

public class VirtualFolderUpdatedEvent extends VirtualFolderEvent {

	private static final long serialVersionUID = -769238193075832162L;

	public static String EVENT_RESOURCE_KEY = "vfolder.updated";
	
	public static String ATTR_TO_VIRTUAL_PATH = "attr.toVirtualPath";
	
	public VirtualFolderUpdatedEvent(Object source, Session session, String virtualPath, String toVirtualPath) {
		super(source, EVENT_RESOURCE_KEY, true, session, virtualPath);
		addAttribute(ATTR_TO_VIRTUAL_PATH, toVirtualPath);
	}

	public VirtualFolderUpdatedEvent(Object source, Throwable e, Session session, String virtualPath, String toVirtualPath) {
		super(source, EVENT_RESOURCE_KEY, e, session, virtualPath);
		addAttribute(ATTR_TO_VIRTUAL_PATH, toVirtualPath);
	}

}
