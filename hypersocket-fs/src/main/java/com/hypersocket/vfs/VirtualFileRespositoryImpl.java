package com.hypersocket.vfs;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Realm;
import com.hypersocket.repository.AbstractRepositoryImpl;
import com.hypersocket.repository.CriteriaConfiguration;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.utils.FileUtils;

@Repository
public class VirtualFileRespositoryImpl extends AbstractRepositoryImpl<Long> implements VirtualFileRepository {
	
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getVirtualFiles(VirtualFile parent, FileResource... resources) {
		return list("parent", parent, VirtualFile.class, new FileResourceCriteria(resources), new ConflictCriteria());
	}
	
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getVirtualFiles(VirtualFile parent) {
		return list("parent", parent, VirtualFile.class, new ConflictCriteria());
	}
	
	@Override
	@Transactional(readOnly=true)
	public VirtualFile getVirtualFileByResource(String virtualPath, FileResource... resources) {
		return get(VirtualFile.class, new VirtualPathCriteria(virtualPath), new FileResourceCriteria(resources), new ConflictCriteria());
	}
	
	@Override
	@Transactional(readOnly=true)
	public VirtualFile getVirtualFile(String virtualPath) {
		return get(VirtualFile.class, new VirtualPathCriteria(virtualPath), new ConflictCriteria());
	}
		
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getReconciledFiles(VirtualFile parent) {
		return list("parent", parent, VirtualFile.class);
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileMount(String displayName, FileResource resource, 
			FileObject fileObject, VirtualFile virtualFile) throws FileSystemException {
		
		String filename;
		VirtualFile parent = null;
		
		if(virtualFile==null) {
			virtualFile = new VirtualFile();
		}
		
		if(resource.getVirtualPath().equals("/") && fileObject.getType()!=FileType.FILE) {
			return getRootFolder(resource.getRealm());
		} else {
			filename = FileUtils.lastPathElement(resource.getVirtualPath());
			parent = reconcileParent(resource);
			
			return buildFile(virtualFile,
					filename,
					displayName,
					resource.getVirtualPath(), 
					fileObject.getType()==FileType.FILE ? VirtualFileType.MOUNTED_FILE : VirtualFileType.MOUNTED_FOLDER, 
					!resource.isReadOnly(), 
					fileObject.getType()==FileType.FILE ? fileObject.getContent().getSize() : 0L, 
					fileObject.getContent().getLastModifiedTime(), 
					parent,
					resource,
					!displayName.equals(filename),
					resource.getRealm());
		}
		
		
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileNewFolder(String displayName, VirtualFile parent, FileObject fileObject, 
			FileResource resource, boolean conflicted) throws FileSystemException {
		
		String filename = fileObject.getName().getBaseName();
		
		return buildFile(new VirtualFile(),
					filename,
					displayName,
					FileUtils.checkEndsWithSlash(parent.getVirtualPath() + filename) , 
					VirtualFileType.FOLDER, 
					!resource.isReadOnly(), 
					0L, 
					fileObject.getContent().getLastModifiedTime(), 
					parent,
					resource,
					conflicted,
					parent.getRealm());
	}
	
	@Override
	@Transactional
	public VirtualFile createVirtualFolder(String displayName, VirtualFile parent) {
		
		return buildFile(new VirtualFile(),
				displayName,
					displayName,
					FileUtils.checkEndsWithSlash(parent.getVirtualPath() + displayName) , 
					VirtualFileType.FOLDER, 
					false, 
					0L, 
					System.currentTimeMillis(), 
					parent,
					null,
					false,
					parent.getRealm());
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileFolder(String displayName, VirtualFile folder, FileObject fileObject, 
			FileResource resource, boolean conflicted) throws FileSystemException {
		
		String filename = fileObject.getName().getBaseName();
		
		return buildFile(folder,
					filename,
					displayName,
					FileUtils.checkEndsWithSlash(folder.getParent().getVirtualPath() + filename) , 
					VirtualFileType.FOLDER, 
					!resource.isReadOnly(), 
					0L, 
					fileObject.getContent().getLastModifiedTime(), 
					folder.getParent(),
					resource,
					conflicted,
					folder.getRealm());
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileParent(FileResource resource) {
		Map<String,VirtualFile> parents = reconcileParents(resource);
		return parents.get(FileUtils.checkEndsWithSlash(
				FileUtils.stripLastPathElement(resource.getVirtualPath())));
	}
	
	@Override
	@Transactional
	public VirtualFile getRootFolder(Realm realm) {
		VirtualFile rootFile = get("filename", "__ROOT__", VirtualFile.class);
		if(rootFile==null) {
			rootFile = buildFile(new VirtualFile(),
					"__ROOT__",
					"/",
					"/",
					VirtualFileType.ROOT,
					false,
					0L,
					System.currentTimeMillis(),
					null,
					null,
					false,
					realm);
		}
		return rootFile;
	}
	
	@Override
	@Transactional
	public Map<String, VirtualFile> reconcileParents(FileResource resource) {
		Map<String,VirtualFile> parents = new HashMap<String,VirtualFile>();
		
		VirtualFile rootFile = getRootFolder(resource.getRealm());
		parents.put("/", rootFile);
		VirtualFile currentParent = rootFile;
		
		List<String> parentPaths = FileUtils.generatePaths(FileUtils.stripLastPathElement(resource.getVirtualPath()));
		for(String path : parentPaths) {
			path = FileUtils.checkEndsWithSlash(path);
			VirtualFile parent = get("virtualPath", path, VirtualFile.class);
			if(parent==null) {
				parent = buildFile(
						new VirtualFile(),
						FileUtils.stripPath(path),
						FileUtils.stripPath(path),
						path,
						VirtualFileType.FOLDER,
						false,
						0L,
						System.currentTimeMillis(),
						currentParent,
						null,
						false,
						resource.getRealm());
			}
			currentParent = parent;
			parents.put(path, parent);
		}
		
		return parents;
	}
	
	VirtualFile buildFile(VirtualFile file, String filename, String displayName, String virtualPath, 
			VirtualFileType type, boolean writable,
			Long size, Long lastModified, VirtualFile parent, 
			FileResource resource, boolean conflicted,
			Realm realm) {
		
		file.setRealm(realm);
		file.setFilename(filename);
		file.setDisplayName(displayName);
		file.setVirtualPath(virtualPath);
		file.setType(type);
		file.setWritable(writable);
		file.setSize(size);
		file.setLastModified(lastModified);
		file.setParent(parent);
		file.setMount(resource);
		file.setConflicted(conflicted);
		file.setHash(VirtualFileUtils.generateHash(filename, 
				virtualPath, 
				type.ordinal(), 
				lastModified, 
				size, 
				writable,
				conflicted));
		save(file);
		return file;
	}

	@Override
	@Transactional
	public void removeReconciledFile(VirtualFile toDelete) {
		delete(toDelete);
	}

	@Override
	@Transactional
	public VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile parent) throws FileSystemException {
		return buildFile(new VirtualFile(), 
				obj.getName().getBaseName(), 
				displayName,
				FileUtils.checkEndsWithSlash(parent.getVirtualPath()) + obj.getName().getBaseName(), 
				VirtualFileType.FILE, 
				!resource.isReadOnly(), 
				obj.getContent().getSize(), 
				obj.getContent().getLastModifiedTime(), 
				parent, 
				resource,
				!displayName.equals(obj.getName().getBaseName()),
				resource.getRealm());
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent) throws FileSystemException {
		return buildFile(virtual, 
				obj.getName().getBaseName(),
				displayName,
				FileUtils.checkEndsWithSlash(parent.getVirtualPath()) + obj.getName().getBaseName(), 
				VirtualFileType.FILE, 
				!resource.isReadOnly(), 
				obj.getContent().getSize(), 
				obj.getContent().getLastModifiedTime(), 
				parent, 
				resource,
				!displayName.equals(obj.getName().getBaseName()),
				resource.getRealm());
	}
	
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> search(String searchColumn, String search, int start, int length, ColumnSort[] sort, VirtualFile parent, FileResource... resources) {
		return super.search(VirtualFile.class, searchColumn, search, start, length, sort, new ParentCriteria(parent), new FileResourceCriteria(resources), new ConflictCriteria());
	}

	@Override
	@Transactional
	public int removeReconciledFolder(VirtualFile toDelete, boolean topLevel) {
		
		int filesToDelete = 0;
		boolean hasFiles =  false;
		for(VirtualFile child : getVirtualFiles(toDelete, toDelete.getMount())) {
			if(child.isFolder()) {
				filesToDelete += removeReconciledFolder(child, false);
				continue;
			}
			hasFiles = true;
		}
		
		if(hasFiles) {
			filesToDelete += removeReconciledFiles(toDelete);
		}
		
		if(topLevel) {
			delete(toDelete);	
		}
		
		flush();
		return filesToDelete;
	}
	
	@Override
	@Transactional
	public int removeReconciledFiles(VirtualFile folder) {
		
		Query update = createQuery("delete from VirtualFile where parent = :parent and mount = :mount", true);
		update.setEntity("parent", folder);
		update.setEntity("mount", folder.getMount());
		return update.executeUpdate();
	}
	
	@Override
	@Transactional
	public void removeFileResource(FileResource resource) {

		Query update = createQuery("update VirtualFile set defaultMount = null where defaultMount = :mount", true);
		update.setEntity("mount", resource);
		update.executeUpdate();
		flush();
		
		update = createQuery("update VirtualFile set parent = :parent where mount = :mount", true);
		update.setParameter("parent", null);
		update.setEntity("mount", resource);
		update.executeUpdate();
		flush();
		
		update = createQuery("delete from VirtualFile where mount = :mount", true);
		update.setEntity("mount", resource);
		update.executeUpdate();
		flush();
	}

	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getVirtualFolders() {
		
		return list(VirtualFile.class, new CriteriaConfiguration() {
			
			@Override
			public void configure(Criteria criteria) {
				criteria.add(
						Restrictions.and(
						Restrictions.in("type", new VirtualFileType[] { VirtualFileType.FOLDER, VirtualFileType.ROOT }),
						Restrictions.isNull("mount")));
				criteria.addOrder(Order.asc("id"));
			}
		});
	}

	@Override
	@Transactional
	public VirtualFile renameVirtualFolder(VirtualFile fromFolder, String toFolder) {
		
		String newName = FileUtils.lastPathElement(toFolder);
		return buildFile(fromFolder,
					newName,
					newName,
					FileUtils.checkEndsWithSlash(toFolder) , 
					VirtualFileType.FOLDER, 
					true, 
					0L, 
					System.currentTimeMillis(), 
					fromFolder.getParent(),
					null,
					false,
					fromFolder.getRealm());
	}

	@Override
	@Transactional(readOnly=true)
	public VirtualFile getVirtualFileById(Long id) {
		return get("id", id, VirtualFile.class);
	}

	@Override
	@Transactional
	public void saveFile(VirtualFile file) {
		save(file);
	}
}
