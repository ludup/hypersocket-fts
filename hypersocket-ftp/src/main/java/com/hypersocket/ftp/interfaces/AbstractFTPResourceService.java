package com.hypersocket.ftp.interfaces;

import org.apache.ftpserver.FtpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.certificates.CertificateResourceService;
import com.hypersocket.ftp.FTPFileSystemFactory;
import com.hypersocket.ftp.FTPUserManager;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.ip.IPRestrictionService;
import com.hypersocket.service.ManageableService;
import com.hypersocket.service.ServiceManagementService;
import com.hypersocket.session.SessionService;

public abstract class AbstractFTPResourceService implements ManageableService{
	
	
	static Logger log = LoggerFactory.getLogger(AbstractFTPResourceService.class);
	
	boolean running = false;
	FtpServer ftpServer;
	Throwable lastError = null;
	
	@Autowired 
	FTPInterfaceResourceService ftpInterfaceResourceService;
	
	@Autowired
	IPRestrictionService ipRestrictionService; 
	
	@Autowired
	FTPUserManager userManager;
	
	@Autowired
	FTPFileSystemFactory filesystemFactory;
	
	@Autowired
	ServiceManagementService serviceManagementService; 
	
	@Autowired
	SessionService sessionService;
	
	@Autowired
	I18NService i18nService;
	
	@Autowired
	CertificateResourceService certificateService;
	
	protected void postConstruct() {
		serviceManagementService.registerService(this);
	}
	
	@Override
	public void stop() {
		try {
			if(ftpServer!=null) {
				ftpServer.stop();
			}
			running = false;
		} catch (Exception e) {
			log.error("Failed to stop FTP service", e);
		}
	}
	
	public abstract boolean start();

	@Override
	public String getResourceKey() {
		return "ftp.service";
	}

	@Override
	public String getResourceBundle() {
		return FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE;
	}

	@Override
	public boolean isRunning() {
		return true;
	}
	
	@Override
	public boolean isError() {
		return lastError != null;
	}
	
	@Override
	public String getErrorText() {
		return lastError == null ? "" : lastError.getMessage();
	}
	
	public static String interfaceName(String ip, int port){
		return String.format("%s:%d", ip, port);
	}

}