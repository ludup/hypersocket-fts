package com.hypersocket.ftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.session.Session;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.VirtualFile;

public class FileObjectFile extends AbstractFtpFile {

	static Logger log = LoggerFactory.getLogger(FileObjectFile.class);

	VirtualFile virtualFile;
	
	FileObjectFile(Session session, FTPFileSystemFactory factory, String path) {
		super(session, factory, path);
	}
	
	public InputStream createInputStream(long position) throws IOException {
		try {
			return factory.getService().downloadFile(absolutePath, position, FTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			log.error("Failed to create InputStream", e);
			throw new IOException(e);
		}
	}

	public OutputStream createOutputStream(long position) throws IOException {
		
		try {
			return factory.getService().uploadFile(absolutePath, position, FTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			log.error("Failed to create InputStream", e);
			throw new IOException(e);
		}
	}

	public boolean delete() {
		try {
			return factory.getService().deleteFile(absolutePath, FTP_PROTOCOL);
		} catch (IOException e) {
			log.error("Failed to delete file", e);
			return false;
		} catch(AccessDeniedException e) {
			return false;
		}
	}

	public boolean doesExist() {
		
		try {
			factory.getService().getFile(absolutePath);
			return true;
		} catch (IOException e) {
			return false;
		} catch (AccessDeniedException e) {
			return false;
		}
	}

	public long getLastModified() {
		try {
			checkFile();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return file.getLastModified();
	}

	public int getLinkCount() {
		return 0;
	}

	public String getName() {
		return FileUtils.lastPathElement(absolutePath);
	}
	
	public boolean isReadable() {
		return true;
	}

	public boolean isRemovable() {
		try {
			checkFile();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return file.getWritable();
	}

	public boolean isWritable() {
		try {
			checkFile();
			return file.getWritable();
		} catch (IOException e) {
			VirtualFile parentFile;
			try {
				parentFile = factory.getService().getFile(FileUtils.stripLastPathElement(absolutePath));
				return parentFile.getWritable();
			} catch (IOException | AccessDeniedException e1) {
				return false;
			}
			
		}
	}
}
