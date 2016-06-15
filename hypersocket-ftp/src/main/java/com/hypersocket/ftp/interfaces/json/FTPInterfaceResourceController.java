package com.hypersocket.ftp.interfaces.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.hypersocket.auth.json.AuthenticationRequired;
import com.hypersocket.auth.json.ResourceController;
import com.hypersocket.auth.json.UnauthorizedException;
import com.hypersocket.i18n.I18N;
import com.hypersocket.json.ResourceList;
import com.hypersocket.json.ResourceStatus;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.properties.json.PropertyItem;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.ResourceException;
import com.hypersocket.resource.ResourceNotFoundException;
import com.hypersocket.resource.ResourceUpdate;
import com.hypersocket.ftp.interfaces.FTPInterfaceResource;
import com.hypersocket.ftp.interfaces.FTPInterfaceResourceColumns;
import com.hypersocket.ftp.interfaces.FTPInterfaceResourceService;
import com.hypersocket.ftp.interfaces.FTPInterfaceResourceServiceImpl;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.tables.Column;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.tables.BootstrapTableResult;
import com.hypersocket.tables.json.BootstrapTablePageProcessor;

@Controller
public class FTPInterfaceResourceController extends ResourceController {

	/**
	 * TODO rename this class to match your entity.
	 * 
	 * rename RequestMapping annotions for your desired resource URLs. e.g
	 * replace fTPInterfaces for example with "applications" FTPInterfaces with "Applications"
	 * fTPInterface with "application" and FTPInterface with "Application"
	 */
	@Autowired
	FTPInterfaceResourceService resourceService;

	@AuthenticationRequired
	@RequestMapping(value = "fTPInterfaces/list", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<FTPInterfaceResource> getResources(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			return new ResourceList<FTPInterfaceResource>(
					resourceService.getResources(sessionUtils
							.getCurrentRealm(request)));
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fTPInterfaces/table", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public BootstrapTableResult tableResources(
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
							return FTPInterfaceResourceColumns.valueOf(col.toUpperCase());
						}

						@Override
						public List<?> getPage(String searchColumn, String searchPattern, int start,
								int length, ColumnSort[] sorting)
								throws UnauthorizedException,
								AccessDeniedException {
							return resourceService.searchResources(
									sessionUtils.getCurrentRealm(request),
									searchColumn, searchPattern, start, length, sorting);
						}

						@Override
						public Long getTotalCount(String searchColumn, String searchPattern)
								throws UnauthorizedException,
								AccessDeniedException {
							return resourceService.getResourceCount(
									sessionUtils.getCurrentRealm(request),
									searchColumn, searchPattern);
						}
					});
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "fTPInterfaces/template", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<PropertyCategory> getResourceTemplate(
			HttpServletRequest request) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {
			return new ResourceList<PropertyCategory>(resourceService.getPropertyTemplate());
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fTPInterfaces/properties/{id}", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceList<PropertyCategory> getActionTemplate(
			HttpServletRequest request, @PathVariable Long id)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException, ResourceNotFoundException {
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			FTPInterfaceResource resource = resourceService.getResourceById(id);
			return new ResourceList<PropertyCategory>(resourceService.getPropertyTemplate(resource));
		} finally {
			clearAuthenticatedContext();
		}
	}

	@AuthenticationRequired
	@RequestMapping(value = "fTPInterfaces/fTPInterface/{id}", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public FTPInterfaceResource getResource(HttpServletRequest request,
			HttpServletResponse response, @PathVariable("id") Long id)
			throws AccessDeniedException, UnauthorizedException,
			ResourceNotFoundException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			return resourceService.getResourceById(id);
		} finally {
			clearAuthenticatedContext();
		}

	}

	@AuthenticationRequired
	@RequestMapping(value = "fTPInterfaces/fTPInterface", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<FTPInterfaceResource> createOrUpdateResource(
			HttpServletRequest request, HttpServletResponse response,
			@RequestBody ResourceUpdate resource)
			throws AccessDeniedException, UnauthorizedException,
			SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			FTPInterfaceResource newResource;

			Realm realm = sessionUtils.getCurrentRealm(request);

			Map<String, String> properties = new HashMap<String, String>();
			for (PropertyItem i : resource.getProperties()) {
				properties.put(i.getId(), i.getValue());
			}
			
			if (resource.getId() != null) {
				newResource = resourceService.updateResource(
						resourceService.getResourceById(resource.getId()),
						resource.getName(), properties);
			} else {
				newResource = resourceService.createResource(
						resource.getName(),
						realm,
						properties);
			}
			return new ResourceStatus<FTPInterfaceResource>(newResource,
					I18N.getResource(sessionUtils.getLocale(request),
							FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE,
							resource.getId() != null ? "resource.updated.info"
									: "resource.created.info", resource
									.getName()));

		} catch (ResourceException e) {
			return new ResourceStatus<FTPInterfaceResource>(false,
					e.getMessage());
		} finally {
			clearAuthenticatedContext();
		}
	}

	@SuppressWarnings("unchecked")
	@AuthenticationRequired
	@RequestMapping(value = "fTPInterfaces/fTPInterface/{id}", method = RequestMethod.DELETE, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<FTPInterfaceResource> deleteResource(
			HttpServletRequest request, HttpServletResponse response,
			@PathVariable("id") Long id) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			FTPInterfaceResource resource = resourceService.getResourceById(id);

			if (resource == null) {
				return new ResourceStatus<FTPInterfaceResource>(false,
						I18N.getResource(sessionUtils.getLocale(request),
								FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE,
								"error.invalidResourceId", id));
			}

			String preDeletedName = resource.getName();
			resourceService.deleteResource(resource);

			return new ResourceStatus<FTPInterfaceResource>(true, I18N.getResource(
					sessionUtils.getLocale(request),
					FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE,
					"resource.deleted.info", preDeletedName));

		} catch (ResourceException e) {
			return new ResourceStatus<FTPInterfaceResource>(false, e.getMessage());
		} finally {
			clearAuthenticatedContext();
		}
	}
}
