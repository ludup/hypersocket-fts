package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.session.Session;

public class VirtualFolderCreatedEvent extends VirtualFolderEvent {

	private static final long serialVersionUID = 4412767198870506735L;

	public static String EVENT_RESOURCE_KEY = "vfolder.created";
	
	public VirtualFolderCreatedEvent(Object source, Session session, String virtualPath) {
		super(source, EVENT_RESOURCE_KEY, true, session, virtualPath);
	}

	public VirtualFolderCreatedEvent(Object source, Throwable e, Session session, String virtualPath) {
		super(source, EVENT_RESOURCE_KEY, e, session, virtualPath);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
