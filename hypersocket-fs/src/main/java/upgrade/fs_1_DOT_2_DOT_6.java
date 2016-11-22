package upgrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceRepository;
import com.hypersocket.vfs.VirtualFileRepository;

public class fs_1_DOT_2_DOT_6 implements Runnable {

	static Logger log = LoggerFactory.getLogger(fs_1_DOT_2_DOT_6.class);
	
	@Autowired
	FileResourceRepository repository;

	@Autowired
	VirtualFileRepository virtualRepository;
	
	public void run() {
	
		/**
		 * Move existing file resources so they end up in a similar structure;
		 */
		for(FileResource resource : repository.allResources()) {
			virtualRepository.clearFileResource(resource);
		}
	}
}
