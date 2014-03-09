package com.hypersocket.fs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class DownloadCompleteEvent extends FileResourceEvent {

	private static final long serialVersionUID = -8393558342445743016L;

	public static final String ATTR_BYTES_OUT = "attr.bytesOut";
	public static final String ATTR_TRANSFER_TIME_MILLIS = "attr.transferTimeMillis";

	public static final String EVENT_RESOURCE_KEY = "fs.downloadComplete";

	public DownloadCompleteEvent(Object source, Session currentSession,
			FileResource resource, String childPath, long bytesOut,
			long timeMillis) {
		super(source, "fs.downloadComplete", true, currentSession, resource,
				childPath);
		addAttribute(ATTR_BYTES_OUT, String.valueOf(bytesOut));
		addAttribute(ATTR_TRANSFER_TIME_MILLIS, String.valueOf(timeMillis));
	}

	public DownloadCompleteEvent(Object source, Throwable t,
			Session currentSession, String name, String childPath) {
		super(source, "fs.downloadComplete", t, currentSession, name, childPath);
	}

}
