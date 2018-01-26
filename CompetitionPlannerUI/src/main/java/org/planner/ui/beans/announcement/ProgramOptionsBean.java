package org.planner.ui.beans.announcement;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Program;
import org.planner.remote.ServiceFacade;
import org.planner.ui.util.JsfUtil;
import org.planner.util.ExpressionParser;
import org.planner.util.LogUtil.FachlicheException;

@Named
@RequestScoped
public class ProgramOptionsBean {

	@Inject
	private ServiceFacade service;

	private Program program;

	private String exprStatus;

	@PostConstruct
	public void init() {
		Long id = (Long) JsfUtil.getViewVariable("id");
		if (id != null)
			program = service.getObject(Program.class, id, 0);
	}

	public List<String> suggestExpr(String text) {
		return ExpressionParser.getCompletion(text);
	}

	public void checkExpr() {
		exprStatus = null;
		String expr = program.getOptions().getExpr();
		if (expr != null) {
			try {
				new ExpressionParser().evaluateExpression(expr, 0, 9); // TODO
			} catch (FachlicheException e) {
				exprStatus = e.getMessage();
			}
		}
	}

	public String getExprStatus() {
		return exprStatus;
	}

	public Program getProgram() {
		return program;
	}

	public void setProgram(Program program) {
		this.program = program;
	}
}
