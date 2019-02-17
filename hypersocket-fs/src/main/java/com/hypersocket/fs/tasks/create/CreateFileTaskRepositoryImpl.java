package com.hypersocket.fs.tasks.create;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import com.hypersocket.properties.ResourceTemplateRepositoryImpl;

@Repository
public class CreateFileTaskRepositoryImpl extends ResourceTemplateRepositoryImpl
		implements CreateFileTaskRepository {

	@PostConstruct
	private void postConstruct() {
		loadPropertyTemplates("tasks/createFile.xml");
	}
}
