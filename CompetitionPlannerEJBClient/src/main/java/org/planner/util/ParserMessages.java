package org.planner.util;

import java.nio.charset.Charset;
import java.util.ResourceBundle;

public class ParserMessages implements org.planner.util.ExpressionParser.ParserMessages {

	public static final ParserMessages INSTANCE = new ParserMessages();

	private ResourceBundle bundle = ResourceBundle.getBundle("org.planner.ui.util.parser.ParserMessages");

	private ParserMessages() {
	}

	private String getUTF8String(String key) {
		String string = bundle.getString(key);
		return new String(string.getBytes(Charset.forName("ISO-8859-1")), Charset.forName("UTF-8"));
	}

	@Override
	public String unknownVariable(String var) {
		return CommonMessages.formatMessage(getUTF8String("unknownVariable"), var);
	}

	@Override
	public String readonlyVariable(String var) {
		return CommonMessages.formatMessage(getUTF8String("readonlyVariable"), var);
	}

	@Override
	public String aNumber() {
		return CommonMessages.formatMessage(getUTF8String("aNumber"));
	}

	@Override
	public String eof() {
		return CommonMessages.formatMessage(getUTF8String("eof"));
	}

	@Override
	public String parseError(String details, int line, int column, String expectings) {
		return CommonMessages.formatMessage(getUTF8String("parseError"), details, line, column, expectings);
	}

	@Override
	public String unexpectedSymbol(Object symbol) {
		return CommonMessages.formatMessage(getUTF8String("unexpectedSymbol"), symbol);
	}

	@Override
	public String unexpectedEof() {
		return CommonMessages.formatMessage(getUTF8String("unexpectedEof"));
	}

	@Override
	public String unexpectedChar(char c) {
		return CommonMessages.formatMessage(getUTF8String("unexpectedChar"), c);
	}

	@Override
	public String incompatibleRelation(Object left, Object right) {
		return CommonMessages.formatMessage(getUTF8String("incompatibleRelation"), left, right);
	}

	@Override
	public String incompatibleAdd(Object left, Object right) {
		return CommonMessages.formatMessage(getUTF8String("incompatibleAdd"), left, right);
	}

	@Override
	public String incompatibleMultiply(Object left, Object right) {
		return CommonMessages.formatMessage(getUTF8String("incompatibleMultiply"), left, right);
	}

	@Override
	public String incompatibleBoolean(Object val) {
		return CommonMessages.formatMessage(getUTF8String("incompatibleBoolean"), val);
	}

	@Override
	public String invalidNumber(Object num) {
		return CommonMessages.formatMessage(getUTF8String("invalidNumber"), num);
	}

	@Override
	public String or() {
		return CommonMessages.formatMessage(getUTF8String("or"));
	}
}
