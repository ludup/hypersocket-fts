package com.hypersocket.ftp.interfaces;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.impl.DefaultDataConnectionConfiguration;
import org.apache.ftpserver.impl.PassivePorts;
import org.apache.ftpserver.ipfilter.IpFilter;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.certificates.CertificateResource;
import com.hypersocket.certificates.CertificateResourceService;
import com.hypersocket.ftp.FTPFileSystemFactory;
import com.hypersocket.ftp.FTPSessionUser;
import com.hypersocket.ftp.FTPUserManager;
import com.hypersocket.ftp.HypersocketListenerFactory;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.ip.IPRestrictionService;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.service.ManageableService;
import com.hypersocket.service.ServiceManagementService;
import com.hypersocket.service.ServiceStatus;
import com.hypersocket.session.SessionService;

@Service
public class FTPResourceService implements ManageableService{
	
	static Logger log = LoggerFactory.getLogger(FTPResourceService.class);
	

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
	
	@PostConstruct
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
	


	
	@Override
	public Collection<ServiceStatus> getStatus() {
		ServiceStatus status = new ServiceStatus() {
			@Override
			public String getResourceKey() {
				return "ftp.service";
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
			
			@Override
			public String getGroup() {
				return "servers";
			}
		};
		return Arrays.asList(status);
	}
	
	@Override
	public boolean start() {
		
		certificateService.setCurrentSession(sessionService.getSystemSession(),	Locale.getDefault());
		certificateService.clearPrincipalContext();
		
		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(userManager);
		serverFactory.setFileSystem(filesystemFactory);
		serverFactory.getListeners().remove("default");
		
		
		serverFactory.getFtplets().put("default", getDefaultFtplets());

		List<FTPInterfaceResource> resources = ftpInterfaceResourceService.allResources();
		
		if(resources == null || resources.isEmpty()){
			lastError = null;
			running = false;
			ftpServer = null;
			return false;
		}
		
		for (FTPInterfaceResource ftpInterfaceResource : resources) {
			createFtpListeners(serverFactory, ftpInterfaceResource);
		}
		// start the server
		ftpServer = serverFactory.createServer();
		
		try {
			ftpServer.start();
			lastError = null;
			running = true;
			if (log.isInfoEnabled()) {
				log.info("Started FTP server");
			}
			return true;
		} catch (FtpException e) {
			log.error("Failed to start FTP server", e);
			lastError = e;
			ftpServer = null;
			return false;
		}
	}

	private DefaultFtplet getDefaultFtplets() {
		return new DefaultFtplet() {

			@Override
			public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
				return super.onConnect(session);
			}

			@Override
			public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {

				if (session.getUser() != null) {
					sessionService.closeSession(((FTPSessionUser) session.getUser()).getSession());
				}
				return super.onDisconnect(session);
			}

		};
	}

	public String[] createFtpListeners(FtpServerFactory serverFactory, FTPInterfaceResource ftpInterfaceResource) {
		String[] interfaces;
		if(ftpInterfaceResource.getAllInterfaces()) {
			interfaces = new String[] { "::" };
		} else {
			interfaces = ResourceUtils.explodeValues(ftpInterfaceResource.getInterfaces());
		}
		if (interfaces != null) {
			for (String intface : interfaces) {
				ListenerFactory factory = createListener(ftpInterfaceResource, intface, getSslConfiguration(ftpInterfaceResource));
				serverFactory.addListener(interfaceName(intface, ftpInterfaceResource.getPort()), ((HypersocketListenerFactory)factory).createListener(ftpInterfaceResource));
			}
		} 
		return interfaces;
	}
	

	public SslConfiguration getSslConfiguration(FTPInterfaceResource ftpInterfaceResource) {
		try{
			if(FTPProtocol.FTP.equals(ftpInterfaceResource.ftpProtocol)){
				return null;
			}

			CertificateResource certificateResource = ftpInterfaceResource.ftpCertificate;
			KeyStore keyStore = null;
			if(certificateResource == null){
				keyStore = certificateService.getDefaultCertificate();
			}else{
				keyStore = certificateService.getResourceKeystore(certificateResource);
			}
			
			File tmp = File.createTempFile(String.format("ftps_%s", ftpInterfaceResource.getName()), ".tmp");
			keyStore.store(new FileOutputStream(tmp), "changeit".toCharArray());
			
			// define SSL configuration
			SslConfigurationFactory ssl = new SslConfigurationFactory();
			ssl.setKeystoreFile(tmp);
			ssl.setKeystorePassword("changeit");
			
			return ssl.createSslConfiguration();
		}catch(Exception e){
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public ListenerFactory createListener(FTPInterfaceResource ftpInterfaceResource, String intface, SslConfiguration sslConfig) {
		if (log.isInfoEnabled()) {
			log.info(String.format("Starting FTP server on interface %s:%d:%s ",intface, ftpInterfaceResource.getPort(), ftpInterfaceResource.ftpProtocol.name()));
		}

		ListenerFactory factory = new HypersocketListenerFactory();

		int idleTime = ftpInterfaceResource.ftpIdleTimeout;
		// set the port of the listener
		factory.setPort(ftpInterfaceResource.getPort());
		factory.setIdleTimeout(idleTime);
		factory.setServerAddress(intface);
		
		String passivePorts = ftpInterfaceResource.ftpPassivePorts;
		
		String passiveExternalAddress = ftpInterfaceResource.ftpPassiveExternalAddress;
		if(StringUtils.isBlank(passiveExternalAddress)) {
			passiveExternalAddress = null;
		}
		
		if(sslConfig != null){
			factory.setSslConfiguration(sslConfig);
			factory.setImplicitSsl(false);
		}
		
		PassivePorts ports = new PassivePorts(passivePorts, true);
		
		factory.setDataConnectionConfiguration(new DefaultDataConnectionConfiguration(
				idleTime, sslConfig, false, false, null, 
				ftpInterfaceResource.getPort(), intface,
				ports, passiveExternalAddress, false));
		
		factory.setIpFilter(new IpFilter() {
			
			@Override
			public boolean accept(InetAddress address) {
				return !ipRestrictionService.isBlockedAddress(address);
			}
		});
		return factory;
	}
	
	public static String interfaceName(String ip, int port){
		return String.format("%s:%d", ip, port);
	}
	
	@Override
	public boolean isSystem() {
		return true;
	}

}
