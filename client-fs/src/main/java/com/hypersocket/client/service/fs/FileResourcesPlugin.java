package com.hypersocket.client.service.fs;

import java.io.IOException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypersocket.client.rmi.BrowserLauncher;
import com.hypersocket.client.rmi.Resource;
import com.hypersocket.client.rmi.Resource.Type;
import com.hypersocket.client.rmi.ResourceImpl;
import com.hypersocket.client.service.AbstractServicePlugin;

public class FileResourcesPlugin extends AbstractServicePlugin {

	static Logger log = LoggerFactory.getLogger(FileResourcesPlugin.class);

	public FileResourcesPlugin() {
		super("mounts");
	}

	protected void reloadResources(List<Resource> realmResources) {
		if (log.isInfoEnabled()) {
			log.info("Loading File Resources");
		}
		startFiles(realmResources);
	}

	@Override
	public boolean onStart() {
		return true;
	}

	protected void startFiles(List<Resource> realmResources) {
		try {
			String json = serviceClient.getTransport().get("mounts/myResources");

			ObjectMapper mapper = new ObjectMapper();

			JsonFileResourceList list = mapper.readValue(json,
					JsonFileResourceList.class);

			Map<String, String> properties = list.getProperties();

			int errors = processFileResources(list.getResources(),
					properties.get("authCode"), realmResources);

			if (errors > 0) {
				// Warn
				serviceClient.showWarning(errors
						+ " websites could not be opened.");
			}

		} catch (IOException e) {
			if (log.isErrorEnabled()) {
				log.error("Could not start files resources", e);
			}
		}
	}

	protected int processFileResources(JsonFileResource[] resources,
			String authCode, List<Resource> realmResources) throws IOException {

		int errors = 0;

		for (JsonFileResource resource : resources) {
			ResourceImpl res = new ResourceImpl("file-"
					+ String.valueOf(resource.getId()), resource.getName());

			res.setLaunchable(true);
			res.setGroup(res.getName());
			res.setGroupIcon(resource.getLogo());
			res.setIcon(resource.getLogo());
			res.setModified(resource.getModifiedDate());
			res.setType(Type.FILE);

			String sessionId = serviceClient.getSessionId();
			res.setResourceLauncher(new BrowserLauncher(serviceClient
					.getTransport().resolveUrl(
							"attach/"
									+ authCode
									+ "/"
									+ sessionId
									+ "?location="
									+ URLEncoder.encode(
											resource.getLaunchUrl(), "UTF-8"))));
			realmResources.add(res);

		}

		return errors;
	}

	@Override
	public void onStop() {

		if (log.isInfoEnabled()) {
			log.info("Stopping File Resources plugin");
		}

		try {
			resourceService.removeResourceRealm(serviceClient.getHost());
		} catch (RemoteException e) {
			log.error(
					"Failed to remove resource realm "
							+ serviceClient.getHost(), e);
		}

	}

	@Override
	public String getName() {
		return "File Resources";
	}

	@Override
	protected boolean onCreatedResource(Resource resource) {
		return true;
	}

	@Override
	protected boolean onUpdatedResource(Resource resource) {
		return true;
	}

	@Override
	protected boolean onDeletedResource(Resource resource) {
		return true;
	}

}
