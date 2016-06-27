package com.hypersocket.ftp.interfaces;

import org.apache.ftpserver.FtpServer;

public class FtpServerStatus {

	boolean running = false;
	FtpServer ftpServer;
	Throwable lastError = null;
	
	public FtpServerStatus() {}
	
	public FtpServerStatus(boolean running, FtpServer ftpServer, Throwable lastError){
		this.running = running;
		this.ftpServer = ftpServer;
		this.lastError = lastError;
	}
	
	public boolean isRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}
	public FtpServer getFtpServer() {
		return ftpServer;
	}
	public void setFtpServer(FtpServer ftpServer) {
		this.ftpServer = ftpServer;
	}
	public Throwable getLastError() {
		return lastError;
	}
	public void setLastError(Throwable lastError) {
		this.lastError = lastError;
	}
	
}
