package com.hypersocket.ftp;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;

public class SessionContextFileSystemViewAdapter implements FileSystemView {

	private FileSystemView view;
	private FTPSessionUser user;
	private FTPFileSystemFactory factory;

	public SessionContextFileSystemViewAdapter(FTPSessionUser user,
			FileSystemView view, FTPFileSystemFactory factory) {
		this.user = user;
		this.view = view;
		this.factory = factory;
	}
	@Override
	public boolean changeWorkingDirectory(String path) throws FtpException {

		factory.setupSessionContext(user.getSession());
		try {
			return view.changeWorkingDirectory(path);
		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public FtpFile getFile(String path) throws FtpException {
		
		factory.setupSessionContext(user.getSession());
		try {
			return view.getFile(path);
		} finally {
			factory.clearSessionContext();
		}
		
	}

	@Override
	public FtpFile getHomeDirectory() throws FtpException {
		
		factory.setupSessionContext(user.getSession());
		try {
			return view.getHomeDirectory();
		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public FtpFile getWorkingDirectory() throws FtpException {
		
		factory.setupSessionContext(user.getSession());
		try {
			return view.getWorkingDirectory();
		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public boolean isRandomAccessible() throws FtpException {
		
		factory.setupSessionContext(user.getSession());
		try {
			return view.isRandomAccessible();
		} finally {
			factory.clearSessionContext();
		}
	}

}
