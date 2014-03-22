package com.hypersocket.ftp;

import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

import com.hypersocket.realm.Principal;

public class FTPUser implements User {

	Principal principal;;
	String password;
	
	public FTPUser(Principal principal, String password) {
		this.principal = principal;
	}
	
	public Principal getPrincipal() {
		return principal;
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
		return principal.getPrincipalName();
	}

	public String getPassword() {
		return password;
	}

}
