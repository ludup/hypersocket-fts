package com.hypersocket.migration;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypersocket.fs.FileResource;
import com.hypersocket.json.JsonMapper;
import com.hypersocket.migration.importer.MigrationImporter;
import com.hypersocket.migration.repository.MigrationRepository;
import com.hypersocket.realm.Realm;
import com.hypersocket.vfs.VirtualFile;

@Component("com.hypersocket.migration.VirtualFileMigrationImporter")
public class VirtualFileMigrationImporter implements MigrationImporter<VirtualFile>{

	@Autowired
	JsonMapper jsonMapper;

	@Autowired
	MigrationRepository migrationRepository;
	    
	@Override
	public void process(VirtualFile entity) {
		/**
		 * If mount is null it returns default mount.
		 * If both are equal it means mount was null.
		 * 
		 * Don't know if it is valid case where both are same ??
		 * 
		 */
		if(entity != null) {
			FileResource mount = entity.getMount();
			FileResource defaultMount = entity.getDefaultMount();
			if(mount != null && mount.equals(defaultMount)) {
				entity.setMount(null);
			}
		}
	}

	@Override
	public void postSave(VirtualFile entity) {
	}

	@Override
	public Class<VirtualFile> getType() {
		return VirtualFile.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processCustomOperationsMap(JsonNode jsonNode, Realm realm) {
		 ObjectMapper mapper = jsonMapper.get();
	     Map<String, Object> customOperationsList = mapper.convertValue(jsonNode, Map.class);
	     
	     List<Map<String, ?>> parentMapping = (List<Map<String, ?>>) customOperationsList.get("parentMapping");
	     
	     if(parentMapping != null) {
	    	 for (Map<String, ?> object : parentMapping) {
	                Long virtualFileLegacyId = getLongValue((Number) object.get("virtualFile"));
	                Long virtualFileParentLegacyId = getLongValue((Number) object.get("virtualFileParent"));

	                VirtualFile virtualFile = getVirtualFile(realm, virtualFileLegacyId);
	                VirtualFile virtualFileParent = getVirtualFile(realm, virtualFileParentLegacyId);

	                virtualFile.setParent(virtualFileParent);
	                
	                migrationRepository.saveOrUpdate(virtualFile);
	            }
	     }
		
	}
	
	private Long getLongValue(Number number) {
        return number.longValue();
    }
	
	private VirtualFile getVirtualFile(Realm realm, Long id) {
		VirtualFile virtualFile = migrationRepository.findEntityByLegacyIdInRealm(VirtualFile.class, id, realm);
        if(virtualFile == null) {
            throw new IllegalStateException(String.format("Virtual File for legacy id %d not found", id));
        }

        return virtualFile;
    }

}
