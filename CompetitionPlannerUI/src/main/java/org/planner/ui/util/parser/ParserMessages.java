package org.planner.ui.util.parser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ParserMessages extends org.planner.util.ExpressionParser.ParserMessages, Messages {

	ParserMessages INSTANCE = GWT.create(ParserMessages.class);

	@Override
	String unknownVariable(String var);

	@Override
	String readonlyVariable(String var);

	@Override
	String aNumber();

	@Override
	String eof();

	@Override
	String parseError(String details, int line, int column, String expectings);

	@Override
	String unexpectedSymbol(Object symbol);

	@Override
	String unexpectedEof();

	@Override
	String unexpectedChar(char c);

	@Override
	String incompatibleRelation(Object left, Object right);

	@Override
	String incompatibleAdd(Object left, Object right);

	@Override
	String incompatibleMultiply(Object left, Object right);

	@Override
	String incompatibleBoolean(Object val);

	@Override
	String invalidNumber(Object num);

	@Override
	String or();
}
