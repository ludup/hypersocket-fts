package com.hypersocket.fs.tasks;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import com.hypersocket.properties.ResourceTemplateRepositoryImpl;

@Repository
public class DeleteFolderTaskRepositoryImpl extends ResourceTemplateRepositoryImpl
		implements DeleteFolderTaskRepository {

	@PostConstruct
	private void postConstruct() {
		loadPropertyTemplates("tasks/deleteFolder.xml");
	}
}
