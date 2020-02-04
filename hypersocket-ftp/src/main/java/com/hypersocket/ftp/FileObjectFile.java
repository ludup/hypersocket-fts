package com.hypersocket.ftp;

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

	public FileObjectFile(Session session, FTPFileSystemFactory factory, String path) {
		super(session, factory, path);
	}
	
	public InputStream createInputStream(long position) throws IOException {
		try {
			return getFactory().getService().downloadFile(getAbsolutePath(), position, FTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			log.error("Failed to create InputStream", e);
			throw new IOException(e.getMessage(), e);
		}
	}

	public OutputStream createOutputStream(long position) throws IOException {
		
		try {
			return getFactory().getService().uploadFile(getAbsolutePath(), position, FTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			log.error("Failed to create InputStream", e);
			throw new IOException(e.getMessage(), e);
		}
	}

	public boolean delete() {
		try {
			return getFactory().getService().deleteFile(getAbsolutePath(), FTP_PROTOCOL);
		} catch (IOException e) {
			log.error("Failed to delete file", e);
			return false;
		} catch(AccessDeniedException e) {
			return false;
		}
	}

	public boolean doesExist() {
		
		try {
			getFactory().getService().getFile(getAbsolutePath());
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
			throw new IllegalStateException(e.getMessage(), e);
		}
		return file.getLastModified();
	}

	public int getLinkCount() {
		return 0;
	}

	public String getName() {
		return FileUtils.lastPathElement(getAbsolutePath());
	}
	
	public boolean isReadable() {
		return true;
	}

	public boolean isRemovable() {
		try {
			checkFile();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
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
				parentFile = getFactory().getService().getFile(FileUtils.stripLastPathElement(getAbsolutePath()));
				return parentFile.getWritable();
			} catch (IOException | AccessDeniedException e1) {
				return false;
			}
			
		}
	}
}
