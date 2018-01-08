package org.planner.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;

public class ExpressionParser {
	public static class ExNode extends SimpleNode {
		protected Deque<String> ops;

		public ExNode(Parser p, int i) {
			super(p, i);
		}

		public ExNode(int i) {
			super(i);
		}

		public void addOp(String s) {
			if (ops == null)
				ops = new ArrayDeque<>();
			ops.add(s);
		}

		public String operator() {
			return ops != null ? ops.pop() : null;
		}
	}

	private static class Visitor extends ParserDefaultVisitor {
		private IntegerProperty doGetProperty(String name, Object data) {
			@SuppressWarnings("unchecked")
			IntegerProperty property = ((Map<String, IntegerProperty>) data).get(name);
			if (property == null)
				throw new FachlicheException(CommonMessages.getResourceBundle(), "exprParser.unknownVariable", name);
			return property;
		}

		private Integer getProperty(String name, Object data) {
			IntegerProperty property = doGetProperty(name, data);
			return property.value;
		}

		private void setProperty(String name, int value, Object data) {
			IntegerProperty property = doGetProperty(name, data);
			if (property.readonly)
				throw new FachlicheException(CommonMessages.getResourceBundle(), "exprParser.readonlyVariable", name);
			property.value = value;
		}

		@Override
		public Object defaultVisit(SimpleNode node, Object data) {
			System.err.println("unhandled visit: " + node);
			return super.defaultVisit(node, data);
		}

		@Override
		public Object visit(Start node, Object data) {
			return super.defaultVisit(node, data);
		}

		@Override
		public Object visit(IfElse node, Object data) {
			Object resultFromIf = node.children[0].jjtAccept(this, data);
			if (Boolean.FALSE.equals(resultFromIf) && node.jjtGetNumChildren() > 1)
				node.children[1].jjtAccept(this, data);
			return resultFromIf;
		}

		@Override
		public Object visit(If node, Object data) {
			Object result = node.children[0].jjtAccept(this, data);
			if (!(result instanceof Boolean))
				throw new FachlicheException(CommonMessages.getResourceBundle(), "exprParser.incompatibleBoolean",
						getNodeValue(node.children[0]));
			if ((Boolean) result)
				node.children[1].jjtAccept(this, data);
			return result;
		}

		@Override
		public Object visit(Or node, Object data) {
			boolean result = false;
			for (int i = 0; i < node.jjtGetNumChildren(); i++) {
				Object o = node.children[i].jjtAccept(this, data);
				result |= o instanceof Boolean && ((Boolean) o).booleanValue();
			}
			return result;
		}

		@Override
		public Object visit(And node, Object data) {
			boolean result = true;
			for (int i = 0; i < node.jjtGetNumChildren(); i++) {
				Object o = node.children[i].jjtAccept(this, data);
				result &= o instanceof Boolean && ((Boolean) o).booleanValue();
			}
			return result;
		}

		@Override
		public Object visit(Equals node, Object data) {
			Object o1 = node.children[0].jjtAccept(this, data);
			Object o2 = node.children[1].jjtAccept(this, data);
			boolean result = equality(o1, o2, node.operator());
			for (int i = 2; i < node.jjtGetNumChildren(); i++) {
				Object o = node.children[i].jjtAccept(this, data);
				result = equality(result, o, node.operator());
			}
			return result;
		}

		private boolean equality(Object o1, Object o2, String op) {
			if (o1.getClass() != o2.getClass())
				throw new FachlicheException(CommonMessages.getResourceBundle(), "exprParser.incompatibleRelation", o1,
						o2);
			switch (op) {
			default:
				throw new IllegalArgumentException();
			case "=":
				return o1 == o2 || o1 != null && o1.equals(o2);
			case "<>":
				return o1 != o2 || o1 != null && !o1.equals(o2);
			}
		}

		@Override
		public Object visit(Relation node, Object data) {
			Object o1 = node.children[0].jjtAccept(this, data);
			Object o2 = node.children[1].jjtAccept(this, data);
			boolean result = comparison(o1, o2, node.operator());
			for (int i = 2; i < node.jjtGetNumChildren(); i++) {
				Object o = node.children[i].jjtAccept(this, data);
				result = comparison(result, o, node.operator());
			}
			return result;
		}

		private boolean comparison(Object o1, Object o2, String op) {
			if (!(o1 instanceof Integer && o2 instanceof Integer))
				throw new FachlicheException(CommonMessages.getResourceBundle(), "exprParser.incompatibleRelation", o1,
						o2);
			switch (op) {
			default:
				throw new IllegalArgumentException();
			case "<":
				return ((Integer) o1).compareTo((Integer) o2) < 0;
			case ">":
				return ((Integer) o1).compareTo((Integer) o2) > 0;
			case "<=":
				return ((Integer) o1).compareTo((Integer) o2) <= 0;
			case ">=":
				return ((Integer) o1).compareTo((Integer) o2) >= 0;
			}
		}

		@Override
		public Object visit(Add node, Object data) {
			Object o1 = node.children[0].jjtAccept(this, data);
			Object o2 = node.children[1].jjtAccept(this, data);
			int result = addition(o1, o2, node.operator());
			for (int i = 2; i < node.jjtGetNumChildren(); i++) {
				Object o = node.children[i].jjtAccept(this, data);
				result = addition(result, o, node.operator());
			}
			return result;
		}

		private int addition(Object o1, Object o2, String op) {
			if (!(o1 instanceof Integer) || !(o2 instanceof Integer))
				throw new FachlicheException(CommonMessages.getResourceBundle(), "exprParser.incompatibleAdd", o1, o2);
			switch (op) {
			default:
				throw new IllegalArgumentException();
			case "+":
				return ((Integer) o1) + ((Integer) o2);
			case "-":
				return ((Integer) o1) - ((Integer) o2);
			}
		}

		@Override
		public Object visit(Multiply node, Object data) {
			Object o1 = node.children[0].jjtAccept(this, data);
			Object o2 = node.children[1].jjtAccept(this, data);
			int result = multiplication(o1, o2, node.operator());
			for (int i = 2; i < node.jjtGetNumChildren(); i++) {
				Object o = node.children[i].jjtAccept(this, data);
				result = multiplication(result, o, node.operator());
			}
			return result;
		}

		private int multiplication(Object o1, Object o2, String op) {
			if (!(o1 instanceof Integer && o2 instanceof Integer))
				throw new FachlicheException(CommonMessages.getResourceBundle(), "exprParser.incompatibleMultiply", o1,
						o2);
			switch (op) {
			default:
				throw new IllegalArgumentException();
			case "*":
				return ((Integer) o1).intValue() * ((Integer) o2).intValue();
			case "/":
				return ((Integer) o1).intValue() / ((Integer) o2).intValue();
			case "%":
				return ((Integer) o1).intValue() % ((Integer) o2).intValue();
			}
		}

		@Override
		public Object visit(Parenthesized node, Object data) {
			return node.children[0].jjtAccept(this, data);
		}

		@Override
		public Object visit(Assignments node, Object data) {
			return super.defaultVisit(node, data);
		}

		@Override
		public Object visit(Assign node, Object data) {
			String name = getNodeValue(node.children[0]).toString();
			Integer value = (Integer) node.children[1].jjtAccept(this, data);
			setProperty(name, value, data);
			return value;
		}

		@Override
		public Object visit(Identifier node, Object data) {
			return getProperty(node.jjtGetValue().toString(), data);
		}

		@Override
		public Object visit(IntegerLiteral node, Object data) {
			try {
				return Integer.valueOf(node.jjtGetValue().toString());
			} catch (NumberFormatException e) {
				throw new FachlicheException(CommonMessages.getResourceBundle(), "exprParser.invalidNumber",
						node.jjtGetValue());
			}
		}

		private Object getNodeValue(Node node) {
			return ((SimpleNode) node).jjtGetValue();
		}
	}

	private static class IntegerProperty {
		private int value;
		private boolean readonly;

		private IntegerProperty(int value, boolean readonly) {
			this.value = value;
			this.readonly = readonly;
		}

		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}

	public static class ExpressionException extends FachlicheException {
		private static final long serialVersionUID = 1L;

		private List<String> expectings;

		public ExpressionException(ResourceBundle bundle, String key, List<String> expectings, Object... args) {
			super(bundle, key, args);
			this.expectings = expectings;
		}

		public List<String> getExpectings() {
			return expectings;
		}
	}

	public static void main(String args[]) throws Exception {
		InputStream in = args.length > 0 ? new FileInputStream(args[0]) : System.in;
		Parser t = new Parser(in);
		try {
			SimpleNode n = t.Start();
			Visitor visitor = new Visitor();
			Map<String, IntegerProperty> props = new HashMap<>();
			props.put("AnzahlBahnen", new IntegerProperty(9, true));
			props.put("AnzahlMeldungen", new IntegerProperty(12, true));
			props.put("InDenZwischenLauf", new IntegerProperty(0, false));
			props.put("InDenEndlauf", new IntegerProperty(0, false));
			n.jjtAccept(visitor, props);
			System.out.println(props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<String> getCompletion(String expr) {
		Parser parser = new Parser(new java.io.StringReader(expr));
		List<String> expectings = new ArrayList<>();
		try {
			parser.Completion();
			throw parser.generateParseException();
		} catch (ParseException e) {
			createExpectings(expectings, e, false, parser.inAssignment);
		} catch (TokenMgrError e) {
		}
		return expectings;
	}

	private static void createExpectings(List<String> expectings, ParseException e, boolean withSpecialTokens,
			boolean inAssignment) {
		for (int[] is : e.expectedTokenSequences) {
			int tok = is[0];
			if (tok == ParserConstants.INTEGER_LITERAL) {
				if (withSpecialTokens)
					expectings.add(CommonMessages.getMessage("exprParser.aNumber"));
			} else if (tok == ParserConstants.IDENTIFIER) {
				if (inAssignment) {
					expectings.add("InDenEndlauf");
					expectings.add("InDenZwischenLauf");
				} else {
					expectings.add("AnzahlBahnen");
					expectings.add("AnzahlMeldungen");
				}
			} else if (tok == ParserConstants.EOF) {
				if (withSpecialTokens)
					expectings.add(CommonMessages.getMessage("exprParser.eof"));
			} else {
				String s = e.tokenImage[tok];
				s = s.substring(1, s.length() - 1);
				if (withSpecialTokens)
					s = "\"" + s + "\"";
				expectings.add(s);
			}
		}
	}

	public static String createExpectingsMessage(List<String> expectings) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < expectings.size(); i++) {
			String expecting = expectings.get(i);
			if (sb.length() > 0) {
				if (i == expectings.size() - 1)
					sb.append(" ").append(CommonMessages.getMessage("or")).append(" ");
				else
					sb.append(", ");
			}
			sb.append(expecting);
		}
		return sb.toString();
	}

	private int intoSemiFinal;
	private int intoFinal;

	public void evaluateExpression(String expr, int numTeams, int numLanes) {
		Parser parser = new Parser(new java.io.StringReader(expr));
		try {
			SimpleNode ast = parser.Start();
			Map<String, IntegerProperty> props = new HashMap<>();
			IntegerProperty AnzahlBahnen = new IntegerProperty(numLanes, true);
			IntegerProperty AnzahlMeldungen = new IntegerProperty(numTeams, true);
			IntegerProperty InDenZwischenLauf = new IntegerProperty(0, false);
			IntegerProperty InDenEndlauf = new IntegerProperty(0, false);
			props.put("AnzahlBahnen", AnzahlBahnen);
			props.put("AnzahlMeldungen", AnzahlMeldungen);
			props.put("InDenZwischenLauf", InDenZwischenLauf);
			props.put("InDenEndlauf", InDenEndlauf);
			ast.jjtAccept(new Visitor(), props);
			intoFinal = InDenEndlauf.value;
			intoSemiFinal = InDenZwischenLauf.value;
		} catch (ParseException e) {
			List<String> expectings = new ArrayList<>();
			createExpectings(expectings, e, true, parser.inAssignment);
			Token token = e.currentToken.next;
			String detailMsg = CommonMessages.getFormattedMessage("exprParser.unexpectedSymbol", token);
			if (token.kind == ParserConstants.EOF)
				detailMsg = CommonMessages.getMessage("exprParser.unexpectedEof");
			throw new ExpressionException(CommonMessages.getResourceBundle(), "exprParser.parseError", expectings,
					detailMsg, token.beginLine, token.beginColumn, createExpectingsMessage(expectings));
		} catch (TokenMgrError e) {
			throw new ExpressionException(CommonMessages.getResourceBundle(), "exprParser.unexpectedChar", null,
					(char) parser.token_source.curChar);
		} catch (Exception e) {
			if (e instanceof FachlicheException)
				throw e;
			throw new TechnischeException(e.getMessage(), e);
		}
	}

	public int getIntoSemiFinal() {
		return intoSemiFinal;
	}

	public int getIntoFinal() {
		return intoFinal;
	}
}
