package com.hypersocket.ftp.interfaces.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.ftp.interfaces.FTPInterfaceResource;
import com.hypersocket.session.Session;

public class FTPInterfaceResourceDeletedEvent extends
		FTPInterfaceResourceEvent {

	private static final long serialVersionUID = -8344184695531380409L;
	
	public static final String EVENT_RESOURCE_KEY = "ftpInterface.deleted";

	public FTPInterfaceResourceDeletedEvent(Object source,
			Session session, FTPInterfaceResource resource) {
		super(source, EVENT_RESOURCE_KEY, session, resource);
	}

	public FTPInterfaceResourceDeletedEvent(Object source,
			FTPInterfaceResource resource, Throwable e, Session session) {
		super(source, EVENT_RESOURCE_KEY, resource, e, session);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
