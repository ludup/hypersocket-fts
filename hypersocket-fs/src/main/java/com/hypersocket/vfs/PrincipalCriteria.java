package com.hypersocket.vfs;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.hypersocket.realm.Principal;
import com.hypersocket.repository.CriteriaConfiguration;

public class PrincipalCriteria implements CriteriaConfiguration {

	private Principal principal;

	public PrincipalCriteria(Principal principal) {
		this.principal = principal;
	}

	@Override
	public void configure(Criteria criteria) {
		if (principal == null) {
			criteria.add(Restrictions.isNull("principal"));
		} else {
			criteria.add(Restrictions.or(Restrictions.eq("principal", principal), Restrictions.isNull("principal")));
		}
	}

}
