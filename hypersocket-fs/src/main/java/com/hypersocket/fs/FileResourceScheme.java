package com.hypersocket.fs;

import org.apache.commons.vfs2.provider.FileProvider;

public class FileResourceScheme {

	String scheme;
	boolean isRemote;
	boolean supportsCredentials;
	boolean showPort;
	Class<? extends FileProvider> provider;
	
	public FileResourceScheme() {
	}
	
	public FileResourceScheme(String scheme, boolean isRemote, boolean supportsCredentials, boolean showPort) {
		this(scheme, isRemote, supportsCredentials, showPort, null);
	}
	
	public FileResourceScheme(String scheme, boolean isRemote, boolean supportsCredentials, boolean showPort, Class<? extends FileProvider> provider) {
		this.scheme = scheme;
		this.isRemote = isRemote;
		this.showPort = showPort;
		this.supportsCredentials = supportsCredentials;
		this.provider = provider;
	}

	public String getScheme() {
		return scheme;
	}

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
	
	

}
