package com.hypersocket.fs;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.plexus.util.StringUtils;

import com.hypersocket.resource.AssignableResource;
import com.hypersocket.util.FileUtils;
import com.hypersocket.util.Utils;

@Entity
@Table(name="file_resources")
public class FileResource extends AssignableResource {

	@Column(name="scheme")
	String scheme;
	
	@Column(name="server", nullable=true)
	String server;
	
	@Column(name="port", nullable=true)
	Integer port;
	
	@Column(name="path")
	String path;
	
	@Column(name="username", nullable=true)
	String username;
	
	@Column(name="password", nullable=true)
	String password;

	@Column(name="read_only")
	boolean readOnly;
	
	@Column(name="show_hidden")
	boolean showHidden;
	
	@Column(name="show_folders")
	boolean showFolders;
	
	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
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
	
	public String getUrl() {
		return getUrl(true, null, null);
	}
	
	@JsonIgnore
	@XmlTransient
	public String getPrivateUrl(String username, String password) {
		return getUrl(false, username, password);
	}
	
	private String getUrl(boolean friendly, String currentUsername, String currentPassword) {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append(scheme);
			buf.append("://");
			
			if(StringUtils.isNotBlank(username)) {
				if(friendly) {
					buf.append(username);
				} else {
					buf.append(URLEncoder.encode(username.equals("${principalName}") ? StringUtils.clean(currentUsername) : username, "UTF-8"));
				}
				if(StringUtils.isNotBlank(password)) {
					buf.append(":");
					if(friendly) {
						buf.append("***");
					} else {
						buf.append(URLEncoder.encode(password.equals("${password}") ? currentPassword : password, "UTF-8"));
					}
				}
				buf.append("@");
			} 
			
			if(server!=null && !server.equals("")) {
				buf.append(server);
				if(port!=null) {
					buf.append(":");
					buf.append(port);
				}
			}

			String thisPath = path;
			if(!friendly) {
				thisPath = thisPath.replace("${principalName}", currentUsername);
			}
			buf.append(FileUtils.checkStartsWithSlash(Utils.checkNull(thisPath)));
			return buf.toString();
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("The system does not appear to support UTF-8!");
		}
	}

	
}
