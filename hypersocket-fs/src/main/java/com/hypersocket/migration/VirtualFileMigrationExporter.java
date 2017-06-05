package com.hypersocket.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hypersocket.migration.exporter.MigrationExporter;
import com.hypersocket.migration.repository.MigrationRepository;
import com.hypersocket.realm.Realm;
import com.hypersocket.vfs.VirtualFile;

@Component("com.hypersocket.migration.VirtualFileMigrationExporter")
public class VirtualFileMigrationExporter implements MigrationExporter<VirtualFile> {
	
	@Autowired
    MigrationRepository migrationRepository;

	@Override
	public Class<VirtualFile> getType() {
		return VirtualFile.class;
	}

	@Override
	public Map<String, List<Map<String, ?>>> produceCustomOperationsMap(Realm realm) {
		List<VirtualFile> virtualFiles = migrationRepository.findAllResourceInRealmOfType(getType(), realm);
        List<Map<String, ?>> parentMapping = new ArrayList<>();
        
        Map<String, List<Map<String, ?>>> customOperationsList = new HashMap<>();

        customOperationsList.put("parentMapping", parentMapping);
        
		for (VirtualFile virtualFile : virtualFiles) {
			if (virtualFile.getParent() != null) {
				Map<String, Long> parentMappingEntry = new HashMap<>();
				parentMappingEntry.put("virtualFile", virtualFile.getLegacyId());
				parentMappingEntry.put("virtualFileParent", virtualFile.getParent().getLegacyId());
				parentMapping.add(parentMappingEntry);
			}
		}
        
		return customOperationsList;
	}

}
