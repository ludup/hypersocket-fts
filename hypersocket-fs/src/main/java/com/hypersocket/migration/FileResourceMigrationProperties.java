package com.hypersocket.migration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypersocket.fs.FileResource;
import com.hypersocket.migration.lookup.LookUpKey;
import com.hypersocket.migration.properties.MigrationProperties;
import com.hypersocket.migration.repository.MigrationExportCriteriaBuilder;
import com.hypersocket.migration.repository.MigrationLookupCriteriaBuilder;
import com.hypersocket.migration.repository.MigrationRepository;
import com.hypersocket.realm.Realm;
import com.hypersocket.repository.AbstractEntity;
import com.hypersocket.util.SpringApplicationContextProvider;
import com.hypersocket.vfs.VirtualFile;

public class FileResourceMigrationProperties implements MigrationProperties {
	
	private Map<Class<?>, MigrationLookupCriteriaBuilder> lookUpCriteriaMap = new HashMap<>();
	
	public FileResourceMigrationProperties() {
		buildLookUpCriteriaForPasswordResource(VirtualFile.class);
	}

	@Override
	public Short sortOrder() {
		return 9000;
	}

	@Override
	public List<Class<? extends AbstractEntity<Long>>> getOrderList() {
		return Arrays.<Class<? extends AbstractEntity<Long>>>asList(
				FileResource.class,
				VirtualFile.class
		);
	}

	@Override
	public Map<Class<?>, MigrationExportCriteriaBuilder> getExportCriteriaMap() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Class<?>, MigrationLookupCriteriaBuilder> getLookupCriteriaMap() {
		return lookUpCriteriaMap;
	}
	
	private void buildLookUpCriteriaForPasswordResource(final Class<VirtualFile> virtualFile) {
		lookUpCriteriaMap.put(virtualFile, new MigrationLookupCriteriaBuilder() {
					
			@Override
			public DetachedCriteria make(Realm realm, LookUpKey lookUpKey, JsonNode node) {
				Object[] values = lookUpKey.getValues();
				Long legacyId = (Long) values[0];
				String filename = (String) values[1];
				
				MigrationRepository migrationRepository = (MigrationRepository) SpringApplicationContextProvider.
	                    getApplicationContext().getBean("migrationRepository");
	            DetachedCriteria criteria = migrationRepository.buildCriteriaFor(virtualFile, "vf");
	            criteria.createAlias("vf.realm", "realm");
	            if(!"__ROOT__".equals(filename)) {
	            	criteria.add(Restrictions.eq("vf.legacyId", legacyId));
	            }
	            criteria.add(Restrictions.eq("vf.filename", filename));
	            criteria.add(Restrictions.eq("realm.id", realm.getId()));
	            return criteria;
			}
		});
		
	}

}
