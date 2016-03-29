package upgrade;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceRepository;
import com.hypersocket.resource.ResourceChangeException;

public class fs_1_DOT_2_DOT_0 implements Runnable {

	static Logger log = LoggerFactory.getLogger(fs_1_DOT_2_DOT_0.class);
	
	@Autowired
	FileResourceRepository repository;
	
	
	@SuppressWarnings("unchecked")
	public void run() {
	

		for(FileResource resource : repository.allResources()) {
			
			if(StringUtils.isBlank(resource.getVirtualPath())) {
				resource.setVirtualPath("/" + resource.getName());
				try {
					repository.saveResource(resource, new HashMap<String,String>());
				} catch (ResourceChangeException e) {
					log.error(String.format("Could not upgrade file resource %s. Resource requires virtual path value", resource.getName()), e);
				}
			}
		}
	}
}
