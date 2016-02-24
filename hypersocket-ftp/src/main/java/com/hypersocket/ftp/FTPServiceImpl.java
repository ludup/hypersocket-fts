package com.hypersocket.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

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
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.hypersocket.auth.AuthenticationModuleType;
import com.hypersocket.auth.AuthenticationSchemeRepository;
import com.hypersocket.auth.UsernameAndPasswordAuthenticator;
import com.hypersocket.certificates.CertificateResourceService;
import com.hypersocket.config.ConfigurationChangedEvent;
import com.hypersocket.config.ConfigurationService;
import com.hypersocket.config.SystemConfigurationService;
import com.hypersocket.events.SystemEvent;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.ip.IPRestrictionService;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmAdapter;
import com.hypersocket.realm.RealmRepository;
import com.hypersocket.realm.RealmService;
import com.hypersocket.server.events.ServerStartedEvent;
import com.hypersocket.server.events.ServerStoppingEvent;
import com.hypersocket.service.ManageableService;
import com.hypersocket.service.ServiceManagementService;
import com.hypersocket.session.SessionService;
import com.hypersocket.session.json.SessionUtils;
import com.mysql.jdbc.StringUtils;

@Service
public class FTPServiceImpl implements FTPService,
		ApplicationListener<SystemEvent> {

	static final String RESOURCE_BUNDLE = "FTPService";

	public static final String AUTHENTICATION_SCHEME_NAME = "FTP";

	public static final String AUTHENTICATION_SCHEME_RESOURCE_KEY = "ftp";
	
	static Logger log = LoggerFactory.getLogger(FTPServiceImpl.class);

	@Autowired
	FTPUserManager userManager;

	@Autowired
	FTPFileSystemFactory filesystemFactory;

	@Autowired
	SystemConfigurationService systemConfigurationService;

	@Autowired
	ConfigurationService configurationService;
	
	@Autowired
	SessionService sessionService;

	@Autowired
	SessionUtils sessionUtils;

	@Autowired
	CertificateResourceService certificateService; 

	@Autowired
	RealmRepository realmRepository;
	
	@Autowired
	RealmService realmService;
	
	@Autowired
	I18NService i18nService;
	
	@Autowired
	AuthenticationSchemeRepository schemeRepository;
	
	@Autowired
	ServiceManagementService serviceManagementService; 
	
	@Autowired
	IPRestrictionService ipRestrictionService; 
	
	FTPService ftpService = new FTPService();
	FTPSService ftpsService = new FTPSService();
	
	@PostConstruct
	private void postConstruct() {
		i18nService.registerBundle(RESOURCE_BUNDLE);
		
		setupRealms();
		
		serviceManagementService.registerService(ftpService);
		serviceManagementService.registerService(ftpsService);
		
	}
	
	private void setupRealms() {

		realmService.registerRealmListener(new RealmAdapter() {
			
			public boolean hasCreatedDefaultResources(Realm realm) {
				return schemeRepository.getSchemeByResourceKeyCount(realm,
						AUTHENTICATION_SCHEME_RESOURCE_KEY) > 0;
			}
			public void onCreateRealm(Realm realm) {
				
				if (log.isInfoEnabled()) {
					log.info("Creating " + AUTHENTICATION_SCHEME_NAME
							+ " authentication scheme for realm "
							+ realm.getName());
				}
				
				List<String> modules = new ArrayList<String>();
				modules.add(UsernameAndPasswordAuthenticator.RESOURCE_KEY);
				schemeRepository.createScheme(realm,
						AUTHENTICATION_SCHEME_NAME, modules,
						AUTHENTICATION_SCHEME_RESOURCE_KEY, 
						false, 
						1, 
						AuthenticationModuleType.BASIC);
			}
		});

	}
	
	public void onApplicationEvent(final SystemEvent event) {

		sessionService.executeInSystemContext(new Runnable() {

			@Override
			public void run() {
				if (event instanceof ServerStartedEvent) {

					if (systemConfigurationService.getBooleanValue("ftp.enabled")) {
						startFTP();
					}
					if (systemConfigurationService.getBooleanValue("ftps.enabled")) {
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
						
						if(enabled) {
							startFTP();
						} else if(!enabled) {
							stopFTP();
						}
					}
					
					if(event.getAttribute(ConfigurationChangedEvent.ATTR_CONFIG_RESOURCE_KEY).equals("ftps.enabled")) {
						boolean enabled = Boolean.parseBoolean(event.getAttribute(ConfigurationChangedEvent.ATTR_NEW_VALUE).toString());
						
						if(enabled) {
							try {
								startFTPS();
							} catch (Exception e) {
								log.error("Failed to start FTPS server");
							}
						} else if(!enabled) {
							stopFTPS();
						}
					}
				}
			}
			
		});

		
		

	}
	
	private void stopFTP() {
		ftpService.stop();
	}
	
	private void stopFTPS() {
		ftpsService.stop();
	}
	
	private void startFTP() {
		ftpService.start();
	}
	
	
	private void startFTPS() {
		ftpsService.start();
	}
	
	class FTPService implements ManageableService {

		boolean running = false;
		FtpServer ftpServer;
		Throwable lastError = null;
		
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
		public void start() {
			
			FtpServerFactory serverFactory = new FtpServerFactory();

			String[] interfaces = systemConfigurationService.getValues("ftp.interfaces");
			if (interfaces != null && interfaces.length > 0) {
				boolean replacedDefault = false;
				for (String i : interfaces) {
					if (log.isInfoEnabled()) {
						log.info("Starting FTP server on " + i);
					}

					ListenerFactory factory = new ListenerFactory();

					int idleTime = systemConfigurationService
							.getIntValue("ftp.idleTimeout");
					// set the port of the listener
					factory.setPort(systemConfigurationService.getIntValue("ftp.port"));
					factory.setIdleTimeout(idleTime);
					factory.setServerAddress(i);
					
					String passivePorts = systemConfigurationService.getValue("ftp.passivePorts");
					
					String passiveExternalAddress = systemConfigurationService.getValue("ftp.passiveExternalInterface");
					if(StringUtils.isEmptyOrWhitespaceOnly(passiveExternalAddress)) {
						passiveExternalAddress = i;
					}
					
					PassivePorts ports = new PassivePorts(passivePorts, true);
					
					factory.setDataConnectionConfiguration(new DefaultDataConnectionConfiguration(
							idleTime, null, false, false, null, 0, i, ports, passiveExternalAddress, false));
					
					factory.setIpFilter(new IpFilter() {
						
						@Override
						public boolean accept(InetAddress address) {
							return !ipRestrictionService.isBlockedAddress(address);
						}
					});
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
				factory.setPort(systemConfigurationService.getIntValue("ftp.port"));
				factory.setIdleTimeout(systemConfigurationService
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
				lastError = null;
				running = true;
				if (log.isInfoEnabled()) {
					log.info("Started FTP server");
				}
			} catch (FtpException e) {
				log.error("Failed to start FTP server", e);
				lastError = e;
				ftpServer = null;
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
			return running;
		}
		
		@Override
		public boolean isError() {
			return lastError!=null;
		}
		
		@Override
		public String getErrorText() {
			return lastError==null ? "" : lastError.getMessage();
		}
		
	}
	
	class FTPSService implements ManageableService {

		boolean running = false;
		FtpServer ftpsServer;
		Throwable lastError = null;
		
		@Override
		public void stop() {
			
			try {
				if(ftpsServer!=null) {
					ftpsServer.stop();
				}
			} catch (Throwable t) {
				log.error("Failed to stop FTPS service", t);
			}
			
		}

		@Override
		public void start() {
			
			try {
				FtpServerFactory serverFactory = new FtpServerFactory();
	
				certificateService.setCurrentPrincipal(realmService
						.getSystemPrincipal(), configurationService.getDefaultLocale(),
						realmService.getSystemPrincipal().getRealm());
				
				KeyStore keystore = certificateService.getDefaultCertificate();
				
				certificateService.clearPrincipalContext();
				
				File tmp = File.createTempFile("ftps", ".tmp");
				keystore.store(new FileOutputStream(tmp), "changeit".toCharArray());
				
				String[] interfaces = systemConfigurationService.getValues("ftps.interfaces");
				if (interfaces != null && interfaces.length > 0) {
					boolean replacedDefault = false;
					for (String i : interfaces) {
						if (log.isInfoEnabled()) {
							log.info("Starting FTPS server on " + i);
						}
	
						ListenerFactory factory = new ListenerFactory();
	
						int idleTime = systemConfigurationService
								.getIntValue("ftps.idleTimeout");
						
						// set the port of the listener
						factory.setPort(systemConfigurationService.getIntValue("ftps.port"));
						factory.setIdleTimeout(systemConfigurationService
								.getIntValue("ftps.idleTimeout"));
						factory.setServerAddress(i);
						
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

						String passivePorts = systemConfigurationService.getValue("ftps.passivePorts");
						
						String passiveExternalAddress = systemConfigurationService.getValue("ftps.passiveExternalInterface");
						if(StringUtils.isEmptyOrWhitespaceOnly(passiveExternalAddress)) {
							passiveExternalAddress = i;
						}
						
						PassivePorts ports = new PassivePorts(passivePorts, true);
						
						factory.setDataConnectionConfiguration(new DefaultDataConnectionConfiguration(
								idleTime, sslConfig, false, false, null, 0, i, ports, passiveExternalAddress, true));
						
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
					factory.setPort(systemConfigurationService.getIntValue("ftps.port"));
					factory.setIdleTimeout(systemConfigurationService
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
					lastError = null;
					running = true;
					if (log.isInfoEnabled()) {
						log.info("Started FTPS server");
					}
				} catch (FtpException e) {
					log.error("Failed to start FTPS server", e);
					lastError = e;
					ftpsServer = null;
				}
			} catch(Throwable t) {
				log.error("Failed to start FTPS service", t);
			}
		}

		@Override
		public String getResourceKey() {
			return "ftps.service";
		}

		@Override
		public String getResourceBundle() {
			return RESOURCE_BUNDLE;
		}

		@Override
		public boolean isRunning() {
			return running;
		}
		

		@Override
		public boolean isError() {
			return lastError!=null;
		}
		
		@Override
		public String getErrorText() {
			return lastError==null ? "" : lastError.getMessage();
		}
		
	}

}
