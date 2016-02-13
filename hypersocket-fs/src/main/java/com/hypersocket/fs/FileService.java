package com.hypersocket.fs;

import org.apache.commons.vfs2.FileSystemOptions;

import com.hypersocket.resource.AbstractResourceRepository;

public interface FileService {

	FileSystemOptions buildFileSystemOptions(FileResource resource);
	
	AbstractResourceRepository<FileResource> getRepository();
}
