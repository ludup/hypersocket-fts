package com.hypersocket.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.fs.FileResource;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.session.Session;

public class FileObjectFile extends AbstractFtpFile {

	static Logger log = LoggerFactory.getLogger(FileObjectFile.class);

	
	FileObjectFile(Session session, FTPFileSystemFactory factory, FileResource resource, FileObject file, String path) {
		super(session, factory, resource, file, path);
	}
	
	public InputStream createInputStream(long position) throws IOException {
		try {
			return factory.getFileResourceService().downloadFile(getAbsolutePath(), position);
		} catch (AccessDeniedException e) {
			log.error("Failed to create InputStream", e);
			throw new IOException(e);
		}
	}

	public OutputStream createOutputStream(long position) throws IOException {
		
		try {
			return factory.getFileResourceService().uploadFile(getAbsolutePath(), position);
		} catch (AccessDeniedException e) {
			log.error("Failed to create InputStream", e);
			throw new IOException(e);
		}
	}

	public boolean delete() {
		try {
			return file.delete();
		} catch (FileSystemException e) {
			log.error("Failed to delete file", e);
			return false;
		}
	}

	public boolean doesExist() {
		try {
			return file.exists();
		} catch (FileSystemException e) {
			log.error("Failed to determine if file exists", e);
			return false;
		}
	}

	public long getLastModified() {
		try {
			return file.getContent().getLastModifiedTime();
		} catch (FileSystemException e) {
			log.error("Failed to determine last modified time", e);
			return 0;
		}
	}

	public int getLinkCount() {
		return 0;
	}

	public String getName() {
		return file.getName().getBaseName();
	}
	
	public boolean isReadable() {
		return true;
	}

	public boolean isRemovable() {
		return !resource.isReadOnly();
	}

	public boolean isWritable() {
		return !resource.isReadOnly();
	}
}
