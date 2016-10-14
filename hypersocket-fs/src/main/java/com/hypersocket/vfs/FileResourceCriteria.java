package com.hypersocket.vfs;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.hypersocket.fs.FileResource;
import com.hypersocket.repository.CriteriaConfiguration;
import com.hypersocket.repository.HibernateUtils;

public class FileResourceCriteria implements CriteriaConfiguration {

	FileResource resource;
	FileResource[] resources;
	
	FileResourceCriteria(FileResource resource) {
		this.resource = resource;
	}
	
	public FileResourceCriteria(FileResource[] resources) {
		this.resources = resources;
	}

	@Override
	public void configure(Criteria criteria) {
		
		criteria.createAlias("folderMounts", "folderMount", Criteria.LEFT_JOIN);
		
		if(resource!=null) {
			criteria.add(Restrictions.or(Restrictions.eq("mount", resource), Restrictions.eq("folderMount.id", resource.getId())));
		} else if(resources!=null && resources.length > 0) {
			criteria.add(Restrictions.or(Restrictions.in("mount", resources), 
					Restrictions.in("folderMount.id", HibernateUtils.getResourceIds(resources))));
		} 
	}

}
