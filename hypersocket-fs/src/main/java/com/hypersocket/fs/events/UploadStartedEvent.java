package com.hypersocket.fs.events;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;

public class UploadStartedEvent extends FileOperationEvent implements FileInputTransformationEvent{

	private static final long serialVersionUID = -6902976068663605328L;

	public static final String EVENT_RESOURCE_KEY = "fs.uploadStarted";
	
	String transformationFilename;
	String originalPath;
	String originalFilename;
	FileObject outputFile;
	OutputStream out;
	
	public UploadStartedEvent(Object source, Session session, FileResource sourceResource,
			String sourcePath, FileObject file, String protocol) {
		super(source, "fs.uploadStarted", true, session, sourceResource, sourcePath, protocol);
		this.outputFile = file;
		this.out = null;
		this.transformationFilename = FileUtils.lastPathElement(sourcePath);
		this.originalFilename = this.transformationFilename;
		if(FileUtils.hasParents(sourcePath)) {
			this.originalPath = FileUtils.stripLastPathElement(sourcePath);
		} else {
			this.originalPath = "";
		}
	}

	public UploadStartedEvent(Object source, Throwable t,
			Session currentSession, String childPath, String protocol) {
		super(source, "fs.uploadStarted", t, currentSession, childPath, protocol);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
	
	public boolean isUsage() {
		return false;
	}
	
	@Override
	public String getTransformationFilename() {
		return transformationFilename;
	}

	@Override
	public void setTransformationFilename(String transformationFilename) throws IOException {
		this.transformationFilename = transformationFilename;
		if(out!=null) {
			throw new IOException("You must set transformation filename BEFORE calling getOutputStream for the transformation source.");
		}
		outputFile = outputFile.getParent().resolveFile(transformationFilename);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if(out==null) {
			return out = outputFile.getContent().getOutputStream();
		}
		return out;
	}

	@Override
	public void setOutputStream(OutputStream out) throws IOException {
		if(out==null) {
			throw new IOException("You cannot set the OutputStream without calling getOutputStream first as you should be using that as a source to transform the file");
		}
		this.out = out;
	}

	public FileObject getOutputFile() {
		return outputFile;
	}

	@Override
	public String getOriginalFilename() {
		return originalFilename;
	}

	public String getTransformationPath() {
		if(StringUtils.isNotBlank(originalPath)) {
			return originalPath + "/" + transformationFilename;
		} else {
			return transformationFilename; 
		}
	}
}
