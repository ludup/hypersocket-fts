package com.hypersocket.ftp.interfaces;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.certificates.CertificateResourceService;
import com.hypersocket.config.ConfigurationChangedEvent;
import com.hypersocket.events.SystemEvent;
import com.hypersocket.ftp.FTPFileSystemFactory;
import com.hypersocket.ftp.FTPUserManager;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.ip.IPRestrictionService;
import com.hypersocket.server.events.ServerStartedEvent;
import com.hypersocket.server.events.ServerStoppingEvent;
import com.hypersocket.service.ManageableService;
import com.hypersocket.service.ServiceManagementService;
import com.hypersocket.session.SessionService;

public abstract class AbstractFTPResourceService implements ManageableService{
	
	
	static final String RESOURCE_BUNDLE = "FTPService";
	
	static Logger log = LoggerFactory.getLogger(AbstractFTPResourceService.class);
	
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
	
	protected Map<String, FtpServerStatus> serverStatusMap = new HashMap<>();

	protected void postConstruct() {
		i18nService.registerBundle(RESOURCE_BUNDLE);
		serviceManagementService.registerService(this);
	}
	
	public void onApplicationEvent(final SystemEvent event) {

		sessionService.executeInSystemContext(new Runnable() {

			@Override
			public void run() {
				if (event instanceof ServerStartedEvent) {
					start();
				} else if (event instanceof ServerStoppingEvent) {
					stop();
				} else if(event instanceof ConfigurationChangedEvent) {
					// TODO Ask Lee
				}
			}
			
		});
	}
	
	@Override
	public void stop() {
		try {
			Set<String> keys = serverStatusMap.keySet();
			for (String key : keys) {
				FtpServerStatus ftpServerStatus = serverStatusMap.get(key);
				if(ftpServerStatus.ftpServer != null) {
					ftpServerStatus.ftpServer.stop();
				}
			}
			
		} catch (Exception e) {
			log.error("Failed to stop FTP service", e);
		}
		
	}

	@Override
	public String getResourceKey() {
		return "ftp.service";
	}

	@Override
	public String getResourceBundle() {
		return RESOURCE_BUNDLE;
	}

	@Override
	public boolean isRunning() {
		return true;
	}
	
	@Override
	public boolean isError() {
		return false;//lastError!=null;
	}
	
	@Override
	public String getErrorText() {
		return ""; ///lastError==null ? "" : lastError.getMessage();
	}

	public Map<String, FtpServerStatus> getServerStatusMap() {
		return serverStatusMap;
	}

	public void setServerStatusMap(Map<String, FtpServerStatus> serverStatusMap) {
		this.serverStatusMap = serverStatusMap;
	}
}
