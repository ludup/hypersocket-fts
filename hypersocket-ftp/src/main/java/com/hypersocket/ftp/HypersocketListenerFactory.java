package com.hypersocket.ftp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.ftpserver.FtpServerConfigurationException;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;

import com.hypersocket.ftp.interfaces.FTPInterfaceResource;

public class HypersocketListenerFactory extends ListenerFactory {
	
	@SuppressWarnings("deprecation")
	public Listener createListener(FTPInterfaceResource resource) {
		try{
    		InetAddress.getByName(getServerAddress());
    	}catch(UnknownHostException e){
    		throw new FtpServerConfigurationException("Unknown host",e);
    	}
    	//Deal with the old style black list and new IP Filter here. 
		if(getBlockedAddresses() != null || getBlockedSubnets() != null) {
			 throw new IllegalStateException("Usage of blockedAddesses/subnets is not supported. ");
		 }
    	
	    return new HypersocketNioListener(getServerAddress(), getPort(), isImplicitSsl(), getSslConfiguration(),
	        	getDataConnectionConfiguration(), getIdleTimeout(), getIpFilter(), resource);
    	
	}
	
	@Override
	public Listener createListener() {
		throw new UnsupportedOperationException("This operation is not supported, may be you need simple ListenerFactory");
	}

}
