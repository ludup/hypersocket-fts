package com.hypersocket.fs.tasks;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import com.hypersocket.properties.ResourceTemplateRepositoryImpl;

@Repository
public class CopyFileTaskRepositoryImpl extends ResourceTemplateRepositoryImpl
		implements CopyFileTaskRepository {

	@PostConstruct
	private void postConstruct() {
		loadPropertyTemplates("tasks/copyFile.xml");
	}
}
