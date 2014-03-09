package com.hypersocket.fs;

import org.springframework.stereotype.Repository;

import com.hypersocket.resource.AbstractAssignableResourceRepositoryImpl;

@Repository
public class FileResourceRepositoryImpl extends
		AbstractAssignableResourceRepositoryImpl<FileResource> implements
		FileResourceRepository {

	@Override
	protected Class<FileResource> getResourceClass() {
		return FileResource.class;
	}

}
