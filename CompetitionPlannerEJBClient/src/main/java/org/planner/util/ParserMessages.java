package org.planner.util;

import java.util.ResourceBundle;

public class ParserMessages implements org.planner.util.ExpressionParser.ParserMessages {

	public static final ParserMessages INSTANCE = new ParserMessages();

	private ResourceBundle bundle = ResourceBundle.getBundle("org.planner.ui.util.parser.ParserMessages");

	private ParserMessages() {
	}

	@Override
	public String unknownVariable(String var) {
		return CommonMessages.formatMessage(bundle.getString("unknownVariable"), var);
	}

	@Override
	public String readonlyVariable(String var) {
		return CommonMessages.formatMessage(bundle.getString("readonlyVariable"), var);
	}

	@Override
	public String aNumber() {
		return CommonMessages.formatMessage(bundle.getString("aNumber"));
	}

	@Override
	public String eof() {
		return CommonMessages.formatMessage(bundle.getString("eof"));
	}

	@Override
	public String parseError(String details, int line, int column, String expectings) {
		return CommonMessages.formatMessage(bundle.getString("parseError"), details, line, column, expectings);
	}

	@Override
	public String unexpectedSymbol(Object symbol) {
		return CommonMessages.formatMessage(bundle.getString("unexpectedSymbol"), symbol);
	}

	@Override
	public String unexpectedEof() {
		return CommonMessages.formatMessage(bundle.getString("unexpectedEof"));
	}

	@Override
	public String unexpectedChar(char c) {
		return CommonMessages.formatMessage(bundle.getString("unexpectedChar"), c);
	}

	@Override
	public String incompatibleRelation(Object left, Object right) {
		return CommonMessages.formatMessage(bundle.getString("incompatibleRelation"), left, right);
	}

	@Override
	public String incompatibleAdd(Object left, Object right) {
		return CommonMessages.formatMessage(bundle.getString("incompatibleAdd"), left, right);
	}

	@Override
	public String incompatibleMultiply(Object left, Object right) {
		return CommonMessages.formatMessage(bundle.getString("incompatibleMultiply"), left, right);
	}

	@Override
	public String incompatibleBoolean(Object val) {
		return CommonMessages.formatMessage(bundle.getString("incompatibleBoolean"), val);
	}

	@Override
	public String invalidNumber(Object num) {
		return CommonMessages.formatMessage(bundle.getString("invalidNumber"), num);
	}

	@Override
	public String or() {
		return CommonMessages.formatMessage(bundle.getString("or"));
	}
}
