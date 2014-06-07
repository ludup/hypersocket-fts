package com.hypersocket.fs.elfinder;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;

import com.hypersocket.fs.FileResource;

public class FileResourceFsItem implements FsItem {

	FileResource mount;
	FileObject file;
	FileObject mountFile;
	FsVolume volume;
	String path;
	
	public FileResourceFsItem(FsVolume volume, FileObject file, FileResource mount, FileObject mountFile) throws IOException {
		this.volume = volume;
		this.file = file;
		this.mountFile = mountFile;
		this.mount = mount;
		
		String relativePath = mountFile.getName().getRelativeName(file.getName());
		if(!relativePath.equals(".")) {
			path = "/" + mount.getName() + "/" + relativePath;
		} else {
			path = "/" + mount.getName();
		}
	}
	
	public String getPath() {
		return path;
	}
	
	public FileResourceFsItem getParent() throws IOException {
		return new FileResourceFsItem(volume, file.getParent(), mount, mountFile);
	}
	
	public FileResource getMount() {
		return mount;
	}

	
	@Override
	public FsVolume getVolume() {
		return volume;
	}
	
	
	public FileObject getFileObject() {
		return file;
	}

	public FileObject getMountFile() {
		return mountFile;
	}

	public boolean isMountRoot() {
		return file.equals(mountFile);
	}

	public String getFilename() {
		return file.getName().getBaseName();
	}
}
