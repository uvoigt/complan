package org.planner.util;

import org.junit.Test;
import org.planner.util.ExpressionParser;

import junit.framework.Assert;

public class ParserTests {

	@Test
	public void expressions() throws Exception {
		String s = " Wenn AnzahlMeldungen =1 Dann InDenEndlauf=1,InDenZwischenLauf=0";
		evaluate(s, 1, 9, 1, 0);
		evaluate(s, 17, 9, 0, 0);

		s = " Wenn AnzahlMeldungen =17 Dann InDenEndlauf=3,InDenZwischenLauf=5";
		evaluate(s, 17, 9, 3, 5);

		s = " Wenn AnzahlMeldungen <> 17 Dann InDenEndlauf=3,InDenZwischenLauf=5";
		evaluate(s, 16, 9, 3, 5);
		evaluate(s, 17, 9, 0, 0);

		s = " Wenn AnzahlMeldungen % 2 = 0 Und (AnzahlMeldungen / 2 = 7 Oder AnzahlBahnen=0 ) Dann InDenEndlauf=3,InDenZwischenLauf=7 Ansonsten Wenn 0 = 0 Dann InDenEndlauf=99";
		evaluate(s, 16, 0, 3, 7);

		s = " Wenn AnzahlMeldungen % 2 = 0 Und (AnzahlMeldungen / 2 = 7 Oder AnzahlBahnen=1 ) Dann InDenEndlauf=3,InDenZwischenLauf=7 Ansonsten Wenn 0 = 0 Dann InDenEndlauf=99, InDenZwischenLauf=100";
		evaluate(s, 16, 0, 99, 100);

		s = " Wenn AnzahlMeldungen> AnzahlBahnen + 2 Dann InDenEndlauf=3,InDenZwischenLauf=7";
		evaluate(s, 17, 9, 3, 7);

		s = " Wenn AnzahlMeldungen<=2*AnzahlBahnen Dann InDenEndlauf=3,InDenZwischenLauf=7";
		evaluate(s, 17, 9, 3, 7);

		s = " Wenn AnzahlMeldungen > AnzahlBahnen/2 Dann InDenEndlauf=3,InDenZwischenLauf=7 ansonsten InDenEndlauf=99";
		evaluate(s, 17, 9, 3, 7);

		s = " Wenn AnzahlMeldungen > AnzahlBahnen / 2 Dann InDenEndlauf=3,InDenZwischenLauf=7 ansonsten InDenEndlauf=99, InDenZwischenLauf=98";
		evaluate(s, 3, 9, 99, 98);

		s = "Wenn AnzahlMeldungen < 3 * AnzahlBahnen und AnzahlMeldungen - 7 > 1 Dann InDenEndlauf = 2, InDenZwischenLauf = 5 ";
		evaluate(s, 19, 9, 2, 5);
	}

	private void evaluate(String s, int numTeams, int numLanes, int intoFinal, int intoSemiFinal) throws Exception {
		ExpressionParser parser = new ExpressionParser();
		parser.evaluateExpression(s, numTeams, numLanes);
		Assert.assertEquals(intoFinal, parser.getIntoFinal());
		Assert.assertEquals(intoSemiFinal, parser.getIntoSemiFinal());
	}
}
