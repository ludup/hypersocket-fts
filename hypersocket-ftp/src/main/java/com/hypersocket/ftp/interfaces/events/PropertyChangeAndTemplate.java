package com.hypersocket.ftp.interfaces.events;

import com.hypersocket.properties.PropertyTemplate;
import com.hypersocket.resource.PropertyChange;

public class PropertyChangeAndTemplate {

	private PropertyTemplate propertyTemplate;
	private PropertyChange propertyChange;
	
	public PropertyChangeAndTemplate(PropertyTemplate propertyTemplate, PropertyChange propertyChange) {
		this.propertyTemplate = propertyTemplate;
		this.propertyChange = propertyChange;
	}

	public PropertyTemplate getPropertyTemplate() {
		return propertyTemplate;
	}

	public void setPropertyTemplate(PropertyTemplate propertyTemplate) {
		this.propertyTemplate = propertyTemplate;
	}

	public PropertyChange getPropertyChange() {
		return propertyChange;
	}

	public void setPropertyChange(PropertyChange propertyChange) {
		this.propertyChange = propertyChange;
	}

}
