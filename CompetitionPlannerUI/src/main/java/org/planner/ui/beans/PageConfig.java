package org.planner.ui.beans;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.planner.ui.util.JsfUtil;

@Named
@ApplicationScoped
public class PageConfig {
	private final Set<String> pages = new HashSet<>();

	public void setTransient(boolean isTransient) {
		String page = (String) JsfUtil.getViewVariable("mainContent");
		if (isTransient)
			pages.add(page);
		else
			pages.remove(page);
	}

	public boolean isTransient() {
		String page = (String) JsfUtil.getViewVariable("mainContent");
		return pages.contains(page);
	}
}
