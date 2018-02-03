package org.planner.ui.util.parser;

import org.planner.util.ExpressionParser;

import com.google.gwt.core.client.EntryPoint;

public class Parser implements EntryPoint {

	@Override
	public void onModuleLoad() {
		exportParser();
	}

	public static String evaluateExpression(String expr, int numTeams, int numLanes) {
		try {
			ExpressionParser parser = new ExpressionParser(ParserMessages.INSTANCE);
			parser.evaluateExpression(expr, numTeams, numLanes);
			return "";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public static native void exportParser()
	/*-{
		$wnd.evaluateExpression = @org.planner.ui.util.parser.Parser::evaluateExpression(*);
	}-*/;
}
