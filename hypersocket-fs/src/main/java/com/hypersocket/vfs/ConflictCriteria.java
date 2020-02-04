package com.hypersocket.vfs;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.hypersocket.repository.CriteriaConfiguration;

public class ConflictCriteria implements CriteriaConfiguration {

	private boolean conflicted = false;
	
	public ConflictCriteria() {
		
	}
	
	public ConflictCriteria(boolean conflicted) {
		this.conflicted = conflicted;
	}
	
	@Override
	public void configure(Criteria criteria) {
		criteria.add(Restrictions.eq("conflicted", conflicted));
	}

}
