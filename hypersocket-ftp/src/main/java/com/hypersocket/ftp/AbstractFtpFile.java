package com.hypersocket.ftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.session.Session;
import com.hypersocket.vfs.VirtualFile;

public abstract class AbstractFtpFile implements FtpFile {

	public static String FTP_PROTOCOL = "FTP";
	static Logger log = LoggerFactory.getLogger(AbstractFtpFile.class);

	final Session session;
	final FTPFileSystemFactory factory;
	final String absolutePath;
	
	VirtualFile file;
	
	AbstractFtpFile(Session session, FTPFileSystemFactory factory, String absolutePath) {
		this.session = session;
		this.factory = factory;
		this.absolutePath = absolutePath;
	}

	protected void checkFile() {
		
		try {
			if(file==null) {
				file = factory.getService().getFile(absolutePath);
			}
		} catch(IOException e) {
			throw new IllegalStateException(e);
		} catch(AccessDeniedException e) {
			throw new IllegalStateException(e);
		}
	}
	public Session getSession() {
		return session;
	}
	
	@Override
	public InputStream createInputStream(long pos) throws IOException {
		throw new IOException("Stream access not allowed on this type of file");
	}

	@Override
	public OutputStream createOutputStream(long pos) throws IOException {
		throw new IOException("Stream access not allowed on this type of file");
	}

	@Override
	public String getAbsolutePath() {
		return absolutePath;
	}

	@Override
	public List<FtpFile> listFiles() {

		List<FtpFile> ret = new ArrayList<FtpFile>();

		try {
			for(VirtualFile f : factory.getService().getChildren(absolutePath)) {
				ret.add(new SessionContextFtpFileAdapter(
						new FileObjectFile(session, 
						factory, 
						f.getVirtualPath()),
						factory));
			}

		} catch (AccessDeniedException e) {
			log.error("Failed to list files", e);
		} catch (FileNotFoundException e) {
			log.error("Failed to list files", e);
		} 

		return ret;
	}

	@Override
	public boolean mkdir() {

		try {
			try {
				factory.getService().getFile(absolutePath);
				return false;
			} catch(FileNotFoundException e) {
				factory.getService().createFile(absolutePath, FTP_PROTOCOL);
			}
		} catch (FileSystemException e) {
			log.error("Failed to create folder", e);
		} catch (IOException e) {
			log.error("Failed to create folder", e);
		} catch (AccessDeniedException e) {
			log.error("Failed to create folder", e);
		}
		return false;
	}

	@Override
	public boolean move(FtpFile ftpFile) {

		AbstractFtpFile toFile = (AbstractFtpFile) ftpFile;
		try {
			factory.getService().renameFile(absolutePath, toFile.getAbsolutePath(), FTP_PROTOCOL);
			return true;
		} catch (FileSystemException e) {
			log.error("Failed to move file", e);
		} catch (IOException e) {
			log.error("Failed to move file", e);
		} catch (AccessDeniedException e) {
			log.error("Failed to move file", e);
		}
		
		return false;
	}

	@Override
	public boolean setLastModified(long lastModified) {
		try {
			factory.getService().setLastModified(absolutePath, lastModified, FTP_PROTOCOL);
			return true;
		} catch (IOException e) {
			log.error("Failed to set last modified time", e);
		} catch(AccessDeniedException e) {
			log.error("Failed to set last modified time", e);
		}
		return false;
	}

	@Override
	public boolean isDirectory() {
		checkFile();
		return file.isFolder() || file.isMount();
	}

	@Override
	public boolean isFile() {
		checkFile();
		return file.isFile();
	}

	@Override
	public boolean isHidden() {
		checkFile();
		return file.isHidden();
	}

	@Override
	public long getSize() {
		checkFile();
		return file.getSize();
	}

	@Override
	public String getGroupName() {
		return session.getCurrentPrincipal().getRealm().getName();
	}

	@Override
	public String getOwnerName() {
		return session.getCurrentPrincipal().getPrincipalName();
	}

}
