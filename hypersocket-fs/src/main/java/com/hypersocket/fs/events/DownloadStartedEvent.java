package com.hypersocket.fs.events;

import java.io.InputStream;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public class DownloadStartedEvent extends FileOperationEvent implements FileTransformationEvent {

	private static final long serialVersionUID = 522157670150342226L;

	public static final String EVENT_RESOURCE_KEY = "fs.downloadStarted";
	
	InputStream in;
	String downloadFilename;
	public DownloadStartedEvent(Object source, Session session, FileResource sourceResource,
			String sourcePath, InputStream in, String protocol) {
		super(source, "fs.downloadStarted", true, session, sourceResource, sourcePath, protocol);
		this.in = in;
		this.downloadFilename = FileUtils.lastPathElement(sourcePath);
	}

	public DownloadStartedEvent(Object source,
			Throwable t, Session currentSession, String mountName,
			String childPath, String protocol) {
		super(source, "fs.downloadStarted", t, currentSession, mountName, childPath, protocol);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
	
	public boolean isUsage() {
		return false;
	}

	@Override
	public InputStream getInputStream() {
		return in;
	}

	@Override
	public void setInputStream(InputStream in) {
		this.in = in;
	}
	
	@Override
	public String getTransformationFilename() {
		return downloadFilename;
	}

	@Override
	public void setTransformationFilename(String downloadFilename) {
		this.downloadFilename = downloadFilename;
	}
}
