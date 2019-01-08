package org.planner.ui.beans.announcement;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.event.ComponentSystemEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Program;
import org.planner.eo.Program.ProgramStatus;
import org.planner.eo.ProgramRace;
import org.planner.eo.Result;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.util.JsfUtil;

@Named
@RequestScoped
public class ResultBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	@Inject
	private RenderBean renderBean;

	private Program program;

	private boolean showEmpty;

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
		Result result = (Result) item;
		// das könnte auch als Argument in der search-xhtml mitgegeben werden
		loadResults(result.getProgramId());
		JsfUtil.setViewVariable("id", program.getId());
		JsfUtil.setViewVariable("filter", null);
	}

	public boolean canDelete(Map<String, Object> item) {
		return item.get("cName").equals(auth.getLoggedInUser().getClub().getName())
				&& item.get("status") == ProgramStatus.created;
	}

	@Override
	protected void doSave() {
	}

	public Program getProgram() {
		return program;
	}

	public boolean isShowEmpty() {
		return showEmpty;
	}

	public void setShowEmpty(boolean showEmpty) {
		this.showEmpty = showEmpty;
	}

	public String getFilter() {
		return (String) JsfUtil.getViewVariable("filter");
	}

	public void setFilter(String filter) {
		JsfUtil.setViewVariable("filter", filter);
	}

	/*
	 * Diese Variante des Filterns (lazy=true und anstelle filterBy="#{renderBean.getRaceFilter(race)}"
	 * filterFunction="#{renderBean.filterRaces}" diese Lösung dient dem Vermeiden von setValue(filteredValue).
	 */
	public void onPrerenderTable(@SuppressWarnings("unused") ComponentSystemEvent event) {
		String filter = getFilter();
		if (program != null && (!showEmpty || filter != null)) {
			for (Iterator<ProgramRace> it = program.getRaces().iterator(); it.hasNext();) {
				ProgramRace race = it.next();
				boolean noResults = !showEmpty && (race.getPlacements() == null || race.getPlacements().isEmpty());
				boolean filtered = filter != null
						&& !renderBean.filterRaces(renderBean.getRaceFilter(race), filter, Locale.getDefault());
				if (noResults || filtered)
					it.remove();
			}
		}
	}

	private void loadResults(Long programId) {
		program = service.getProgram(programId, true, true);
	}
}
