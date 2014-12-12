package com.hypersocket.fs;

public class FileResourceScheme {

	String scheme;
	boolean isRemote;
	boolean supportsCredentials;
	boolean showPort;
	
	public FileResourceScheme() {
	}
	
	public FileResourceScheme(String scheme, boolean isRemote, boolean supportsCredentials, boolean showPort) {
		this.scheme = scheme;
		this.isRemote = isRemote;
		this.showPort = showPort;
		this.supportsCredentials = supportsCredentials;
	}

	public String getScheme() {
		return scheme;
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
