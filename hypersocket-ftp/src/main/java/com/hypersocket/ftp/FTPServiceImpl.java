package com.hypersocket.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.hypersocket.certs.CertificateService;
import com.hypersocket.config.ConfigurationChangedEvent;
import com.hypersocket.config.ConfigurationService;
import com.hypersocket.events.SystemEvent;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.realm.RealmService;
import com.hypersocket.server.events.ServerStartedEvent;
import com.hypersocket.server.events.ServerStoppingEvent;
import com.hypersocket.session.SessionService;
import com.hypersocket.session.json.SessionUtils;

@Service
public class FTPServiceImpl implements FTPService,
		ApplicationListener<SystemEvent> {

	static final String RESOURCE_BUNDLE = "FTPService";
	
	static Logger log = LoggerFactory.getLogger(FTPServiceImpl.class);

	@Autowired
	FTPUserManager userManager;

	@Autowired
	FTPFileSystemFactory filesystemFactory;

	@Autowired
	ConfigurationService configurationService;

	@Autowired
	SessionService sessionService;

	@Autowired
	SessionUtils sessionUtils;

	@Autowired
	CertificateService certificateService; 
	
	@Autowired
	RealmService realmService;
	
	@Autowired
	I18NService i18nService;
	
	FtpServer ftpServer;
	FtpServer ftpsServer;
	
	@PostConstruct
	private void postConstruct() {
		i18nService.registerBundle(RESOURCE_BUNDLE);
	}
	
	public void onApplicationEvent(SystemEvent event) {

		if (event instanceof ServerStartedEvent) {

			if (configurationService.getBooleanValue("ftp.enabled")) {
				startFTP();
			}
			if (configurationService.getBooleanValue("ftps.enabled")) {
				try {
					startFTPS();
				} catch (Exception e) {
					log.error("Failed to start FTPS server", e);
				}
			}
		} else if (event instanceof ServerStoppingEvent) {
		
			stopFTP();
			stopFTPS();
		
		} else if(event instanceof ConfigurationChangedEvent) {
			
			if(event.getAttribute(ConfigurationChangedEvent.ATTR_CONFIG_RESOURCE_KEY).equals("ftp.enabled")) {
				boolean enabled = Boolean.parseBoolean(event.getAttribute(ConfigurationChangedEvent.ATTR_NEW_VALUE).toString());
				
				if(enabled && ftpServer==null) {
					startFTP();
				} else if(!enabled && ftpServer!=null) {
					stopFTP();
				}
			}
			
			if(event.getAttribute(ConfigurationChangedEvent.ATTR_CONFIG_RESOURCE_KEY).equals("ftps.enabled")) {
				boolean enabled = Boolean.parseBoolean(event.getAttribute(ConfigurationChangedEvent.ATTR_NEW_VALUE).toString());
				
				if(enabled && ftpsServer==null) {
					try {
						startFTPS();
					} catch (Exception e) {
						log.error("Failed to start FTPS server");
					}
				} else if(!enabled && ftpsServer!=null) {
					stopFTPS();
				}
			}
		}

	}
	
	private void stopFTP() {
		if(ftpServer!=null) {
			if (log.isInfoEnabled()) {
				log.info("Stopping FTP servers");
			}

			ftpServer.stop();
			
			if (log.isInfoEnabled()) {
				log.info("Stopped FTP servers");
			}
		}
	}
	
	private void stopFTPS() {
		if(ftpsServer!=null) {
			if (log.isInfoEnabled()) {
				log.info("Stopping FTPS servers");
			}

			ftpsServer.stop();
			
			if (log.isInfoEnabled()) {
				log.info("Stopped FTPS servers");
			}
		}
	}
	
	private void startFTP() {

		FtpServerFactory serverFactory = new FtpServerFactory();

		String[] interfaces = configurationService.getValues("ftp.interfaces");
		if (interfaces != null && interfaces.length > 0) {
			boolean replacedDefault = false;
			for (String i : interfaces) {
				if (log.isInfoEnabled()) {
					log.info("Starting FTP server on " + i);
				}

				ListenerFactory factory = new ListenerFactory();

				// set the port of the listener
				factory.setPort(configurationService.getIntValue("ftp.port"));
				factory.setIdleTimeout(configurationService
						.getIntValue("ftp.idleTimeout"));
				factory.setServerAddress(i);

				if (!replacedDefault) {
					serverFactory.addListener("default",
							factory.createListener());
					replacedDefault = true;
				} else {
					serverFactory.addListener(i, factory.createListener());
				}
			}
		} else {
			ListenerFactory factory = new ListenerFactory();

			// set the port of the listener
			factory.setPort(configurationService.getIntValue("ftp.port"));
			factory.setIdleTimeout(configurationService
					.getIntValue("ftp.idleTimeout"));

			serverFactory.addListener("default", factory.createListener());
		}

		// start the server
		serverFactory.setUserManager(userManager);
		serverFactory.setFileSystem(filesystemFactory);
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
					sessionService.closeSession(((FTPSessionUser) session
							.getUser()).getSession());
				}
				return super.onDisconnect(session);
			}

		});
		ftpServer = serverFactory.createServer();

		try {
			ftpServer.start();

			if (log.isInfoEnabled()) {
				log.info("Started FTP server");
			}
		} catch (FtpException e) {
			log.error("Failed to start FTP server", e);
			ftpServer = null;
		}
	}
	
	
	private void startFTPS() throws CertificateException, AccessDeniedException, IOException, KeyStoreException, NoSuchAlgorithmException {
		
		
		FtpServerFactory serverFactory = new FtpServerFactory();

		certificateService.setCurrentPrincipal(realmService
				.getSystemPrincipal(), Locale.getDefault(),
				realmService.getSystemPrincipal().getRealm());
		
		KeyStore keystore = certificateService.getDefaultCertificate();
		
		certificateService.clearPrincipalContext();
		
		File tmp = File.createTempFile("ftps", ".tmp");
		keystore.store(new FileOutputStream(tmp), "changeit".toCharArray());
		
		String[] interfaces = configurationService.getValues("ftps.interfaces");
		if (interfaces != null && interfaces.length > 0) {
			boolean replacedDefault = false;
			for (String i : interfaces) {
				if (log.isInfoEnabled()) {
					log.info("Starting FTPS server on " + i);
				}

				ListenerFactory factory = new ListenerFactory();

				// set the port of the listener
				factory.setPort(configurationService.getIntValue("ftps.port"));
				factory.setIdleTimeout(configurationService
						.getIntValue("ftps.idleTimeout"));
				factory.setServerAddress(i);

				// define SSL configuration
				SslConfigurationFactory ssl = new SslConfigurationFactory();
				ssl.setKeystoreFile(tmp);
				ssl.setKeystorePassword("changeit");
				
				factory.setSslConfiguration(ssl.createSslConfiguration());
				factory.setImplicitSsl(true);
				
				if (!replacedDefault) {
					serverFactory.addListener("default",
							factory.createListener());
					replacedDefault = true;
				} else {
					serverFactory.addListener(i, factory.createListener());
				}
			}
		} else {
			ListenerFactory factory = new ListenerFactory();

			// set the port of the listener
			factory.setPort(configurationService.getIntValue("ftps.port"));
			factory.setIdleTimeout(configurationService
					.getIntValue("ftps.idleTimeout"));
			
			// define SSL configuration
			SslConfigurationFactory ssl = new SslConfigurationFactory();
			ssl.setKeystoreFile(tmp);
			ssl.setKeystorePassword("changeit");
			factory.setSslConfiguration(ssl.createSslConfiguration());
			factory.setImplicitSsl(true);
			
			serverFactory.addListener("default", factory.createListener());
		}
		

		// start the server
		serverFactory.setUserManager(userManager);
		serverFactory.setFileSystem(filesystemFactory);
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
					sessionService.closeSession(((FTPSessionUser) session
							.getUser()).getSession());
				}
				return super.onDisconnect(session);
			}

		});
		ftpsServer = serverFactory.createServer();

		try {
			ftpsServer.start();

			if (log.isInfoEnabled()) {
				log.info("Started FTPS server");
			}
		} catch (FtpException e) {
			log.error("Failed to start FTPS server", e);
			ftpsServer = null;
		}
	}

}
