package com.hypersocket.ftp;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hypersocket.auth.AuthenticationService;
import com.hypersocket.config.ConfigurationService;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.realm.RealmService;
import com.hypersocket.session.Session;
import com.hypersocket.session.SessionService;

@Component
public class FTPFileSystemFactory implements FileSystemFactory {

	@Autowired
	I18NService i18nService;
	
	@Autowired
	FileResourceService fileResourceService;
	
	@Autowired
	RealmService realmService;
	
	@Autowired
	PermissionService permissionService;
	
	@Autowired
	AuthenticationService authenticationService;
	
	@Autowired
	SessionService sessionService;
	
	@Autowired
	ConfigurationService configurationService;
	
	public FileSystemView createFileSystemView(User user) throws FtpException {
		FTPSessionUser sessionUser = (FTPSessionUser) user;
		return new SessionContextFileSystemViewAdapter(sessionUser, new FTPFileSystem((FTPSessionUser)user, this), this);
	}
	
	
	public FileResourceService getFileResourceService() {
		return fileResourceService;
	}
	
	public I18NService getI18NService() {
		return i18nService;
	}
	
	public void setupSessionContext(Session session) {
		fileResourceService.setCurrentSession(session, i18nService.getDefaultLocale());
		realmService.setCurrentSession(session, i18nService.getDefaultLocale());
		permissionService.setCurrentSession(session, i18nService.getDefaultLocale());
		authenticationService.setCurrentSession(session, i18nService.getDefaultLocale());
		sessionService.setCurrentSession(session, i18nService.getDefaultLocale());
		configurationService.setCurrentSession(session, i18nService.getDefaultLocale());
	}
	
	public void clearSessionContext() {
		authenticationService.clearPrincipalContext();
		sessionService.clearPrincipalContext();
		realmService.clearPrincipalContext();
		permissionService.clearPrincipalContext();
		configurationService.clearPrincipalContext();
	}

}
