package org.planner.ui.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.jhlabs.image.ChromeFilter;

public class ImageCreator {

	public byte[] createCCAbbreviation(String firstName, String lastName) throws IOException {
		int width = 64;
		int height = 64;
		BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gc = src.createGraphics();

		Point center = new Point(width / 2, height / 2);
		int r = (int) (Math.random() * 60);
		int g = (int) (Math.random() * 60);
		int b = (int) (Math.random() * 60);
		Color color = new Color(r, g, b);
		float radius = width / 2;
		float[] fractions = { 0.0f, 0.5f, 1.0f };
		Point2D focus = new Point2D.Float(width / 2, height / 2);
		Color[] colors = { color, color, new Color(0, 0, 0, 0) };
		gc.setPaint(new RadialGradientPaint(center, radius, focus, fractions, colors, CycleMethod.NO_CYCLE));
		gc.fillRect(0, 0, width, height);

		Font font = new Font("SansSerif", Font.BOLD, width / 2);
		gc.setFont(font);
		StringBuilder sb = new StringBuilder(2);
		if (firstName != null && firstName.length() > 0)
			sb.append(Character.toUpperCase(firstName.charAt(0)));
		if (lastName != null && lastName.length() > 0)
			sb.append(Character.toUpperCase(lastName.charAt(0)));
		String text = sb.toString();
		FontMetrics fm = gc.getFontMetrics();
		int stringWidth = fm.stringWidth(text);
		int stringHeight = fm.getAscent();
		gc.setPaint(Color.yellow);
		gc.drawString(text, (width - stringWidth) / 2, height / 2 + stringHeight / 2 - fm.getDescent() + 1);

		BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		ChromeFilter filter = new ChromeFilter();
		filter.setBumpSoftness(5.46f);
		filter.setBumpHeight(0.58f);
		filter.setAmount(0.66f);
		filter.setDiffuseColor(-4408132);
		filter.filter(src, dest);
		// aus irgendeinem Grund kam es beim direkten Schreiben auf den ServletOutputStream
		// ab und zu zum Hängenbleiben in PNGImageWriter Zeile 140 ...
		// io.undertow.servlet.spec.ServletOutputStreamImpl Zeile 566
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(dest, "PNG", out);
		return out.toByteArray();
	}
}
