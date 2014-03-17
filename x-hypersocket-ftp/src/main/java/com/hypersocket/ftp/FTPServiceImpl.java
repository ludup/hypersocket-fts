package com.hypersocket.ftp;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.hypersocket.fs.FileResourceService;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.server.events.HypersocketServerEvent;
import com.hypersocket.server.events.ServerStartedEvent;
import com.hypersocket.server.events.ServerStoppingEvent;

@Service
public class FTPServiceImpl implements FTPService, ApplicationListener<HypersocketServerEvent> {

	static Logger log = LoggerFactory.getLogger(FTPServiceImpl.class);
	
	@Autowired
	FTPUserManager userManager;
	
	@Autowired
	FTPFileSystemFactory filesystemFactory;
	
	FtpServer server;
	public void onApplicationEvent(HypersocketServerEvent event) {
		
		if(event instanceof ServerStartedEvent) {
			
			if(log.isInfoEnabled()) {
				log.info("Starting FTP server");
			}
			FtpServerFactory serverFactory = new FtpServerFactory();

			ListenerFactory factory = new ListenerFactory();
	
			// set the port of the listener
			factory.setPort(2221);

			// replace the default listener
			serverFactory.addListener("default", factory.createListener());
			
			// start the server

			serverFactory.setUserManager(userManager);
			serverFactory.setFileSystem(filesystemFactory);
			
			server = serverFactory.createServer();         
			
			try {
				server.start();
				
				if(log.isInfoEnabled()) {
					log.info("Started FTP server");
				}
			} catch (FtpException e) {
				log.error("Failed to start FTP server", e);
				server = null;
			}
		
		} else if(event instanceof ServerStoppingEvent) {
			
			if(server!=null) {
				
				if(log.isInfoEnabled()) {
					log.info("Stopping FTP server");
				}
				server.stop();
			}
		}
		
	}


	
	
	

}
