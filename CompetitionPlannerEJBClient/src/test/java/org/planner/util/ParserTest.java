package org.planner.util;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;
import org.planner.util.ExpressionParser.ExpressionException;

import junit.framework.Assert;

public class ParserTest {

	@Test
	public void equals() throws Exception {
		String s = " Wenn AnzahlMeldungen =1 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 1, 0, 1, 2);
		assertEquals(s, 3, 0, 0, 0);

		s = "Wenn 1 = 1 <> (1 = 0) Dann InDenEndlauf = 7";
		assertEquals(s, 0, 0, 7, 0);
	}

	@Test
	public void equalsElse() throws Exception {
		String s = " Wenn AnzahlMeldungen =1 Dann InDenEndlauf=1, InDenZwischenLauf=2 ansonsten InDenEndlauf=5, InDenZwischenLauf=6 ";
		assertEquals(s, 1, 0, 1, 2);
		assertEquals(s, 3, 0, 5, 6);
	}

	@Test
	public void noEquals() throws Exception {
		String s = " Wenn AnzahlMeldungen <> 1 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 2, 0, 1, 2);
	}

	@Test
	public void lessThan() throws Exception {
		String s = " Wenn AnzahlMeldungen < 3 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 2, 0, 1, 2);
	}

	@Test
	public void lessThanEquals() throws Exception {
		String s = " Wenn AnzahlMeldungen <= 3 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 3, 0, 1, 2);
		assertEquals(s, 2, 0, 1, 2);
	}

	@Test
	public void lessThanFail() throws Exception {
		String s = " Wenn (AnzahlMeldungen und 1) <1 Dann InDenEndlauf=1, InDenZwischenLauf=2 ansonsten InDenEndlauf=5, InDenZwischenLauf=6 ";
		assertFail(s, 1, 0);
	}

	@Test
	public void greaterThan() throws Exception {
		String s = " Wenn AnzahlMeldungen > 3 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 4, 0, 1, 2);
	}

	@Test
	public void greaterThanEquals() throws Exception {
		String s = " Wenn AnzahlMeldungen >= 5 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 5, 0, 1, 2);
		assertEquals(s, 6, 0, 1, 2);
	}

	@Test
	public void addition() throws Exception {
		String s = " Wenn AnzahlMeldungen <= 2 + AnzahlBahnen Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 5, 9, 1, 2);

		s = " Wenn 2 - 3 + 7 + 5 = 11 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 5, 9, 1, 2);
	}

	@Test
	public void substraction() throws Exception {
		String s = " Wenn AnzahlMeldungen > 2 - AnzahlBahnen Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 5, 9, 1, 2);
	}

	@Test
	public void multiplication() throws Exception {
		String s = " Wenn AnzahlMeldungen = 3 * AnzahlBahnen Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 27, 9, 1, 2);

		s = " Wenn 3 * 7 / 3 * 3 = 21 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 27, 9, 1, 2);
	}

	@Test
	public void division() throws Exception {
		String s = " Wenn AnzahlMeldungen <= AnzahlBahnen / 3 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 2, 9, 1, 2);
	}

	@Test
	public void modulus() throws Exception {
		String s = " Wenn AnzahlMeldungen = AnzahlBahnen%2 Dann InDenEndlauf=1, InDenZwischenLauf=2";
		assertEquals(s, 1, 9, 1, 2);
	}

	@Test
	public void parenthesized() throws Exception {
		String s = " Wenn 1 + 8 + 9 = 18 Dann InDenEndlauf=1,InDenZwischenLauf=3";
		assertEquals(s, 27, 7, 1, 3);
	}

	@Test
	public void relation() throws Exception {
		String s = "Wenn 5 > 3 und (2 < 10)  Dann InDenEndlauf=1";
		assertEquals(s, 0, 0, 1, 0);

		s = "Wenn 5 > 3 > 2 < 10  Dann InDenEndlauf=1";
		assertFail(s, 1, 1);
	}

	@Test
	public void wrongVariable() throws Exception {
		String s = "Wenn test = 7 Dann InDenEndlauf=1";
		assertFail(s, 1, 1);
	}

	@Test
	public void tooLargeNumber() throws Exception {
		String s = "Wenn test = 7 Dann InDenEndlauf = " + Long.MAX_VALUE;
		assertFail(s, 1, 1);
	}

	@Test
	public void elseStatement() throws Exception {
		String s = "Wenn 1 > 2 Dann InDenEndlauf=1 ansonsten InDenEndlauf=7, InDenZwischenLauf=6";
		assertEquals(s, 0, 0, 7, 6);

		s = "Wenn 1 < 2 Dann InDenEndlauf=1 ansonsten InDenEndlauf=7, InDenZwischenLauf=6 wenn 2 < 3 dann InDenZwischenLauf=2";
		assertEquals(s, 0, 0, 1, 2);
	}

	@Test
	public void elseIfStatement() throws Exception {
		String s = "Wenn 1 > 2 Dann InDenEndlauf=1 ansonsten wenn 2 < 3 dann InDenEndlauf=7, InDenZwischenLauf=6";
		assertEquals(s, 0, 0, 7, 6);
	}

	@Test
	public void regression28Registrations() throws Exception {
		String s = readRessource("defaultExpression");
		assertEquals(s, 28, 9, 3, 6);
	}

	private void assertEquals(String s, int numTeams, int numLanes, int intoFinal, int intoSemiFinal) throws Exception {
		ExpressionParser parser = new ExpressionParser(ParserMessages.INSTANCE);
		parser.evaluateExpression(s, numTeams, numLanes);
		Assert.assertEquals(intoFinal, parser.getIntoFinal());
		Assert.assertEquals(intoSemiFinal, parser.getIntoSemiFinal());
	}

	private void assertFail(String s, int numTeams, int numLanes) throws Exception {
		ExpressionParser parser = new ExpressionParser(ParserMessages.INSTANCE);
		try {
			parser.evaluateExpression(s, numTeams, numLanes);
			Assert.fail("keine exception");
		} catch (ExpressionException e) {
		}
	}

	private String readRessource(String name) throws Exception {
		InputStream in = ClassLoader.getSystemResourceAsStream(name);
		StringBuilder sb = new StringBuilder();
		InputStreamReader r = new InputStreamReader(in, "UTF8");
		char[] buf = new char[1000];
		for (int c; (c = r.read(buf)) != -1;)
			sb.append(buf, 0, c);
		r.close();
		return sb.toString();
	}
}
