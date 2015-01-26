package com.hypersocket.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.fs.FileResource;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.session.Session;
import com.hypersocket.util.FileUtils;

public abstract class AbstractFtpFile implements FtpFile {

	public static String FTP_PROTOCOL = "FTP";
	static Logger log = LoggerFactory.getLogger(AbstractFtpFile.class);

	final Session session;
	final FTPFileSystemFactory factory;
	final FileResource resource;
	final FileObject file;
	final String absolutePath;

	AbstractFtpFile(Session session, FTPFileSystemFactory factory,
			FileResource resource, FileObject file, String absolutePath) {
		this.session = session;
		this.factory = factory;
		this.resource = resource;
		this.file = file;
		this.absolutePath = FileUtils.checkEndsWithNoSlash(absolutePath);
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
			for (FileObject f : file.getChildren()) {
				ret.add(new SessionContextFtpFileAdapter(new FileObjectFile(session, factory, resource, f,
						FileUtils.checkEndsWithSlash(absolutePath)
								+ f.getName().getBaseName()), factory));
			}
		} catch (FileSystemException e) {
			log.error("Failed to list files", e);
		}

		return ret;
	}

	@Override
	public boolean mkdir() {

		try {
			if (file.exists()) {
				return false;
			}
			factory.getFileResourceService().createFolder(FileUtils.getParentPath(getAbsolutePath()), 
					FileUtils.lastPathElement(getAbsolutePath()), FTP_PROTOCOL);
			
			file.createFolder();
			return true;
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
		if (ftpFile instanceof RootFile) {
			if (log.isDebugEnabled()) {
				log.debug("Invalid attempt to move a file into the root virtual file");
			}
			return false;
		} else if (ftpFile instanceof AbstractFtpFile) {
			AbstractFtpFile toFile = (AbstractFtpFile) ftpFile;
			try {
				file.moveTo(toFile.getFileObject());
				return true;
			} catch (FileSystemException e) {
				log.error("Failed to move file", e);
			}
		}
		return false;
	}

	public FileObject getFileObject() {
		return file;
	}

	@Override
	public boolean setLastModified(long lastModified) {
		try {
			file.getContent().setLastModifiedTime(lastModified);
			return true;
		} catch (FileSystemException e) {
			log.error("Failed to set last modified time", e);
		}
		return false;
	}

	@Override
	public boolean isDirectory() {
		try {
			return file.getType() == FileType.FOLDER
					|| file.getType() == FileType.FILE_OR_FOLDER;
		} catch (FileSystemException e) {
			log.error("Failed to determine file type", e);
			return false;
		}
	}

	@Override
	public boolean isFile() {
		try {
			return file.getType() == FileType.FILE
					|| file.getType() == FileType.IMAGINARY;
		} catch (FileSystemException e) {
			log.error("Failed to determine file type", e);
			return false;
		}
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public long getSize() {
		try {
			return file.getContent().getSize();
		} catch (FileSystemException e) {
			log.error("Failed to determin file size", e);
			return 0;
		}

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
