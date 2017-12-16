package org.planner.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionParser {
	private static class Visitor extends ParserDefaultVisitor {
		private IntegerProperty doGetProperty(String name, Object data) {
			@SuppressWarnings("unchecked")
			IntegerProperty property = ((Map<String, IntegerProperty>) data).get(name);
			if (property == null)
				throw new IllegalArgumentException("Keine bekannte Variable: " + name); // TODO fachliche
			return property;
		}

		private Integer getProperty(String name, Object data) {
			IntegerProperty property = doGetProperty(name, data);
			return property.value;
		}

		private void setProperty(String name, int value, Object data) {
			IntegerProperty property = doGetProperty(name, data);
			if (property.readonly)
				throw new IllegalArgumentException("Die Variable " + name + " ist nicht änderbar"); // TODO
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
		public Object visit(EfElse node, Object data) {
			Boolean ifWasTrue = (Boolean) node.jjtGetChild(0).jjtAccept(this, data);
			if (!ifWasTrue)
				node.jjtGetChild(1).jjtAccept(this, data);
			return ifWasTrue;
		}

		@Override
		public Object visit(If node, Object data) {
			Object result = node.jjtGetChild(0).jjtAccept(this, data);
			if (Boolean.TRUE.equals(result))
				node.jjtGetChild(1).jjtAccept(this, data);
			return result;
		}

		@Override
		public Object visit(Or node, Object data) {
			Object o1 = node.jjtGetChild(0).jjtAccept(this, data);
			Object o2 = node.jjtGetChild(1).jjtAccept(this, data);
			return o1 instanceof Boolean && o2 instanceof Boolean && ((Boolean) o1).booleanValue()
					|| ((Boolean) o2).booleanValue();
		}

		@Override
		public Object visit(And node, Object data) {
			Object o1 = node.jjtGetChild(0).jjtAccept(this, data);
			Object o2 = node.jjtGetChild(1).jjtAccept(this, data);
			return o1 instanceof Boolean && o2 instanceof Boolean && ((Boolean) o1).booleanValue()
					&& ((Boolean) o2).booleanValue();
		}

		@Override
		public Object visit(Equals node, Object data) {
			Object o1 = node.jjtGetChild(0).jjtAccept(this, data);
			Object o2 = node.jjtGetChild(1).jjtAccept(this, data);
			switch ((String) node.jjtGetValue()) {
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
			Object o1 = node.jjtGetChild(0).jjtAccept(this, data);
			Object o2 = node.jjtGetChild(1).jjtAccept(this, data);
			if (!(o1 instanceof Integer && o2 instanceof Integer))
				throw new IllegalArgumentException();
			switch ((String) node.jjtGetValue()) {
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
			Object o1 = node.jjtGetChild(0).jjtAccept(this, data);
			Object o2 = node.jjtGetChild(1).jjtAccept(this, data);
			if (!(o1 instanceof Integer && o2 instanceof Integer))
				throw new IllegalArgumentException();
			switch ((String) node.jjtGetValue()) {
			default:
				throw new IllegalArgumentException();
			case "+":
				return ((Integer) o1).intValue() + ((Integer) o2).intValue();
			case "-":
				return ((Integer) o1).intValue() - ((Integer) o2).intValue();
			}
		}

		@Override
		public Object visit(Multiply node, Object data) {
			Object o1 = node.jjtGetChild(0).jjtAccept(this, data);
			Object o2 = node.jjtGetChild(1).jjtAccept(this, data);
			if (!(o1 instanceof Integer && o2 instanceof Integer))
				throw new IllegalArgumentException();
			switch ((String) node.jjtGetValue()) {
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
			return node.jjtGetChild(0).jjtAccept(this, data);
		}

		@Override
		public Object visit(Assignments node, Object data) {
			return super.defaultVisit(node, data);
		}

		@Override
		public Object visit(Assign node, Object data) {
			String name = getNodeValue(node.jjtGetChild(0)).toString();
			Integer value = Integer.valueOf(getNodeValue(node.jjtGetChild(1)).toString());
			setProperty(name, value, data);
			return value;
		}

		@Override
		public Object visit(Identifier node, Object data) {
			return getProperty(node.jjtGetValue().toString(), data);
		}

		@Override
		public Object visit(IntegerLiteral node, Object data) {
			return Integer.valueOf(node.jjtGetValue().toString());
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

	public static class ExpressionException extends Exception {
		private static final long serialVersionUID = 1L;

		private List<String> expectings;

		public ExpressionException(String message, List<String> expectings) {
			super(message);
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
			parser.Start();
			throw parser.generateParseException();
		} catch (ParseException e) {
			createExpectings(expectings, e, false);
		} catch (TokenMgrError e) {
		}
		return expectings;
	}

	private static void createExpectings(List<String> expectings, ParseException e, boolean withInteger) {
		for (int[] is : e.expectedTokenSequences) {
			int tok = is[0];
			if (tok == ParserConstants.INTEGER_LITERAL) {
				if (withInteger)
					expectings.add("eine Zahl");
			} else if (tok == ParserConstants.IDENTIFIER) {
				if (e.currentToken.kind == ParserConstants.DANN) {
					expectings.add("InDenEndlauf");
					expectings.add("InDenZwischenLauf");
				} else {
					expectings.add("AnzahlBahnen");
					expectings.add("AnzahlMeldungen");
				}
			} else {
				String s = e.tokenImage[tok];
				s = s.substring(1, s.length() - 1);
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

	public static void evaluateExpression(String expr) throws ExpressionException {
		new ExpressionParser().evaluateExpression(expr, 0, 0);
	}

	public void evaluateExpression(String expr, int numTeams, int numLanes) throws ExpressionException {
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
			createExpectings(expectings, e, true);
			Token token = e.currentToken.next;
			String msg = "Unerwartetes Symbol \"" + token + "\" in Zeile " + token.beginLine + ", Spalte "
					+ token.beginColumn;
			if (token.kind == ParserConstants.EOF)
				msg = "Unerwartetes Ende der Eingabe";
			throw new ExpressionException(
					msg + ". An dieser Stelle wird " + createExpectingsMessage(expectings) + " erwartet.", expectings); // TODO
		} catch (TokenMgrError e) {
			throw new ExpressionException(
					"Zeichen wird nicht unterstützt: \"" + (char) parser.token_source.curChar + "\"", null); // TODO
		} catch (Exception e) {
			throw new ExpressionException(e.getMessage(), null); // TODO
		}
	}

	public int getIntoSemiFinal() {
		return intoSemiFinal;
	}

	public int getIntoFinal() {
		return intoFinal;
	}
}
