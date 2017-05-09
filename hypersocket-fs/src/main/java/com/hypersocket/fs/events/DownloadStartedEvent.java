package com.hypersocket.fs.events;

import java.io.InputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public class DownloadStartedEvent extends FileOperationEvent implements FileOutputTransformationEvent {

	private static final long serialVersionUID = 522157670150342226L;

	public static final String EVENT_RESOURCE_KEY = "fs.downloadStarted";
	
	InputStream in;
	String transformationFilename;
	String originalFilename;
	
	public DownloadStartedEvent(Object source, Session session, FileResource sourceResource,
			FileObject file, String sourcePath, InputStream in, String protocol) {
		super(source, EVENT_RESOURCE_KEY, true, session, sourceResource, file, sourcePath, protocol);
		this.in = in;
		this.transformationFilename = FileUtils.lastPathElement(sourcePath);
		this.originalFilename = FileUtils.lastPathElement(sourcePath);
	}

	public DownloadStartedEvent(Object source,
			Throwable t, Session currentSession,
			String childPath, String protocol) {
		super(source, EVENT_RESOURCE_KEY, t, currentSession, childPath, protocol);
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
		return transformationFilename;
	}

	@Override
	public void setTransformationFilename(String transformationFilename) {
		this.transformationFilename = transformationFilename;
	}

	@Override
	public String getOriginalFilename() {
		return originalFilename;
	}
}
