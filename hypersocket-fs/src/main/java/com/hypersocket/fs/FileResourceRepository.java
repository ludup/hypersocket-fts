package com.hypersocket.fs;

import java.util.Collection;

import com.hypersocket.resource.AbstractAssignableResourceRepository;

public interface FileResourceRepository extends AbstractAssignableResourceRepository<FileResource> {

	Collection<FileResource> getResourcesByVirtualPath(String virtualPath);

}
