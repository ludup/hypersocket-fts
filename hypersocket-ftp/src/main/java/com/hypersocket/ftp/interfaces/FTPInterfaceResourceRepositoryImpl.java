package com.hypersocket.ftp.interfaces;

import java.util.Collection;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.resource.AbstractResourceRepositoryImpl;

@Repository
public class FTPInterfaceResourceRepositoryImpl extends
		AbstractResourceRepositoryImpl<FTPInterfaceResource> implements
		FTPInterfaceResourceRepository {

	@Override
	protected Class<FTPInterfaceResource> getResourceClass() {
		return FTPInterfaceResource.class;
	}
	
	@Override
	@Transactional(readOnly=true)
	public Collection<FTPInterfaceResource> allInterfaces() {
		return allEntities(FTPInterfaceResource.class);
	}

}
