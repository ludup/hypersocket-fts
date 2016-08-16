package com.hypersocket.ftp.interfaces;

import java.util.Collection;

import com.hypersocket.resource.AbstractResourceRepository;

public interface FTPInterfaceResourceRepository extends
		AbstractResourceRepository<FTPInterfaceResource> {

	Collection<FTPInterfaceResource> allInterfaces();
}
