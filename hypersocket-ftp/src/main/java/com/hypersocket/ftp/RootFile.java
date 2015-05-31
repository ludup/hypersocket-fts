package com.hypersocket.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.fs.FileResource;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.session.Session;

public class RootFile implements FtpFile {

	static Logger log = LoggerFactory.getLogger(RootFile.class);
	String path;
	long lastModified = System.currentTimeMillis();
	Session session;
	FTPFileSystemFactory factory;

	RootFile(String path, Session session,
			FTPFileSystemFactory factory) {
		this.path = path;
		this.session = session;
		this.factory = factory;
	}

	public InputStream createInputStream(long arg0) throws IOException {
		throw new IOException("Stream access not allowed on this type of file");
	}

	public OutputStream createOutputStream(long arg0) throws IOException {
		throw new IOException("Stream access not allowed on this type of file");
	}

	public boolean delete() {
		if (log.isDebugEnabled()) {
			log.debug("Attempt to delete root file");
		}
		return false;
	}

	public boolean doesExist() {
		return true;
	}

	public String getAbsolutePath() {
		return path;
	}

	public String getGroupName() {
		return session.getCurrentPrincipal().getPrincipalName();
	}

	public long getLastModified() {
		return lastModified;
	}

	public int getLinkCount() {
		return 0;
	}

	public String getName() {
		return "/";
	}

	public String getOwnerName() {
		return session.getCurrentPrincipal().getPrincipalName();
	}

	public long getSize() {
		return 0;
	}

	public boolean isDirectory() {
		return true;
	}

	public boolean isFile() {
		return false;
	}

	public boolean isHidden() {
		return false;
	}

	public boolean isReadable() {
		return true;
	}

	public boolean isRemovable() {
		return false;
	}

	public boolean isWritable() {
		return false;
	}

	public List<FtpFile> listFiles() {
		List<FtpFile> ret = new ArrayList<FtpFile>();
		try {
			for (FileResource resource : factory.getFileResourceService()
					.getResources(session.getCurrentPrincipal())) {
				try {
					ret.add(new MountFile(session, factory, resource, factory.getFileResourceService().resolveMountFile(resource)));
				} catch (IOException e) {
					log.error("Failed to resolve mount file", e);
				}
			}
		} catch (AccessDeniedException e) {
			log.error(
					"Caught access denied exception whilst attempting to read users mounts",
					e);
		} 
		return ret;
	}

	public boolean mkdir() {
		return false;
	}

	public boolean move(FtpFile arg0) {
		return false;
	}

	public boolean setLastModified(long arg0) {
		return false;
	}

}
