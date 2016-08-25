package com.hypersocket.ftp;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.ipfilter.IpFilter;
import org.apache.ftpserver.listener.nio.NioListener;
import org.apache.ftpserver.ssl.SslConfiguration;

import com.hypersocket.ftp.interfaces.FTPInterfaceResource;

public class HypersocketNioListener extends NioListener {
	
	private FTPInterfaceResource resource;

	public HypersocketNioListener(String serverAddress, int port, boolean implicitSsl,
			SslConfiguration sslConfiguration, DataConnectionConfiguration dataConnectionConfig, int idleTimeout,
			IpFilter ipFilter, FTPInterfaceResource resource) {
		super(serverAddress, port, implicitSsl, sslConfiguration, dataConnectionConfig, idleTimeout, ipFilter);
		this.resource = resource;
	}

	public FTPInterfaceResource getFTPInterfaceResource(){
		return this.resource;
	}
	
	@Override
	public synchronized void stop() {
		super.stop();
		this.resource = null;
	}
}
