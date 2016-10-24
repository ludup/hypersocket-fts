package com.hypersocket.vfs.json;

import com.hypersocket.vfs.VirtualFile;

public class VirtualFileWrapper {

	VirtualFile file;
	boolean rootWritable;
	public VirtualFileWrapper(VirtualFile file, boolean rootWritable) {
		this.file = file;
		this.rootWritable = rootWritable;
	}
	public VirtualFile getFile() {
		return file;
	}
	public boolean isRootWritable() {
		return rootWritable;
	}

	
}
