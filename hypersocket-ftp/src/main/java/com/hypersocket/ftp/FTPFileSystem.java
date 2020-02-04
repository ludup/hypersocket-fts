package com.hypersocket.ftp;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.utils.FileUtils;

public class FTPFileSystem implements FileSystemView {

	static Logger log = LoggerFactory.getLogger(FTPFileSystem.class);

	private FTPSessionUser user;
	private FtpFile workingDir;
	private FTPFileSystemFactory factory;

	public FTPFileSystem(FTPSessionUser user, FTPFileSystemFactory factory)
			throws FtpException {
		if (log.isDebugEnabled()) {
			log.debug("Created FileSystem for " + user.getName());
		}
		this.user = user;
		this.factory = factory;
		this.workingDir = getHomeDirectory();
	}

	public boolean changeWorkingDirectory(String path) throws FtpException {

		if (log.isDebugEnabled()) {
			log.debug("Changing directory to " + path);
		}

		FtpFile tmp = getFile(path);

		if (!tmp.doesExist() || !tmp.isDirectory()) {
			return false;
		}
		workingDir = tmp;

		return true;

	}

	public void dispose() {
		if(log.isInfoEnabled()) {
			log.info("Filesystem is being disposed");
		}
	}

	public FtpFile getFile(String path) throws FtpException {

		if (log.isDebugEnabled()) {
			log.debug("Resolving file " + path);
		}

		if (!path.startsWith("/")) {
			if (path.equals(".") || path.equals("./")) {
				return workingDir;
			}
			if (path.equals("..")) {
				path = FileUtils.getParentPath(workingDir.getAbsolutePath());
			} else {
				path = FileUtils.checkEndsWithSlash(workingDir
						.getAbsolutePath()) + path;
			}
		}

		if(path.equals("/")) {
			return getHomeDirectory();
		}

		return new SessionContextFtpFileAdapter(new FileObjectFile(user.getSession(), factory, path), factory);

	}

	public FtpFile getHomeDirectory() throws FtpException {
		if (log.isDebugEnabled()) {
			log.info("Requesting home directory for "
					+ user.getPrincipal().getPrincipalName());
		}
		return new SessionContextFtpFileAdapter(new FileObjectFile(user.getSession(), factory, "/"), factory);
	}

	public FtpFile getWorkingDirectory() throws FtpException {
		if (log.isDebugEnabled()) {
			log.info("Requesting working directory for "
					+ user.getPrincipal().getPrincipalName());
		}
		return workingDir;
	}

	public boolean isRandomAccessible() throws FtpException {
		return false;
	}

}
