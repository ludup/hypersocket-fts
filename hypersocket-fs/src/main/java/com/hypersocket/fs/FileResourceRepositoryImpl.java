package com.hypersocket.fs;

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmRestriction;
import com.hypersocket.repository.CriteriaConfiguration;
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
	public Collection<FileResource> getResourcesByVirtualPath(String virtualPath, Realm realm) {
		return list("virtualPath", virtualPath, FileResource.class, new RealmRestriction(realm));
	}

	@Override
	@Transactional(readOnly=true)
	public Collection<FileResource> getNonRootResources(Realm realm) {
		return list(FileResource.class, new RealmRestriction(realm), new CriteriaConfiguration() {
			
			@Override
			public void configure(Criteria criteria) {
				criteria.add(Restrictions.not(Restrictions.eq("virtualPath", "/")));
			}
		});
	}

}
