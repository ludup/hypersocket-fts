package com.hypersocket.fs;

import org.apache.commons.vfs2.provider.FileProvider;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class FileResourceScheme {

	String scheme;
	boolean isRemote;
	boolean supportsCredentials;
	boolean showPort;
	boolean showPath;
	boolean readOnly;
	boolean showHidden;
	boolean showFolders;
	FileService fileService;
	Class<? extends FileProvider> provider;
	
	public FileResourceScheme() {
	}
	
	public FileResourceScheme(String scheme, 
			boolean isRemote, 
			boolean supportsCredentials, 
			boolean showPort,
			boolean showPath,
			boolean readOnly,
			boolean showHidden,
			boolean showFolders) {
		this(scheme, isRemote, supportsCredentials, showPort, showPath, readOnly, showHidden, showFolders, null, null);
	}
	
	public FileResourceScheme(String scheme, 
			boolean isRemote, 
			boolean supportsCredentials, 
			boolean showPort, 
			boolean showPath,
			boolean readOnly,
			boolean showHidden,
			boolean showFolders,
			Class<? extends FileProvider> provider) {
		this(scheme, isRemote, supportsCredentials, showPort, showPath, readOnly, showHidden, showFolders, provider, null);
	}
	
	public FileResourceScheme(String scheme, 
			boolean isRemote, 
			boolean supportsCredentials, 
			boolean showPort, 
			boolean showPath,
			boolean readOnly,
			boolean showHidden,
			boolean showFolders,
			FileService fileService) {
		this(scheme, isRemote, supportsCredentials, showPort, showPath, readOnly, showHidden, showFolders, null, fileService);
	}
	
	public FileResourceScheme(String scheme, 
			boolean isRemote, 
			boolean supportsCredentials, 
			boolean showPort, 
			boolean showPath,
			boolean readOnly,
			boolean showHidden,
			boolean showFolders,
			Class<? extends FileProvider> provider,
			FileService fileService) {
		this.scheme = scheme;
		this.isRemote = isRemote;
		this.showPort = showPort;
		this.showPath = showPath;
		this.readOnly = readOnly;
		this.showHidden = showHidden;
		this.showFolders = showFolders;
		this.supportsCredentials = supportsCredentials;
		this.provider = provider;
		this.fileService = fileService;
	}

	public String getScheme() {
		return scheme;
	}
	
	@JsonIgnore
	public FileService getFileService() {
		return fileService;
	}
	
	@JsonIgnore
	public Class<? extends FileProvider> getProvider() {
		return provider;
	}
	
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public boolean isRemote() {
		return isRemote;
	}

	public void setRemote(boolean isRemote) {
		this.isRemote = isRemote;
	}

	public boolean isShowPort() {
		return showPort;
	}

	public void setShowPort(boolean showPort) {
		this.showPort = showPort;
	}
	
	public boolean isSupportsCredentials() {
		return supportsCredentials;
	}

	public void setSupportsCredentials(boolean supportsCredentials) {
		this.supportsCredentials = supportsCredentials;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isShowHidden() {
		return showHidden;
	}

	public void setShowHidden(boolean showHidden) {
		this.showHidden = showHidden;
	}

	public boolean isShowFolders() {
		return showFolders;
	}

	public void setShowFolders(boolean showFolders) {
		this.showFolders = showFolders;
	}

	public boolean isShowPath() {
		return showPath;
	}

	public void setShowPath(boolean showPath) {
		this.showPath = showPath;
	}

	
}
