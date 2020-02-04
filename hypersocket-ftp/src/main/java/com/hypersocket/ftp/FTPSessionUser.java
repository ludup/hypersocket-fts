package com.hypersocket.ftp;

import java.util.Collection;
import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Principal;
import com.hypersocket.session.Session;

public class FTPSessionUser implements User {

	private Session session;
	private String password;
	private Collection<FileResource> mounts;
	
	public FTPSessionUser(Session session, String password, Collection<FileResource> mounts) {
		this.session = session;
		this.mounts = mounts;
	}
	
	public Principal getPrincipal() {
		return session.getCurrentPrincipal();
	}

	public AuthorizationRequest authorize(AuthorizationRequest arg0) {
		return arg0;
	}

	public List<Authority> getAuthorities() {
		return null;
	}

	public List<Authority> getAuthorities(Class<? extends Authority> arg0) {
		return null;
	}

	public boolean getEnabled() {
		return true;
	}

	public String getHomeDirectory() {
		return "/";
	}

	public int getMaxIdleTime() {
		return 0;
	}

	public String getName() {
		return session.getCurrentPrincipal().getPrincipalName();
	}

	public String getPassword() {
		return password;
	}
	
	public Session getSession() {
		return session;
	}
	
	boolean hasSingleMount() {
		return mounts.size() == 1;
	}
	
	Collection<FileResource> getMounts() {
		return mounts;
	}

}
