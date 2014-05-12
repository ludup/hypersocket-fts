package com.hypersocket.fs;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.resource.AbstractAssignableResourceRepositoryImpl;

@Repository
@Transactional
public class FileResourceRepositoryImpl extends
		AbstractAssignableResourceRepositoryImpl<FileResource> implements
		FileResourceRepository {

	@Override
	protected Class<FileResource> getResourceClass() {
		return FileResource.class;
	}

}
