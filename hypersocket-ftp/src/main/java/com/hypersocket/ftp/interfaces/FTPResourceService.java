package com.hypersocket.ftp.interfaces;

import java.io.IOException;
import java.net.InetAddress;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
		
		FtpServerFactory serverFactory = new FtpServerFactory();
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
					sessionService.closeSession(((FTPSessionUser) session.getUser()).getSession());
				}
				return super.onDisconnect(session);
			}

		});

		List<FTPInterfaceResource> resources = ftpInterfaceResourceService.allResources();
		
		for (FTPInterfaceResource ftpInterfaceResource : resources) {
			
			if(FTPProtocol.FTPS.equals(ftpInterfaceResource.ftpProtocol)){
				continue;
			}
			
			String[] interfaces = ResourceUtils.explodeValues(ftpInterfaceResource.ftpInterfaces);
			
			if (interfaces != null && interfaces.length > 0) {
				for (String intface : interfaces) {
					if (log.isInfoEnabled()) {
						log.info("Starting FTP server on " + intface);
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
					
					PassivePorts ports = new PassivePorts(passivePorts, true);
					
					factory.setDataConnectionConfiguration(new DefaultDataConnectionConfiguration(
							idleTime, null, false, false, null, 0, intface, ports, passiveExternalAddress, false));
					
					factory.setIpFilter(new IpFilter() {
						
						@Override
						public boolean accept(InetAddress address) {
							return !ipRestrictionService.isBlockedAddress(address);
						}
					});
					
					serverFactory.addListener(intface, factory.createListener());
				}
			} else {
				ListenerFactory factory = new ListenerFactory();

				// set the port of the listener
				factory.setPort(ftpInterfaceResource.ftpPort);
				factory.setIdleTimeout(ftpInterfaceResource.ftpIdleTimeout);

				serverFactory.addListener(ftpInterfaceResource.getName(), factory.createListener());
			}
			
			// start the server
			FtpServer ftpServer = serverFactory.createServer();
			
			FtpServerStatus ftpServerStatus = new FtpServerStatus();
			ftpServerStatus.ftpServer = ftpServer;
			
			try {
				ftpServer.start();
				ftpServerStatus.lastError = null;
				ftpServerStatus.running = true;
				if (log.isInfoEnabled()) {
					log.info("Started FTP server");
				}
			} catch (FtpException e) {
				log.error("Failed to start FTP server", e);
				ftpServerStatus.lastError = e;
				ftpServerStatus.ftpServer = null;
			}
			
			serverStatusMap.put(ftpInterfaceResource.getName(), ftpServerStatus);
		}
		return true;
	}

}
