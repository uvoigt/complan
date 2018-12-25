package org.planner.ui.beans.announcement;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.planner.eo.Program;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.util.JsfUtil;

@Named
@RequestScoped
public class ResultBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Program program;

	@Override
	@PostConstruct
	public void init() {
		super.init();

		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null) {
			JsfUtil.setViewVariable("id", id);
			if (!isCancelPressed()) {
				loadResults(id);
			}
		}
	}

	@Override
	public void setItem(Object item) {
		program = (Program) item;
		// das könnte auch als Argument in der search-xhtml mitgegeben werden
		loadResults(program.getId());
		JsfUtil.setViewVariable("id", program.getId());
		JsfUtil.setViewVariable("filter", null);
	}

	public boolean canDelete(Map<String, String> item) {
		return item.get("announcement.club.name").equals(auth.getLoggedInUser().getClub().getName());
	}

	@Override
	protected void doSave() {
	}

	public Program getProgram() {
		return program;
	}

	public String getFilter() {
		return (String) JsfUtil.getViewVariable("filter");
	}

	public void setFilter(String filter) {
		JsfUtil.setViewVariable("filter", filter);
	}

	private void loadResults(Long id) {
		program = service.getResults(id);
	}
}
