package org.planner.ui.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.jhlabs.image.ChromeFilter;

public class ImageCreator {

	private Integer fontHeight;

	public byte[] createCCAbbreviation(String firstName, String lastName) throws IOException {
		int width = 64;
		int height = 64;
		BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gc = src.createGraphics();

		StringBuilder sb = new StringBuilder(2);
		if (firstName != null && firstName.length() > 0)
			sb.append(Character.toUpperCase(firstName.charAt(0)));
		if (lastName != null && lastName.length() > 0)
			sb.append(Character.toUpperCase(lastName.charAt(0)));
		String text = sb.toString();

		Font font = new Font("Serif", Font.BOLD, width / 2);
		gc.setFont(font);
		int fontHeight = getRealFontHeight(text, src, gc);

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

		gc.setPaint(Color.yellow);
		gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		FontMetrics fm = gc.getFontMetrics();
		int stringWidth = fm.stringWidth(text);

		int x = (width - stringWidth) / 2;
		int y = (height + fontHeight) / 2;
		gc.drawString(text, x, y);

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

	private int getRealFontHeight(String text, BufferedImage image, Graphics2D gc) {
		if (fontHeight == null) {
			gc.setPaint(Color.yellow);
			gc.drawString(text, 0, image.getHeight());
			Raster data = image.getData();
			int[] pixels = new int[data.getWidth() * 4];
			Rows: for (int i = data.getHeight() - 1; i >= 0; i--) {
				data.getPixels(0, i, data.getWidth(), 1, pixels);
				for (int j = 0; j < pixels.length; j++) {
					if (pixels[j] != 0)
						continue Rows;
				}
				fontHeight = data.getHeight() - i - 1;
				Composite composite = gc.getComposite();
				gc.setComposite(AlphaComposite.Clear);
				gc.fillRect(0, 0, data.getWidth(), data.getHeight());
				gc.setComposite(composite);
				break;
			}
		}
		return fontHeight;
	}
}
