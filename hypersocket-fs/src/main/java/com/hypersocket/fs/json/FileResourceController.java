package com.hypersocket.fs.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.hypersocket.auth.json.AuthenticationRequired;
import com.hypersocket.auth.json.ResourceController;
import com.hypersocket.auth.json.UnauthorizedException;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceScheme;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.fs.FileResourceServiceImpl;
import com.hypersocket.i18n.I18N;
import com.hypersocket.i18n.I18NServiceImpl;
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
import com.hypersocket.resource.ResourceExportException;
import com.hypersocket.resource.ResourceNotFoundException;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.session.json.SessionUtils;
import com.hypersocket.tables.BootstrapTableResult;
import com.hypersocket.tables.Column;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.tables.json.BootstrapTablePageProcessor;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.utils.HypersocketUtils;

@Controller
public class FileResourceController extends ResourceController {

	static Logger log = LoggerFactory.getLogger(FileResourceController.class);

	@Autowired
	FileResourceService mountService;

	@Autowired
	SessionUtils sessionUtils;

	@RequestMapping(value = "mounts/schemes", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<FileResourceScheme> getResourcesByCurrentPrincipal(
			HttpServletRequest request, HttpServletResponse response)
			throws AccessDeniedException, UnauthorizedException {
		return new ResourceList<FileResourceScheme>(mountService.getSchemes());
	}

	@AuthenticationRequired
	@RequestMapping(value = "mounts/list", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<FileResource> getResources(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			return new ResourceList<FileResource>(
					mountService.getResources(sessionUtils
							.getCurrentRealm(request)));
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "mounts/template", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<PropertyCategory> getResourceTemplate(
			HttpServletRequest request) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			return new ResourceList<PropertyCategory>();
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "mounts/table", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public BootstrapTableResult tableMounts(final HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			return processDataTablesRequest(request,
					new BootstrapTablePageProcessor() {

						@Override
						public Column getColumn(String col) {
							return ResourceColumns.valueOf(col.toUpperCase());
						}

						@Override
						public List<?> getPage(String searchPattern, int start,
								int length, ColumnSort[] sorting)
								throws UnauthorizedException,
								AccessDeniedException {
							return mountService.searchResources(
									sessionUtils.getCurrentRealm(request),
									searchPattern, start, length, sorting);
						}

						@Override
						public Long getTotalCount(String searchPattern)
								throws UnauthorizedException,
								AccessDeniedException {
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
	@RequestMapping(value = "mounts/fingerprint", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<String> getFingerprint(
			HttpServletRequest request, HttpServletResponse response)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			return new ResourceStatus<String>(true, mountService.getFingerprint());
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "mounts/myResources", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<FileResource> myFileResources(
			final HttpServletRequest request, HttpServletResponse response)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			ResourceList<FileResource> list = new ResourceList<FileResource>(
					new HashMap<String,String>(),
					mountService.getPersonalResources(sessionUtils
							.getPrincipal(request)));
			list.getProperties().put(
					"authCode",
					sessionService.createSessionToken(
							sessionUtils.getSession(request)).getShortCode());
			return list;
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "mounts/personal", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public BootstrapTableResult personalMounts(
			final HttpServletRequest request, HttpServletResponse response)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			return processDataTablesRequest(request,
					new BootstrapTablePageProcessor() {

						@Override
						public Column getColumn(String col) {
							return ResourceColumns.valueOf(col.toUpperCase());
						}

						@Override
						public Collection<?> getPage(String searchPattern,
								int start, int length, ColumnSort[] sorting)
								throws UnauthorizedException,
								AccessDeniedException {
							return mountService.searchPersonalResources(
									sessionUtils.getPrincipal(request),
									searchPattern, start, length, sorting);
						}

						@Override
						public Long getTotalCount(String searchPattern)
								throws UnauthorizedException,
								AccessDeniedException {
							return mountService.getPersonalResourceCount(
									sessionUtils.getPrincipal(request),
									searchPattern);
						}
					});
		} finally {
			clearAuthenticatedContext();
		}
	}

	@SuppressWarnings("unchecked")
	@AuthenticationRequired
	@RequestMapping(value = "mounts/mount", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<FileResource> createOrUpdateResource(
			HttpServletRequest request, HttpServletResponse response,
			@RequestBody FileResourceUpdate resource)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			Realm realm = sessionUtils.getCurrentRealm(request);

			Set<Role> roles = new HashSet<Role>();
			for (Long id : resource.getRoles()) {
				roles.add(permissionRepository.getRoleById(id));
			}

			FileResource r;
			if (resource.getId() != null) {
				r = mountService.getResourceById(resource.getId());
				buildResource(realm, r, resource, roles, false,
						mountService.getCurrentUsername(),
						mountService.getCurrentPassword());
				mountService.updateResource(r, new HashMap<String, String>());
			} else {
				r = new FileResource();
				buildResource(realm, r, resource, roles, true,
						mountService.getCurrentUsername(),
						mountService.getCurrentPassword());
				mountService.createResource(r, new HashMap<String, String>());
			}

			return new ResourceStatus<FileResource>(r, I18N.getResource(
					sessionUtils.getLocale(request),
					FileResourceServiceImpl.RESOURCE_BUNDLE,
					resource.getId() != null ? "mount.updated.info"
							: "mount.created.info", resource.getName()));

		} catch (ResourceException e) {
			return new ResourceStatus<FileResource>(false, e.getMessage());
		} finally {
			clearAuthenticatedContext();
		}
	}

	@SuppressWarnings("unchecked")
	@AuthenticationRequired
	@RequestMapping(value = "mounts/mount/{id}", method = RequestMethod.DELETE, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<FileResource> deleteResource(
			HttpServletRequest request, HttpServletResponse response,
			@PathVariable("id") Long id) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			FileResource resource = mountService.getResourceById(id);

			if (resource == null) {
				return new ResourceStatus<FileResource>(false,
						I18N.getResource(sessionUtils.getLocale(request),
								FileResourceServiceImpl.RESOURCE_BUNDLE,
								"error.invalidResourceId", id));
			}

			String preDeletedName = resource.getName();
			mountService.deleteResource(resource);

			return new ResourceStatus<FileResource>(true, I18N.getResource(
					sessionUtils.getLocale(request),
					FileResourceServiceImpl.RESOURCE_BUNDLE,
					"mount.deleted.info", preDeletedName));

		} catch (ResourceException e) {
			return new ResourceStatus<FileResource>(false, e.getMessage());
		} finally {
			clearAuthenticatedContext();
		}
	}

	private void buildResource(Realm realm, FileResource resource,
			FileResourceUpdate update, Set<Role> roles, boolean creating,
			String username, String password) throws UnauthorizedException,
			ResourceChangeException, ResourceCreationException {
		resource.setName(update.getName());

		resource.setRealm(realm);
		resource.setScheme(update.getScheme());
		resource.setServer(update.getServer());
		resource.setPort(update.getPort());
		resource.setPath(FileUtils.convertBackslashToForwardSlash(update
				.getPath()));
		resource.setLogo(update.getLogo());
		resource.setUsername(update.getUsername());
		resource.setPassword(update.getPassword());

		resource.setReadOnly(update.isReadOnly());
		resource.setShowFolders(update.isShowFolders());
		resource.setShowHidden(update.isShowHidden());

		resource.setRoles(roles);

		// try {
		// if (!mountService.testVFSUri(resource.getPrivateUrl(username,
		// password))) {
		// if (creating) {
		// throw new ResourceCreationException(
		// FileResourceServiceImpl.RESOURCE_BUNDLE,
		// "error.fileDoesNotExist", resource.getUrl());
		// } else {
		// throw new ResourceChangeException(
		// FileResourceServiceImpl.RESOURCE_BUNDLE,
		// "error.fileDoesNotExist", resource.getUrl());
		// }
		// }
		// } catch (FileSystemException e) {
		// log.error("Failed to access " + resource.getUrl(), e);
		// if (creating) {
		// throw new ResourceCreationException(
		// FileResourceServiceImpl.RESOURCE_BUNDLE,
		// "error.failedToAccessFile", e.getMessage());
		// } else {
		// throw new ResourceChangeException(
		// FileResourceServiceImpl.RESOURCE_BUNDLE,
		// "error.failedToAccessFile", e.getMessage());
		// }
		// }
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "mounts/export/{id}", method = RequestMethod.GET, produces = { "text/plain" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public String exportResource(HttpServletRequest request,
			HttpServletResponse response, @PathVariable("id") long id)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException, ResourceNotFoundException,
			ResourceExportException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		try {
			response.setHeader("Content-Disposition", "attachment; filename=\""
					+ mountService.getResourceCategory() + "-"
					+ mountService.getResourceById(id).getName() + ".json\"");
			return mountService.exportResoure(id);
		} finally {
			clearAuthenticatedContext();
		}

	}

	@AuthenticationRequired
	@RequestMapping(value = "mounts/export", method = RequestMethod.GET, produces = { "text/plain" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public String exportAll(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException,
			ResourceNotFoundException, ResourceExportException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		try {
			response.setHeader("Content-Disposition", "attachment; filename=\""
					+ mountService.getResourceCategory() + ".json\"");
			return mountService.exportAllResoures();
		} finally {
			clearAuthenticatedContext();
		}

	}

	@AuthenticationRequired
	@RequestMapping(value = "mounts/import", method = { RequestMethod.POST }, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<FileResource> importAll(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value = "file") MultipartFile jsonFile,
			@RequestParam(required = false) boolean dropExisting)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
		}
		try {
			String json = IOUtils.toString(jsonFile.getInputStream());
			if (!HypersocketUtils.isValidJSON(json)) {
				throw new ResourceException(
						I18NServiceImpl.USER_INTERFACE_BUNDLE,
						"error.incorrectJSON");
			}
			Collection<FileResource> collects = mountService.importResources(
					json, getCurrentRealm(), dropExisting);
			return new ResourceStatus<FileResource>(true, I18N.getResource(
					sessionUtils.getLocale(request),
					I18NServiceImpl.USER_INTERFACE_BUNDLE,
					"resource.import.success", collects.size()));
		} catch (ResourceException e) {
			return new ResourceStatus<FileResource>(false, e.getMessage());
		} catch (Exception e) {
			return new ResourceStatus<FileResource>(false, I18N.getResource(
					sessionUtils.getLocale(request),
					I18NServiceImpl.USER_INTERFACE_BUNDLE,
					"resource.import.failure", e.getMessage()));
		} finally {
			clearAuthenticatedContext();
		}
	}

}
