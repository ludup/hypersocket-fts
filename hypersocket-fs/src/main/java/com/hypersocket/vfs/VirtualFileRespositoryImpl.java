package com.hypersocket.vfs;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
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
		return list("parent", parent, VirtualFile.class, new FileResourceCriteria(resources));
	}

	@Override
	@Transactional(readOnly=true)
	public VirtualFile getMountFile(FileResource resource) {
		return get("mount", resource, VirtualFile.class, new VirtualFileTypeCriteria(VirtualFileType.MOUNT));
	}
	
	@Override
	@Transactional(readOnly=true)
	public VirtualFile getVirtualFile(String virtualPath, FileResource... resources) {
		return get(VirtualFile.class, new VirtualPathCriteria(virtualPath), new FileResourceCriteria(resources));
	}
	
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getReconciledFiles(VirtualFile parent) {
		return list("parent", parent, VirtualFile.class);
	}
	
	@Override
	@Transactional(readOnly=true)
	public VirtualFile getReconciledFile(String virtualPath) {
		return get(VirtualFile.class, new VirtualPathCriteria(virtualPath));
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileMount(FileResource resource, FileObject fileObject) throws FileSystemException {
		
		String filename;
		VirtualFile parent = null;
		
		if(resource.getVirtualPath().equals("/")) {
			filename = "__ROOT__";
		} else {
			filename = FileUtils.lastPathElement(resource.getVirtualPath());
			parent = reconcileParent(resource);
		}
		
		return buildFile(new VirtualFile(),
					filename,
					resource.getVirtualPath(), 
					VirtualFileType.MOUNT, 
					!resource.isReadOnly(), 
					0L, 
					fileObject.getContent().getLastModifiedTime(), 
					parent,
					resource,
					resource.getRealm());
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileNewFolder(VirtualFile parent, FileObject fileObject) throws FileSystemException {
		
		String filename = fileObject.getName().getBaseName();
		
		return buildFile(new VirtualFile(),
					filename,
					FileUtils.checkEndsWithSlash(parent.getVirtualPath() + filename) , 
					VirtualFileType.FOLDER, 
					parent.isMounted() && !parent.getMount().isReadOnly(), 
					0L, 
					fileObject.getContent().getLastModifiedTime(), 
					parent,
					parent.getMount(),
					parent.getRealm());
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileFolder(VirtualFile folder, FileObject fileObject) throws FileSystemException {
		
		String filename = fileObject.getName().getBaseName();
		
		return buildFile(folder,
					filename,
					FileUtils.checkEndsWithSlash(folder.getParent().getVirtualPath() + filename) , 
					VirtualFileType.FOLDER, 
					folder.getParent().isMounted() && !folder.getParent().getMount().isReadOnly(), 
					0L, 
					fileObject.getContent().getLastModifiedTime(), 
					folder.getParent(),
					folder.getParent().getMount(),
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
	public Map<String, VirtualFile> reconcileParents(FileResource resource) {
		Map<String,VirtualFile> parents = new HashMap<String,VirtualFile>();
		
		VirtualFile rootFile = get("filename", "__ROOT__", VirtualFile.class, new FileResourceCriteria(resource));
		if(rootFile==null) {
			rootFile = buildFile(new VirtualFile(),
					"__ROOT__",
					"/",
					VirtualFileType.FOLDER,
					false,
					0L,
					System.currentTimeMillis(),
					null,
					null,
					resource.getRealm());
		}
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
						path,
						VirtualFileType.FOLDER,
						false,
						0L,
						System.currentTimeMillis(),
						currentParent,
						null,
						resource.getRealm());
			}
			currentParent = parent;
			parents.put(path, parent);
		}
		
		return parents;
	}
	
	VirtualFile buildFile(VirtualFile file, String filename, String virtualPath, 
			VirtualFileType type, boolean writable,
			Long size, Long lastModified, VirtualFile parent, FileResource resource,
			Realm realm) {
		
		file.setRealm(realm);
		file.setFilename(filename);
		file.setVirtualPath(virtualPath);
		file.setType(type);
		file.setWritable(writable);
		file.setSize(size);
		file.setLastModified(lastModified);
		file.setParent(parent);
		file.setMount(resource);
		file.setHash(VirtualFileUtils.generateHash(filename, 
				virtualPath, 
				type.ordinal(), 
				lastModified, 
				size, 
				writable));
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
	public VirtualFile reconcileFile(FileObject obj, FileResource resource, VirtualFile parent) throws FileSystemException {
		return buildFile(new VirtualFile(), 
				obj.getName().getBaseName(), 
				FileUtils.checkEndsWithSlash(parent.getVirtualPath()) + obj.getName().getBaseName(), 
				VirtualFileType.FILE, 
				!resource.isReadOnly(), 
				obj.getContent().getSize(), 
				obj.getContent().getLastModifiedTime(), 
				parent, 
				resource,
				resource.getRealm());
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileFile(FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent) throws FileSystemException {
		return buildFile(virtual, 
				obj.getName().getBaseName(), 
				FileUtils.checkEndsWithSlash(parent.getVirtualPath()) + obj.getName().getBaseName(), 
				VirtualFileType.FILE, 
				!resource.isReadOnly(), 
				obj.getContent().getSize(), 
				obj.getContent().getLastModifiedTime(), 
				parent, 
				resource,
				resource.getRealm());
	}
	
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> search(String searchColumn, String search, int start, int length, ColumnSort[] sort, VirtualFile parent, FileResource... resources) {
		return super.search(VirtualFile.class, searchColumn, search, start, length, sort, new ParentCriteria(parent), new FileResourceCriteria(resources));
	}

	@Override
	public int removeReconciledFolder(VirtualFile toDelete) {
		
		int filesToDelete = 0;
		for(VirtualFile child : getVirtualFiles(toDelete, toDelete.getMount())) {
			if(child.isFolder()) {
				filesToDelete += removeReconciledFolder(child);
				continue;
			}
			filesToDelete++;
		}
		Query update = createQuery("delete from VirtualFile where parent = :parent", true);
		update.setEntity("parent", toDelete);
		update.executeUpdate();
		
		return filesToDelete;
	}

	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getMounts() {
		
		return list(VirtualFile.class, new CriteriaConfiguration() {
			
			@Override
			public void configure(Criteria criteria) {
				criteria.add(
						Restrictions.or(
						Restrictions.eq("type", VirtualFileType.MOUNT),
						Restrictions.isNull("mount")));
				criteria.addOrder(Order.asc("id"));
			}
		});
	}
}
