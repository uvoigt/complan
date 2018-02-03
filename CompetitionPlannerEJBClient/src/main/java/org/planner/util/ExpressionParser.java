package org.planner.util;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		private final ParserMessages messages;

		public Visitor(ParserMessages messages) {
			this.messages = messages;
		}

		private IntegerProperty doGetProperty(String name, Object data) {
			@SuppressWarnings("unchecked")
			IntegerProperty property = ((Map<String, IntegerProperty>) data).get(name);
			if (property == null)
				throw new RuntimeException(messages.unknownVariable(name));
			return property;
		}

		private Integer getProperty(String name, Object data) {
			IntegerProperty property = doGetProperty(name, data);
			return property.value;
		}

		private void setProperty(String name, int value, Object data) {
			IntegerProperty property = doGetProperty(name, data);
			if (property.readonly)
				throw new RuntimeException(messages.readonlyVariable(name));
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
				throw new RuntimeException(messages.incompatibleBoolean(getNodeValue(node.children[0])));
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
				throw new RuntimeException(messages.incompatibleRelation(o1, o2));
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
				throw new RuntimeException(messages.incompatibleRelation(o1, o2));
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
				throw new RuntimeException(messages.incompatibleAdd(o1, o2));
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
				throw new RuntimeException(messages.incompatibleMultiply(o1, o2));
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
				throw new RuntimeException(messages.invalidNumber(node.jjtGetValue()));
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

	public static class ExpressionException extends RuntimeException {
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

	public interface ParserMessages {
		String unknownVariable(String var);

		String readonlyVariable(String var);

		String aNumber();

		String eof();

		String parseError(String details, int line, int column, String expectings);

		String unexpectedSymbol(Object symbol);

		String unexpectedEof();

		String unexpectedChar(char c);

		String incompatibleRelation(Object left, Object right);

		String incompatibleAdd(Object left, Object right);

		String incompatibleMultiply(Object left, Object right);

		String incompatibleBoolean(Object val);

		String invalidNumber(Object num);

		String or();
	}

	public static List<String> getCompletion(String expr, ParserMessages messages) {
		Parser parser = new Parser(new StringReader(expr));
		List<String> expectings = new ArrayList<>();
		try {
			parser.Completion();
			throw parser.generateParseException();
		} catch (ParseException e) {
			createExpectings(expectings, e, false, parser.inAssignment, messages);
		} catch (TokenMgrError e) {
		}
		return expectings;
	}

	private static void createExpectings(List<String> expectings, ParseException e, boolean withSpecialTokens,
			boolean inAssignment, ParserMessages messages) {
		for (int[] is : e.expectedTokenSequences) {
			int tok = is[0];
			if (tok == ParserConstants.INTEGER_LITERAL) {
				if (withSpecialTokens)
					expectings.add(messages.aNumber());
			} else if (tok == ParserConstants.IDENTIFIER) {
				if (inAssignment) {
					expectings.add("DirektInDenEndlauf");
					expectings.add("InDenZwischenLauf");
					expectings.add("InDenEndlauf");
				} else {
					expectings.add("AnzahlBahnen");
					expectings.add("AnzahlMeldungen");
				}
			} else if (tok == ParserConstants.EOF) {
				if (withSpecialTokens)
					expectings.add(messages.eof());
			} else {
				String s = e.tokenImage[tok];
				s = s.substring(1, s.length() - 1);
				if (withSpecialTokens)
					s = "\"" + s + "\"";
				expectings.add(s);
			}
		}
	}

	private String createExpectingsMessage(List<String> expectings) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < expectings.size(); i++) {
			String expecting = expectings.get(i);
			if (sb.length() > 0) {
				if (i == expectings.size() - 1)
					sb.append(" ").append(messages.or()).append(" ");
				else
					sb.append(", ");
			}
			sb.append(expecting);
		}
		return sb.toString();
	}

	private int directlyIntoFinal;
	private int intoSemiFinal;
	private int intoFinal;

	private final ParserMessages messages;

	public ExpressionParser(ParserMessages messages) {
		this.messages = messages;
	}

	public void evaluateExpression(String expr, int numTeams, int numLanes) throws ExpressionException {
		Parser parser = new Parser(new StringReader(expr));
		try {
			SimpleNode ast = parser.Start();
			Map<String, IntegerProperty> props = new HashMap<>();

			IntegerProperty AnzahlBahnen = new IntegerProperty(numLanes, true);
			IntegerProperty AnzahlMeldungen = new IntegerProperty(numTeams, true);
			IntegerProperty DirektInDenEndlauf = new IntegerProperty(0, false);
			IntegerProperty InDenZwischenLauf = new IntegerProperty(0, false);
			IntegerProperty InDenEndlauf = new IntegerProperty(0, false);

			props.put("AnzahlBahnen", AnzahlBahnen);
			props.put("AnzahlMeldungen", AnzahlMeldungen);
			props.put("DirektInDenEndlauf", DirektInDenEndlauf);
			props.put("InDenZwischenLauf", InDenZwischenLauf);
			props.put("InDenEndlauf", InDenEndlauf);

			ast.jjtAccept(new Visitor(messages), props);
			directlyIntoFinal = DirektInDenEndlauf.value;
			intoSemiFinal = InDenZwischenLauf.value;
			intoFinal = InDenEndlauf.value;

		} catch (ParseException e) {
			List<String> expectings = new ArrayList<>();
			createExpectings(expectings, e, true, parser.inAssignment, messages);
			Token token = e.currentToken.next;
			String detailMsg = messages.unexpectedSymbol(token);
			if (token.kind == ParserConstants.EOF)
				detailMsg = messages.unexpectedEof();
			throw new ExpressionException(messages.parseError(detailMsg, token.beginLine, token.beginColumn,
					createExpectingsMessage(expectings)), expectings);
		} catch (TokenMgrError e) {
			throw new ExpressionException(messages.unexpectedChar((char) parser.token_source.curChar), null);
		} catch (Exception e) {
			throw new ExpressionException(e.getMessage(), null);
		}
	}

	public int getDirectlyIntoFinal() {
		return directlyIntoFinal;
	}

	public int getIntoSemiFinal() {
		return intoSemiFinal;
	}

	public int getIntoFinal() {
		return intoFinal;
	}
}
