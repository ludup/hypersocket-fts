package com.hypersocket.fs;

import java.io.IOException;

import org.apache.commons.vfs2.FileSystemOptions;

import com.hypersocket.resource.AbstractResourceRepository;

public interface FileService {

	FileSystemOptions buildFileSystemOptions(FileResource resource) throws IOException;
	
	AbstractResourceRepository<FileResource> getRepository();
}
