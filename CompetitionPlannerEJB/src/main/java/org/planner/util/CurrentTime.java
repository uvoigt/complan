package org.planner.util;

import java.util.Date;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CurrentTime {

	public Date now() {
		return new Date();
	}
}
