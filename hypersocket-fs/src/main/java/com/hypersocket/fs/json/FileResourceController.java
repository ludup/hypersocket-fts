package com.hypersocket.fs.json;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
import com.hypersocket.fs.FileResourceScheme;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.fs.UploadProcessor;
import com.hypersocket.fs.tree.TreeFile;
import com.hypersocket.fs.tree.TreeFolder;
import com.hypersocket.fs.tree.TreeList;
import com.hypersocket.i18n.I18N;
import com.hypersocket.json.ResourceList;
import com.hypersocket.json.ResourceStatus;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.Role;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceColumns;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.resource.ResourceException;
import com.hypersocket.resource.ResourceNotFoundException;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.session.json.SessionUtils;
import com.hypersocket.tables.Column;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.tables.DataTablesResult;
import com.hypersocket.tables.json.DataTablesPageProcessor;
import com.hypersocket.util.FileUtils;

@Controller
public class FileResourceController extends ResourceController {

	static Logger log = LoggerFactory.getLogger(FileResourceController.class);
	
	public static final String HTTP_PROTOCOL = "HTTP";
	public static final String CONTENT_INPUTSTREAM = "ContentInputStream";
	
	@Autowired
	FileResourceService mountService;

	@Autowired
	SessionUtils sessionUtils;

	
	
	@RequestMapping(value = "schemes", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<FileResourceScheme> getResourcesByCurrentPrincipal(
			HttpServletRequest request, HttpServletResponse response)
			throws AccessDeniedException, UnauthorizedException {
		return new ResourceList<FileResourceScheme>(mountService.getSchemes());
	}

	@AuthenticationRequired
	@RequestMapping(value = "mounts", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<FileResource> getResources(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);
		try {
			return new ResourceList<FileResource>(
					mountService.getResources(sessionUtils
							.getCurrentRealm(request)));
		} finally {
			clearAuthenticatedContext(mountService);
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "template/mount", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<PropertyCategory> getResourceTemplate(HttpServletRequest request)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			return new ResourceList<PropertyCategory>();
		} finally {
			clearAuthenticatedContext();
		}	
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "table/mounts", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public DataTablesResult tableMounts(final HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);

		try {
			return processDataTablesRequest(request,
					new DataTablesPageProcessor() {

						@Override
						public Column getColumn(int col) {
							return ResourceColumns.values()[col];
						}

						@Override
						public List<?> getPage(String searchPattern, int start, int length,
								ColumnSort[] sorting) throws UnauthorizedException, AccessDeniedException {
							return mountService.searchResources(sessionUtils.getCurrentRealm(request),
									searchPattern, start, length, sorting);
						}
						
						@Override
						public Long getTotalCount(String searchPattern) throws UnauthorizedException, AccessDeniedException {
							return mountService.getResourceCount(
									sessionUtils.getCurrentRealm(request),
									searchPattern);
						}
					});
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "mount", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<FileResource> createOrUpdateResource(
			HttpServletRequest request, HttpServletResponse response,
			@RequestBody FileResourceUpdate resource)
			throws AccessDeniedException, UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);
		try {

			Realm realm = sessionUtils.getCurrentRealm(request);

			Set<Role> roles = new HashSet<Role>();
			for (Long id : resource.getRoles()) {
				roles.add(permissionService.getRoleById(id, realm));
			}

			FileResource r;
			if (resource.getId() != null) {
				r = mountService.getResourceById(resource.getId());
				buildResource(realm, r, resource, roles);
				mountService.updateResource(r);
			} else {
				r = new FileResource();
				buildResource(realm, r, resource, roles);
				mountService.createResource(r);
			}

			return new ResourceStatus<FileResource>(r, I18N.getResource(
					sessionUtils.getLocale(request),
					FileResourceService.RESOURCE_BUNDLE,
					resource.getId() != null ? "mount.updated.info"
							: "mount.created.info", resource.getName()));

		} catch (ResourceCreationException e) {
			return new ResourceStatus<FileResource>(false, I18N.getResource(
					sessionUtils.getLocale(request), e.getBundle(),
					e.getResourceKey(), e.getArgs()));
		} catch (ResourceChangeException e) {
			return new ResourceStatus<FileResource>(false, I18N.getResource(
					sessionUtils.getLocale(request), e.getBundle(),
					e.getResourceKey(), e.getArgs()));
		} catch (ResourceNotFoundException e) {
			return new ResourceStatus<FileResource>(false, I18N.getResource(
					sessionUtils.getLocale(request), e.getBundle(),
					e.getResourceKey(), e.getArgs()));
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "mount/{id}", method = RequestMethod.DELETE, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<FileResource> deleteResource(
			HttpServletRequest request, HttpServletResponse response,
			@PathVariable("id") Long id) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);
		try {

			FileResource resource = mountService.getResourceById(id);

			if (resource == null) {
				return new ResourceStatus<FileResource>(false,
						I18N.getResource(sessionUtils.getLocale(request),
								FileResourceService.RESOURCE_BUNDLE,
								"error.invalidResourceId", id));
			}

			String preDeletedName = resource.getName();
			mountService.deleteResource(resource);

			return new ResourceStatus<FileResource>(true, I18N.getResource(
					sessionUtils.getLocale(request),
					FileResourceService.RESOURCE_BUNDLE, "mount.deleted.info",
					preDeletedName));

		} catch (ResourceException e) {
			return new ResourceStatus<FileResource>(false, e.getMessage());
		} finally {
			clearAuthenticatedContext();
		}
	}

	
	@SuppressWarnings("rawtypes")
	@AuthenticationRequired
	@RequestMapping(value = "fsDelete/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<?> delete(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);
		try {
			
			return new ResourceStatus(mountService.deleteURIFile(
					request.getHeader("Host"), "api/fsDelete",
					URLDecoder.decode(request.getRequestURI(), "UTF-8"), HTTP_PROTOCOL));

		} finally {
			clearAuthenticatedContext();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@AuthenticationRequired
	@RequestMapping(value = "fsCreateFolder/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<TreeList> createFolder(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);
		try {

			String uri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
			
			FileResource resource = mountService.getMountForURIPath(
					request.getHeader("Host"), "api/fsCreateFolder",
					uri);

			FileObject mountFile = mountService.resolveMountFile(resource);
			
			List folders = new ArrayList();
			
			FileObject newFile = mountService.createURIFolder(
					request.getHeader("Host"), "api/fsCreateFolder",
					uri, HTTP_PROTOCOL);
			
			folders.add(new TreeFolder(newFile, mountFile, resource));
			return new ResourceStatus(new TreeList(folders), "");

		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@SuppressWarnings("rawtypes")
	@AuthenticationRequired
	@RequestMapping(value = "fsRename/**", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<?> rename(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam String toUri) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);
		try {

			return new ResourceStatus(mountService.renameURIFile(
					request.getHeader("Host"), "api/fsRename",
					URLDecoder.decode(request.getRequestURI(), "UTF-8"),
					URLDecoder.decode(toUri, "UTF-8"), HTTP_PROTOCOL));

		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fsDownload/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseStatus(value = HttpStatus.OK)
	public void downloadFile(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam String forceDownload) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);
		try {

			String uri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
			
			mountService.downloadURIFile(request.getHeader("Host"), 
					"api/fsDownload", uri, new HttpDownloadProcessor(request, response, 0, Long.MAX_VALUE, HTTP_PROTOCOL), HTTP_PROTOCOL);
			

		} catch (Exception e) {
			try {
				response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
			} catch (IOException e1) {
			}
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fsUpload/**", method = RequestMethod.POST, produces = {"application/json" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public ResourceStatus<TreeFile> uploadFile(HttpServletRequest request,
			HttpServletResponse response,
			@RequestPart(value = "file") MultipartFile file)
			throws AccessDeniedException, UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);

		try {

			String uri = FileUtils.checkEndsWithSlash(URLDecoder.decode(request.getRequestURI(), "UTF-8"));
			uri += FileUtils.lastPathElement(file.getOriginalFilename());
			
			UploadProcessor<TreeFile> processor = new UploadProcessor<TreeFile>() {

				TreeFile treeFile;
				@Override
				public void processUpload(FileResource resource,
						FileObject mountFile,
						String childPath, FileObject file) throws FileSystemException {
					treeFile = new TreeFile(file,  mountFile);
				}

				@Override
				public TreeFile getResult() {
					return treeFile;
				}
				
			};
			mountService.uploadURIFile(request.getHeader("Host"), 
					"api/fsUpload", uri, file.getInputStream(), processor, HTTP_PROTOCOL);
			
			return new ResourceStatus<TreeFile>(processor.getResult());
			
			
		} finally {
			clearAuthenticatedContext(mountService);
		}
	}
	
	@SuppressWarnings("unchecked")
	@AuthenticationRequired
	@RequestMapping(value = "fsList/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public TreeList list(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request), mountService);
		try {

			@SuppressWarnings("rawtypes")
			List folders = new ArrayList();
			String uri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
			FileResource resource = mountService.getMountForURIPath(
					request.getHeader("Host"), "api/fsList",
					uri);

			FileObject mountFile = mountService.resolveMountFile(resource);

			String childPath = mountService.resolveURIChildPath(resource,
					"api/fsList", uri);

			FileObject file = mountFile.resolveFile(childPath);

			for (FileObject f : file.getChildren()) {
				if (f.getType() == FileType.FOLDER
						&& (!f.isHidden() || resource.isShowHidden())) {
					folders.add(new TreeFolder(f, mountFile, resource));
				}
			}

			for (FileObject f : file.getChildren()) {
				if (f.getType() == FileType.FILE
						&& (!f.isHidden() || resource.isShowHidden())) {
					folders.add(new TreeFile(f, mountFile));
				}
			}

			return new TreeList(folders);

		} catch (FileSystemException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			clearAuthenticatedContext();
		}
	}

	private void buildResource(Realm realm, FileResource resource,
			FileResourceUpdate update, Set<Role> roles)
			throws UnauthorizedException {
		resource.setName(update.getName());

		resource.setRealm(realm);
		resource.setScheme(update.getScheme());
		resource.setServer(update.getServer());
		resource.setPort(update.getPort());
		resource.setPath(update.getPath());
		resource.setUsername(update.getUsername());
		resource.setPassword(update.getPassword());

		resource.setReadOnly(update.isReadOnly());
		resource.setShowFolders(update.isShowFolders());
		resource.setShowHidden(update.isShowHidden());

		resource.setRoles(roles);
	}

}
