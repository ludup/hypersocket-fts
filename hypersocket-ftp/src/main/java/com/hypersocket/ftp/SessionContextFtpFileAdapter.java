package com.hypersocket.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;

import com.hypersocket.session.Session;

public class SessionContextFtpFileAdapter implements FtpFile {

	private FtpFile file;
	private Session session;
	private FTPFileSystemFactory factory;
	
	public SessionContextFtpFileAdapter(AbstractFtpFile file, FTPFileSystemFactory factory) {
		this.file = file;
		this.session = file.getSession();
		this.factory = factory;
	}
	
	public SessionContextFtpFileAdapter(FtpFile file, FTPFileSystemFactory factory, Session session) {
		this.file = file;
		this.factory = factory;
		this.session = session;
	}

	@Override
	public InputStream createInputStream(long startPosition) throws IOException {
		factory.setupSessionContext(session);

		try {
			return file.createInputStream(startPosition);

		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public OutputStream createOutputStream(long startPosition) throws IOException {
		
		factory.setupSessionContext(session);

		try {
			return file.createOutputStream(startPosition);

		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public boolean delete() {
		
		factory.setupSessionContext(session);

		try {
			return file.delete();

		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public boolean doesExist() {
		return file.doesExist();
	}

	@Override
	public String getAbsolutePath() {
		return file.getAbsolutePath();
	}

	@Override
	public String getGroupName() {
		return file.getGroupName();
	}

	@Override
	public long getLastModified() {
		return file.getLastModified();
	}

	@Override
	public int getLinkCount() {
		return file.getLinkCount();
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public String getOwnerName() {
		return file.getOwnerName();
	}

	@Override
	public long getSize() {
		return file.getSize();
	}

	@Override
	public boolean isDirectory() {
		return file.isDirectory();
	}

	@Override
	public boolean isFile() {
		return file.isFile();
	}

	@Override
	public boolean isHidden() {
		return file.isHidden();
	}

	@Override
	public boolean isReadable() {
		return file.isReadable();
	}

	@Override
	public boolean isRemovable() {
		return file.isRemovable();
	}

	@Override
	public boolean isWritable() {
		return file.isWritable();
	}

	@Override
	public List<FtpFile> listFiles() {
		factory.setupSessionContext(session);

		try {
			return file.listFiles();

		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public boolean mkdir() {
		
		factory.setupSessionContext(session);

		try {
			return file.mkdir();

		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public boolean move(FtpFile path) {
		factory.setupSessionContext(session);

		try {
			return file.move(path);

		} finally {
			factory.clearSessionContext();
		}
	}

	@Override
	public boolean setLastModified(long lastModified) {
		factory.setupSessionContext(session);

		try {
			return file.setLastModified(lastModified);
		} finally {
			factory.clearSessionContext();
		}
	}

}
