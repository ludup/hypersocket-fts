package com.hypersocket.vfs;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.hypersocket.repository.CriteriaConfiguration;
import com.hypersocket.utils.FileUtils;

public class VirtualPathCriteria implements CriteriaConfiguration {

	String virtualPath;

	public VirtualPathCriteria(String virtualPath) {
		this.virtualPath = virtualPath;
	}
	
	VirtualPathCriteria(String virtualPath, String column) {
		this.virtualPath = virtualPath;
	}
	
	@Override
	public void configure(Criteria criteria) {
		criteria.add(Restrictions.or(Restrictions.eq("virtualPath", FileUtils.checkEndsWithNoSlash(virtualPath)), 
				Restrictions.eq("virtualPath", FileUtils.checkEndsWithSlash(virtualPath))));

	}

}
