package com.hypersocket.fs;

import java.util.Collection;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.resource.AbstractAssignableResourceRepositoryImpl;

@Repository
public class FileResourceRepositoryImpl extends
		AbstractAssignableResourceRepositoryImpl<FileResource> implements
		FileResourceRepository {

	@Override
	protected Class<FileResource> getResourceClass() {
		return FileResource.class;
	}

	@Override
	@Transactional(readOnly=true)
	public Collection<FileResource> getResourcesByVirtualPath(String virtualPath) {
		return list("virtualPath", virtualPath, FileResource.class);
	}

}
