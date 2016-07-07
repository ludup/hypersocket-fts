package com.hypersocket.ftp.interfaces;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

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
import org.springframework.stereotype.Service;

import com.hypersocket.certificates.CertificateResource;
import com.hypersocket.ftp.FTPSessionUser;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.service.ManageableService;
import com.mysql.jdbc.StringUtils;

@Service
public class FTPResourceService extends AbstractFTPResourceService implements ManageableService{
	
	static Logger log = LoggerFactory.getLogger(FTPResourceService.class);
	

	@PostConstruct
	protected void postConstruct() {
		super.postConstruct();
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
		String[] interfaces = ResourceUtils.explodeValues(ftpInterfaceResource.ftpInterfaces);
		
		if (interfaces != null) {
			for (String intface : interfaces) {
				ListenerFactory factory = createListener(ftpInterfaceResource, intface, getSslConfiguration(ftpInterfaceResource));
				serverFactory.addListener(interfaceName(intface, ftpInterfaceResource.ftpPort), factory.createListener());
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
			throw new IllegalStateException(e);
		}
	}

	public ListenerFactory createListener(FTPInterfaceResource ftpInterfaceResource, String intface, SslConfiguration sslConfig) {
		if (log.isInfoEnabled()) {
			log.info(String.format("Starting FTP server on interface %s:%d:%s ",intface, ftpInterfaceResource.ftpPort, ftpInterfaceResource.ftpProtocol.name()));
		}

		ListenerFactory factory = new ListenerFactory();

		int idleTime = ftpInterfaceResource.ftpIdleTimeout;
		// set the port of the listener
		factory.setPort(ftpInterfaceResource.ftpPort);
		factory.setIdleTimeout(idleTime);
		factory.setServerAddress(intface);
		
		String passivePorts = ftpInterfaceResource.ftpPassivePorts;
		
		String passiveExternalAddress = ftpInterfaceResource.ftpPassiveExternalAddress;
		if(StringUtils.isEmptyOrWhitespaceOnly(passiveExternalAddress)) {
			passiveExternalAddress = intface;
		}
		
		if(sslConfig != null){
			factory.setSslConfiguration(sslConfig);
			factory.setImplicitSsl(true);
		}
		
		PassivePorts ports = new PassivePorts(passivePorts, true);
		
		factory.setDataConnectionConfiguration(new DefaultDataConnectionConfiguration(
				idleTime, sslConfig, false, false, null, ftpInterfaceResource.ftpPort, intface, ports, passiveExternalAddress, sslConfig != null));
		
		factory.setIpFilter(new IpFilter() {
			
			@Override
			public boolean accept(InetAddress address) {
				return !ipRestrictionService.isBlockedAddress(address);
			}
		});
		return factory;
	}

}
