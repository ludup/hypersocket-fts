package com.hypersocket.vfs;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.hypersocket.repository.CriteriaConfiguration;

public class VirtualFileTypeCriteria implements CriteriaConfiguration {

	VirtualFileType[] type;
	
	VirtualFileTypeCriteria(VirtualFileType... type) {
		this.type = type;
	}
	
	@Override
	public void configure(Criteria criteria) {
		criteria.add(Restrictions.in("type", type));
	}

}
