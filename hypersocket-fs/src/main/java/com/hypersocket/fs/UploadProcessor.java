package com.hypersocket.fs;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

public interface UploadProcessor<T> {

	void processUpload(FileResource resource, FileObject mountFile,
			String childPath, FileObject file) throws FileSystemException;
	
	T getResult();
}
