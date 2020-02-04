package com.hypersocket.vfs;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.hypersocket.repository.CriteriaConfiguration;

public class VirtualFileTypeCriteria implements CriteriaConfiguration {

	private VirtualFileType[] type;
	
	public VirtualFileTypeCriteria(VirtualFileType... type) {
		this.type = type;
	}
	
	@Override
	public void configure(Criteria criteria) {
		criteria.add(Restrictions.in("type", type));
	}

}
