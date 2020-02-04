package com.hypersocket.vfs;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.hypersocket.repository.CriteriaConfiguration;

public class ParentCriteria implements CriteriaConfiguration {

	private VirtualFile parent;

	public ParentCriteria(VirtualFile parent) {
		this.parent = parent;
	}

	@Override
	public void configure(Criteria criteria) {
		if (parent == null) {
			criteria.add(Restrictions.isNull("parent"));
		} else {
			criteria.add(Restrictions.eq("parent", parent));
		}
	}

}
