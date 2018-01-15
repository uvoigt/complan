package org.planner.ui.util;

import java.security.Principal;

import javax.security.auth.login.LoginException;

import org.jboss.security.auth.spi.DatabaseServerLoginModule;
import org.planner.eo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeepLoggedInLoginModule extends DatabaseServerLoginModule {

	private static final Logger LOG = LoggerFactory.getLogger(KeepLoggedInLoginModule.class);

	private Principal identity;

	@Override
	@SuppressWarnings("unchecked")
	public boolean login() throws LoginException {
		Object user = sharedState.get("javax.security.auth.login.user");
		if (user == null) {
			User authenticatedUser = KeepLoggedInAuthenticationMechanism.getAuthenticatedUser();
			if (authenticatedUser != null) {
				try {
					identity = createIdentity(authenticatedUser.getUserId());
				} catch (Exception e) {
					LOG.error("Error creating identity", e);
					return false;
				}
				if (LOG.isDebugEnabled())
					LOG.debug("Successful login of " + getIdentity() + " using cookie");
				super.loginOk = true;
				sharedState.put("javax.security.auth.login.user", authenticatedUser.getUserId());
				return true;
			}
		}
		boolean success = super.login();
		if (success) {
			if (LOG.isDebugEnabled())
				LOG.debug("Successfull login of " + getIdentity() + " using the database");
		}
		return success;
	}

	@Override
	protected Principal getIdentity() {
		return identity != null ? identity : super.getIdentity();
	}
}
