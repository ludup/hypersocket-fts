package com.hypersocket.ftp;

import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

import com.hypersocket.realm.Principal;
import com.hypersocket.session.Session;

public class FTPSessionUser implements User {

	Session session;;
	String password;
	
	public FTPSessionUser(Session session, String password) {
		this.session = session;
	}
	
	public Principal getPrincipal() {
		return session.getPrincipal();
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
		return session.getPrincipal().getPrincipalName();
	}

	public String getPassword() {
		return password;
	}
	
	public Session getSession() {
		return session;
	}

}
