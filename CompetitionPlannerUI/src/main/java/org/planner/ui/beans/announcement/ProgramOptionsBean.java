package org.planner.ui.beans.announcement;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Program;
import org.planner.remote.ServiceFacade;
import org.planner.ui.beans.UrlParameters;
import org.planner.ui.util.JsfUtil;
import org.planner.util.ExpressionParser;
import org.planner.util.ParserMessages;

/*
 * existiert nur, damit bei der Suggestion nicht das gesamte Program geladen wird
 */
@Named
@RequestScoped
public class ProgramOptionsBean {

	@Inject
	private ServiceFacade service;

	@Inject
	private UrlParameters urlParameters;

	private Program program;

	public List<String> suggestExpr(String text) {
		return ExpressionParser.getCompletion(text, ParserMessages.INSTANCE);
	}

	public Program getProgram() {
		if (program == null) {
			Long id = urlParameters.getId();
			if (id == null)
				id = (Long) JsfUtil.getViewVariable("id");
			if (id != null)
				program = service.getObject(Program.class, id);
		}
		return program;
	}

	public void setProgram(Program program) {
		this.program = program;
	}
}
