package upgrade;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceRepository;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmRepository;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.vfs.VirtualFile;
import com.hypersocket.vfs.VirtualFileRepository;

public class fs_1_DOT_2_DOT_0 implements Runnable {

	static Logger log = LoggerFactory.getLogger(fs_1_DOT_2_DOT_0.class);
	
	@Autowired
	FileResourceRepository repository;
	
	@Autowired
	RealmRepository realmRepository;
	
	@Autowired
	VirtualFileRepository virtualRepository;
	
	@SuppressWarnings("unchecked")
	public void run() {
	
		/**
		 * Ensure we have a root folder.
		 */
		for(Realm realm : realmRepository.allRealms()) {
			virtualRepository.getRootFolder(realm);
		}
		
		/**
		 * Move existing file resources so they end up in a similar structure;
		 */
		for(FileResource resource : repository.allResources()) {
			
			if(StringUtils.isBlank(resource.getVirtualPath())) {
				VirtualFile vFolder = virtualRepository.createVirtualFolder(resource.getName(), 
						virtualRepository.getRootFolder(resource.getRealm()));
				vFolder.setDefaultMount(resource);				
				virtualRepository.saveFile(vFolder);
				try {
					resource.setVirtualPath("/" + resource.getName());
					repository.saveResource(resource, new HashMap<String,String>());
				} catch (ResourceChangeException e) {
					log.error(String.format("Could not upgrade file resource %s. Resource requires virtual path value", resource.getName()), e);
				}
			}
		}
	}
}
