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

import com.hypersocket.ftp.FTPSessionUser;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.service.ManageableService;
import com.mysql.jdbc.StringUtils;

@Service
public class FTPSResourceService extends AbstractFTPResourceService implements ManageableService{
	
	static Logger log = LoggerFactory.getLogger(FTPSResourceService.class);

	@PostConstruct
	protected void postConstruct() {
		super.postConstruct();
	}
	
	@Override
	public boolean start() {
		try{
		
			FtpServerFactory serverFactory = new FtpServerFactory();
			serverFactory.setUserManager(userManager);
			serverFactory.setFileSystem(filesystemFactory);
			
			certificateService.setCurrentSession(sessionService.getSystemSession(), 
					Locale.getDefault());
			
			KeyStore keystore = certificateService.getDefaultCertificate();
			
			certificateService.clearPrincipalContext();
			
			File tmp = File.createTempFile("ftps", ".tmp");
			keystore.store(new FileOutputStream(tmp), "changeit".toCharArray());
			
			
			
			serverFactory.getFtplets().put("default", new DefaultFtplet() {
	
				@Override
				public FtpletResult onConnect(FtpSession session)
						throws FtpException, IOException {
					return super.onConnect(session);
				}
	
				@Override
				public FtpletResult onDisconnect(FtpSession session)
						throws FtpException, IOException {
	
					if (session.getUser() != null) {
						sessionService.closeSession(((FTPSessionUser) session.getUser()).getSession());
					}
					return super.onDisconnect(session);
				}
	
			});
	
			List<FTPInterfaceResource> resources = ftpInterfaceResourceService.allResources();
			
			for (FTPInterfaceResource ftpInterfaceResource : resources) {
				
				if(FTPProtocol.FTP.equals(ftpInterfaceResource.ftpProtocol)){
					continue;
				}
				
				String[] interfaces = ResourceUtils.explodeValues(ftpInterfaceResource.ftpInterfaces);
				
				if (interfaces != null && interfaces.length > 0) {
					for (String intface : interfaces) {
						if (log.isInfoEnabled()) {
							log.info("Starting FTPS server on " + intface);
						}
	
						ListenerFactory factory = new ListenerFactory();
	
						int idleTime = ftpInterfaceResource.ftpIdleTimeout;
						
						// set the port of the listener
						factory.setPort(ftpInterfaceResource.ftpPort);
						factory.setIdleTimeout(idleTime);
						factory.setServerAddress(intface);
						
						factory.setIpFilter(new IpFilter() {
							
							@Override
							public boolean accept(InetAddress address) {
								return !ipRestrictionService.isBlockedAddress(address);
							}
						});
						// define SSL configuration
						SslConfigurationFactory ssl = new SslConfigurationFactory();
						ssl.setKeystoreFile(tmp);
						ssl.setKeystorePassword("changeit");
						
						SslConfiguration sslConfig = ssl.createSslConfiguration();
						factory.setSslConfiguration(sslConfig);
						factory.setImplicitSsl(true);
	
						String passivePorts = ftpInterfaceResource.ftpPassivePorts;
						
						String passiveExternalAddress = ftpInterfaceResource.ftpPassiveExternalAddress;
						if(StringUtils.isEmptyOrWhitespaceOnly(passiveExternalAddress)) {
							passiveExternalAddress = intface;
						}
						
						PassivePorts ports = new PassivePorts(passivePorts, true);
						
						factory.setDataConnectionConfiguration(new DefaultDataConnectionConfiguration(
								idleTime, sslConfig, false, false, null, 0, intface, ports, passiveExternalAddress, true));
						
						serverFactory.addListener(intface, factory.createListener());
					}
				} else {
					ListenerFactory factory = new ListenerFactory();
	
					// set the port of the listener
					factory.setPort(ftpInterfaceResource.ftpPort);
					factory.setIdleTimeout(ftpInterfaceResource.ftpIdleTimeout);
					
					// define SSL configuration
					SslConfigurationFactory ssl = new SslConfigurationFactory();
					ssl.setKeystoreFile(tmp);
					ssl.setKeystorePassword("changeit");
					factory.setSslConfiguration(ssl.createSslConfiguration());
					factory.setImplicitSsl(true);
					
					serverFactory.addListener("default", factory.createListener());
				}
				
				// start the server
				ftpServer = serverFactory.createServer();
				
				
				try {
					ftpServer.start();
					lastError = null;
					running = true;
					if (log.isInfoEnabled()) {
						log.info("Started FTPS server");
					}
				} catch (FtpException e) {
					log.error("Failed to start FTPS server", e);
					lastError = e;
					ftpServer = null;
				}
				
			}
			
		}catch(Exception e){
			log.error("Failed to start FTPS service", e);
		}
		return true;
	}

}
