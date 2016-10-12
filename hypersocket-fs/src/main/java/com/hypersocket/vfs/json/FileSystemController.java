package com.hypersocket.vfs.json;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.hypersocket.auth.json.AuthenticationRequired;
import com.hypersocket.auth.json.ResourceController;
import com.hypersocket.auth.json.UnauthorizedException;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.fs.FileResourceServiceImpl;
import com.hypersocket.i18n.I18N;
import com.hypersocket.json.RequestStatus;
import com.hypersocket.json.ResourceList;
import com.hypersocket.json.ResourceStatus;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.resource.ResourceException;
import com.hypersocket.server.HypersocketServer;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.session.json.SessionUtils;
import com.hypersocket.tables.BootstrapTableResourceProcessor;
import com.hypersocket.tables.BootstrapTableResult;
import com.hypersocket.tables.Column;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.upload.FileUploadService;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.VirtualFile;
import com.hypersocket.vfs.VirtualFileService;
import com.hypersocket.vfs.VirtualFileType;

@Controller
public class FileSystemController extends ResourceController {

	static Logger log = LoggerFactory.getLogger(FileSystemController.class);
	
	public static final String HTTP_PROTOCOL = "HTTP";
	public static final String CONTENT_INPUTSTREAM = "ContentInputStream";
	
	@Autowired
	SessionUtils sessionUtils;
	
	@Autowired
	FileUploadService fileUploadService; 
	
	@Autowired
	VirtualFileService fileService; 
	
	@Autowired
	FileResourceService fileResourceService; 
	
	@Autowired
	HypersocketServer server;
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/mounts", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<VirtualFile> getMounts(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			return new ResourceList<VirtualFile>(fileService.getVirtualFolders());
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/mountsByPath/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<FileResource> getMountsForPath(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			String virtualPath = FileUtils.checkStartsWithSlash(
					FileUtils.stripParentPath(server.getApiPath() + "/fs/mountsByPath", 
							URLDecoder.decode(request.getRequestURI(), "UTF-8")));
			return new ResourceList<FileResource>(fileResourceService.getResourcesByVirtualPath(virtualPath));
		} finally {
			clearAuthenticatedContext();
		}
	}

	
	@AuthenticationRequired
	@RequestMapping(value = "fs/tree", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public Collection<TreeNode> getTree(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			
			VirtualFile rootFolder = fileService.getRootFolder();
			TreeNode rootNode = new TreeNode();
			rootNode.setId(rootFolder.getId().toString());
			rootNode.setParent("#");
			rootNode.setText("/");
			rootNode.getState().opened = true;
			rootNode.setFileType(VirtualFileType.ROOT);
			rootNode.setType("default");
			rootNode.setResourceId(rootFolder.getId());
			rootNode.setVirtualPath("/");
			
			List<TreeNode> results = new ArrayList<TreeNode>();
			results.add(rootNode);

			Collection<FileResource> mounts = fileService.getRootMounts();
			
			for(FileResource resource : mounts) {
				try {
					TreeNode node = new TreeNode();
					node.setParent(rootNode.id);
					node.setText(resource.getName());
					node.getState().opened = true;
					node.setFileType(VirtualFileType.FOLDER);
					node.setType("mount");
					node.setVirtualPath(resource.getVirtualPath());
					node.setResourceId(resource.getId());
					rootNode.children.add(node);
				} catch (Throwable e) {
					log.error(String.format("Could not add root mount resource %s", resource.getName()), e);
				}
			}
			
			Map<String,TreeNode> nodesById = new HashMap<String,TreeNode>();
			Map<String,TreeNode> nodesByPath = new HashMap<String,TreeNode>();
			
			nodesById.put(rootNode.getId().toString(), rootNode);
			nodesByPath.put(rootNode.getVirtualPath(), rootNode);
			
			for(VirtualFile file : fileService.getVirtualFolders()) {
				try {
					if(file.getParent()!=null) {
						TreeNode node = new TreeNode();
						node.setText(file.getDisplayName());
						node.setResourceId(file.getId());
					
						node.setId(String.valueOf(file.getId()));
						node.setParent(String.valueOf(file.getParent().getId()));
						node.getState().opened = true;
						node.setFileType(file.getType());
						
						node.setVirtualPath(file.getVirtualPath());
						node.setType("default");

						nodesById.get(String.valueOf(file.getParent().getId())).children.add(node);
						nodesByPath.put(FileUtils.checkEndsWithSlash(file.getVirtualPath()), node);
						nodesById.put(node.getId(), node);
						
					} 
				} catch (Throwable e) {
					log.error(String.format("Could not add virtual folder %s %s", file.getFilename(), file.getVirtualPath()), e);
				}
			}
			
			for(FileResource resource : fileService.getNonRootMounts()) {
				try {
					TreeNode node = new TreeNode();
					TreeNode parent = nodesByPath.get(FileUtils.checkEndsWithSlash(resource.getVirtualPath()));
					if(parent==null) {
						log.info(resource.getVirtualPath());
					}
					node.setId(String.valueOf(resource.getId()));
					node.setParent(parent.id);
					node.setText(resource.getName());
					node.getState().opened = true;
					node.setResourceId(resource.getId());
					node.setFileType(VirtualFileType.FOLDER);
					node.setType("mount");
					node.setVirtualPath(resource.getVirtualPath());
					parent.children.add(node);
				} catch (Throwable e) {
					log.error(String.format("Could not add file resource %s", resource.getName()), e);
				}
			}
			
			return results;
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/delete/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<Object> delete(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			
			String virtualPath = FileUtils.checkStartsWithSlash(
					FileUtils.stripParentPath(server.getApiPath() + "/fs/delete", 
							URLDecoder.decode(request.getRequestURI(), "UTF-8")));
			if(fileService.deleteFile(virtualPath, HTTP_PROTOCOL)) {
				return new ResourceStatus<Object>(true);
			} else {
				return new ResourceStatus<Object>(false);
			}
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/setDefaultMount/{fileId}/{mountId}", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<VirtualFile> setDefaultMound(HttpServletRequest request,
			HttpServletResponse response, @PathVariable Long fileId, @PathVariable Long mountId) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			
			FileResource mount = null;
			if(mountId > 0) {
				mount = fileResourceService.getResourceById(mountId);
			}
			VirtualFile file = fileService.getFileById(fileId);
			fileService.setDefaultMount(file, mount);
			return new ResourceStatus<VirtualFile>(file);
		} catch (ResourceException e) {
			return new ResourceStatus<VirtualFile>(false, e.getMessage());
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/file/{fileId}", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<VirtualFile> getFileById(HttpServletRequest request,
			HttpServletResponse response, @PathVariable Long fileId) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			
			VirtualFile file = fileService.getFileById(fileId);
			return new ResourceStatus<VirtualFile>(file);
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "fs/createVirtualFolder/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<VirtualFile> createVirtualFolder(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		
		String virtualPath = FileUtils.checkStartsWithSlash(
				FileUtils.stripParentPath(server.getApiPath() + "/fs/createVirtualFolder", 
						URLDecoder.decode(request.getRequestURI(), "UTF-8")));
		try {
			return new ResourceStatus<VirtualFile>(fileService.createVirtualFolder(virtualPath));
		} catch(FileExistsException e) { 
			return new ResourceStatus<VirtualFile>(false, I18N.getResource(sessionUtils.getLocale(request),
					FileResourceServiceImpl.RESOURCE_BUNDLE, "error.pathExists", virtualPath));
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/createFolder/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<VirtualFile> createFolder(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			String virtualPath = FileUtils.checkStartsWithSlash(
					FileUtils.stripParentPath(server.getApiPath() + "/fs/createFolder", 
							URLDecoder.decode(request.getRequestURI(), "UTF-8")));
			return new ResourceStatus<VirtualFile>(fileService.createUntitledFolder(virtualPath, HTTP_PROTOCOL));
		} catch(AccessDeniedException ex) { 
			return new ResourceStatus<VirtualFile>(false, I18N.getResource(sessionUtils.getLocale(request),
					FileResourceServiceImpl.RESOURCE_BUNDLE, "error.folderPermissionDenied"));
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/rename/**", method = { RequestMethod.POST, RequestMethod.GET }, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<VirtualFile> rename(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam String toUri) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			String virtualPath = FileUtils.checkStartsWithSlash(
					FileUtils.stripParentPath(server.getApiPath() + "/fs/rename", 
							URLDecoder.decode(request.getRequestURI(), "UTF-8")));
			String toVirtualPath = FileUtils.checkStartsWithSlash(
					FileUtils.stripParentPath(server.getApiPath() + "/fs/rename", toUri));
			
			return new ResourceStatus<VirtualFile>(fileService.renameFile(virtualPath, toVirtualPath, HTTP_PROTOCOL));
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/download/**", method = RequestMethod.GET, produces = { "application/json" })
	public void downloadFile(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(required=false) Boolean forceDownload) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException, IOException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			String virtualPath = FileUtils.checkStartsWithSlash(
					FileUtils.stripParentPath(server.getApiPath() + "/fs/download",
							URLDecoder.decode(request.getRequestURI(), "UTF-8")));

			fileService.downloadFile(virtualPath, new HttpDownloadProcessor(
							request, 
							response, 
							0, 
							Long.MAX_VALUE, 
							HTTP_PROTOCOL, 
							sessionUtils.getActiveSession(request)), 
							HTTP_PROTOCOL);

			response.setStatus(HttpStatus.OK.value());
			
		} catch(FileSystemException ex) {  
			if(ex.getCause() instanceof FileNotFoundException) {
				response.sendError(HttpStatus.NOT_FOUND.value());
				return;
			}
			throw ex;
		} catch(FileNotFoundException ex) {  
			response.sendError(HttpStatus.NOT_FOUND.value());
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/upload/**", method = RequestMethod.POST, produces = {"application/json" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public ResourceStatus<FileUpload> uploadFile(final HttpServletRequest request,
			HttpServletResponse response,
			final @RequestPart(value = "file") MultipartFile file)
			throws AccessDeniedException, UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {

			String virtualPath = FileUtils.checkEndsWithSlash(
					FileUtils.checkStartsWithSlash(FileUtils.stripParentPath(server.getApiPath() + "/fs/upload", 
					URLDecoder.decode(request.getRequestURI(), "UTF-8"))));
			virtualPath = virtualPath + FileUtils.lastPathElement(file.getOriginalFilename());
			
			return new ResourceStatus<FileUpload>(fileService.uploadFile(
					FileUtils.checkEndsWithNoSlash(virtualPath), file.getInputStream(), null, HTTP_PROTOCOL));
			
			
		} catch(Exception e) { 
			return new ResourceStatus<FileUpload>(false, e.getMessage());
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/list/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<VirtualFile> list(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			String virtualPath = FileUtils.checkStartsWithSlash(
					FileUtils.stripParentPath(server.getApiPath() + "/fs/list", 
							URLDecoder.decode(request.getRequestURI(), "UTF-8")));
			
			return new ResourceList<VirtualFile>(fileService.listChildren(virtualPath, HTTP_PROTOCOL));

		} catch (FileSystemException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "fs/lastError", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public RequestStatus getLastError(HttpServletRequest request) throws UnauthorizedException, SessionTimeoutException {
		
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			Throwable e = (Throwable) request.getSession().getAttribute("lastError");
			if(e!=null) {
				String msg = e.getMessage() + (e.getCause()!=null ? "; " + e.getCause().getMessage() : "");
				return new RequestStatus(true, msg);
			} else {
				return new RequestStatus(false);
			}
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/search/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public BootstrapTableResult search(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			final String virtualPath = FileUtils.checkStartsWithSlash(
					FileUtils.stripParentPath(server.getApiPath() + "/fs/search", 
							URLDecoder.decode(request.getRequestURI(), "UTF-8")));
			
			return processDataTablesRequest(request, new BootstrapTableResourceProcessor<VirtualFile>() {
				
				@Override
				public Long getTotalCount(String searchColumn, String searchPattern)
						throws UnauthorizedException, AccessDeniedException {
					long count = fileService.getSearchCount(virtualPath, "filename", searchPattern);
					return count;
				}
				
				@Override
				public Collection<?> getPage(String searchColumn, String searchPattern, int start, int length, ColumnSort[] sorting)
						throws UnauthorizedException, AccessDeniedException {
					try {
						Collection<?> results = fileService.searchFiles(virtualPath, "filename", searchPattern, start, length, sorting, HTTP_PROTOCOL);
						return results;
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
				
				@Override
				public Column getColumn(String column) {
					return FileSystemColumn.valueOf(column.toUpperCase());
				}

				@Override
				public VirtualFile getResource() throws IOException, AccessDeniedException {
					return fileService.getFile(virtualPath);
				}
			});

		} catch (IllegalStateException e) {
			request.getSession().setAttribute("lastError", e);
			throw e;
		} catch (IOException e) {
			request.getSession().setAttribute("lastError", e);
			throw e;
		} finally {
			clearAuthenticatedContext();
		}
	}
}
