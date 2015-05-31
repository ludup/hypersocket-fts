package com.hypersocket.ftp;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class MountFile extends AbstractFtpFile {

	boolean singleMount = false;
	public MountFile(Session session, FTPFileSystemFactory fileResourceService, FileResource resource, FileObject file, boolean singleMount) {
		super(session, fileResourceService, resource, file, singleMount ? "/" : "/" + resource.getName(), "/" + resource.getName());
		this.singleMount = singleMount;
	}

	public MountFile(Session session, FTPFileSystemFactory fileResourceService, FileResource resource, FileObject file) {
		this(session, fileResourceService, resource, file, false);
	}
	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public boolean doesExist() {
		return true;
	}
	
	@Override
	public long getLastModified() {
		return resource.getModifiedDate().getTime();
	}

	@Override
	public int getLinkCount() {
		return 0;
	}

	@Override
	public String getName() {
		return resource.getName();
	}

	@Override
	public long getSize() {
		return 0;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return true;
	}

	@Override
	public boolean isRemovable() {
		return false;
	}

	@Override
	public boolean isWritable() {
		return false;
	}
}
