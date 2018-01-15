package org.planner.ui.util;

import javax.servlet.ServletContext;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

public class KeepLoggedInExtension implements ServletExtension {

	@Override
	public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
		deploymentInfo.addAuthenticationMechanism("KEEP", new KeepLoggedInAuthenticationMechanism.Factory());
	}
}
