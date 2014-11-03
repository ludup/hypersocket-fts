package com.hypersocket.ftp;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.fs.FileResource;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.util.FileUtils;

public class FTPFileSystem implements FileSystemView {

	static Logger log = LoggerFactory.getLogger(FTPFileSystem.class);

	FTPSessionUser user;
	FtpFile workingDir;
	FTPFileSystemFactory factory;

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



		try {
			
			// Resolve the file from the working directory
			FileResource mount = factory.getFileResourceService().getMountForPath(path);
			
			FileObject mountFile = factory.getFileResourceService().resolveMountFile(mount);

			String childPath = factory.getFileResourceService()
					.resolveChildPath(mount, path);

			if (childPath.equals("")) {
				return new SessionContextFtpFileAdapter(new MountFile(user.getSession(), factory, mount, mountFile), factory);
			} else {
				FileObject file = mountFile.resolveFile(childPath);
				return new FileObjectFile(user.getSession(), factory, mount, file, "/"
						+ FileUtils.checkEndsWithSlash(mount.getName())
						+ childPath);

			}
		} catch (FileSystemException e) {
			log.error("Failed to resolve file " + path, e);
			throw new FtpException(e);
		} catch (AccessDeniedException e) {
			log.error("Failed to resolve file " + path, e);
			throw new FtpException(e);
		} catch (IOException e) {
			log.error("Failed to resolve file " + path, e);
			throw new FtpException(e);
		}

	}

	public FtpFile getHomeDirectory() throws FtpException {
		if (log.isDebugEnabled()) {
			log.info("Requesting home directory for "
					+ user.getPrincipal().getPrincipalName());
		}
		return new SessionContextFtpFileAdapter(new RootFile("/", user.getSession(), factory), factory, user.getSession());
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
