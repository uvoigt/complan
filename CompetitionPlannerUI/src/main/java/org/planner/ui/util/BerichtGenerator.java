package org.planner.ui.util;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.inject.Named;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.planner.eo.Address;
import org.planner.eo.Announcement;
import org.planner.eo.Program;
import org.planner.eo.ProgramRace;
import org.planner.eo.Race;
import org.planner.eo.Team;
import org.planner.eo.TeamMember;
import org.planner.eo.User;
import org.planner.ui.util.text.TextFormat.Keyword;
import org.planner.util.LogUtil.FachlicheException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Image;
import com.itextpdf.text.List;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Section;
import com.itextpdf.text.TabStop.Alignment;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

/**
 * Erzeugt einen Bericht, der aus Textbestandteilen sowie Übersichtsdiagrammen besteht. Die Klasse ist nicht
 * Thread-safe. Es muss deshalb für jeden Request eine neue Instanz erzeugt werden.
 * 
 * @author Uwe Voigt, IBM
 */
@Named
public class BerichtGenerator {

	private class HeaderWriter extends PdfPageEventHelper {
		private Image logo;
		private PdfPTable header;

		private HeaderWriter(byte[] imageData, String title) throws Exception {
			final int dim = 50;
			if (imageData != null) {
				logo = Image.getInstance(imageData);
				logo.scaleToFit(dim, dim);
				logo.setAbsolutePosition(20, PageSize.A4.getTop() - logo.getScaledHeight() - 20);
			}
			header = new PdfPTable(1);
			header.setTotalWidth(555);
			PdfPCell cell = new PdfPCell(new Phrase(title, defaultFont));
			cell.setBorder(Rectangle.BOTTOM);
			header.addCell(cell);
		}

		@Override
		public void onEndPage(PdfWriter writer, Document document) {
			if (isToc)
				return;
			try {
				if (logo != null)
					writer.getDirectContent().addImage(logo);
				header.writeSelectedRows(0, -1, 20,
						document.top() + ((document.topMargin() + header.getTotalHeight()) / 2),
						writer.getDirectContent());
			} catch (DocumentException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	private enum Action {
		html, head, title, body, //
		h1, h2, h3, //
		br, hr, //
		blockquote, //
		div, span, //
		table, tbody, tr, td, //
		ul, ol, li, //
		font, //
		strong, b, em, i, u, s, sub, sup, //
		p, img
	}

	private interface KeywordHandler {
		/**
		 * @return null, wenn nicht behandelt
		 */
		String replace(StringBuilder text, Match match) throws Exception;
	}

	private class Match {
		private Keyword keyword;
		private int start;
		private int end;

		private Match(Keyword keyword, int start, int end) {
			this.keyword = keyword;
			this.start = start;
			this.end = end;
		}
	}

	private class SaxHandler extends DefaultHandler {
		private class Context {
			private Action action;

			private Attributes attributes;

			private BaseColor bgColor;

			private Font font = defaultFont;

			private int textRise;

			private Context(Action action, Attributes attributes) {
				this.action = action;
				this.attributes = new AttributesImpl(attributes);
				if (!currentContext.isEmpty()) {
					Context parent = currentContext.peek();
					bgColor = parent.bgColor;
					font = parent.font;
				}
			}

			<T> T getAttValueIgnoreCase(String name, Class<T> type) {
				for (int i = 0, n = attributes.getLength(); i < n; i++) {
					String qn = attributes.getQName(i);
					if (name.equalsIgnoreCase(qn)) {
						String s = attributes.getValue(i);
						if (s == null)
							return null;
						try {
							return type.getConstructor(String.class).newInstance(s);
						} catch (Exception e) {
							throw new IllegalArgumentException("Fehler beim Lesen des Attributs " + name, e);
						}
					}
				}
				return null;
			}
		}

		private class Table {
			private ArrayList<PdfPCell> cells = new ArrayList<PdfPCell>();

			private Integer columnNumber;

			PdfPCell getCurrentCell() {
				return cells.get(cells.size() - 1);
			}
		}

		private final class AnnouncementKeywordHandler implements KeywordHandler {
			private final Announcement announcement;

			private AnnouncementKeywordHandler(Announcement announcement) {
				this.announcement = announcement;
			}

			@Override
			public String replace(StringBuilder text, Match match) throws Exception {
				switch (match.keyword) {
				case name:
					return announcement.getName();
				case category:
					return announcement.getCategory().getName();
				case location:
					Address address = announcement.getLocation().getClub() != null
							? announcement.getLocation().getClub().getAddress()
							: announcement.getLocation().getAddress();
					return address.getCity().getName()
							+ (address.getAddition() != null ? " " + address.getAddition() : "");
				case address:
					address = announcement.getLocation().getClub() != null
							? announcement.getLocation().getClub().getAddress()
							: announcement.getLocation().getAddress();
					return address.getPostCode() + " " + address.getCity().getName() + " " + address.getStreet();
				case homepage:
					return announcement.getClub().getAddress().getHomepage() != null
							? announcement.getClub().getAddress().getHomepage() : "";
				case startDate:
					return DateFormat.getDateInstance().format(announcement.getStartDate());
				case endDate:
					return DateFormat.getDateInstance().format(announcement.getEndDate());
				case races:
					PdfPTable table = new PdfPTable(new float[] { .15f, .1f, .05f, .4f, .15f, .15f });
					table.setWidthPercentage(100);
					table.getDefaultCell().disableBorderSide(Rectangle.BOX);

					Font headerFont = new Font(defaultFont);
					headerFont.setStyle(defaultFont.getStyle() | Font.BOLD);
					table.addCell(new Phrase("Tag", headerFont));
					table.addCell(new Phrase("Zeit", headerFont));
					table.addCell(new Phrase("Nr.", headerFont));
					table.addCell(new Phrase("Bezeichnung", headerFont));
					table.addCell(new Phrase("Distanz", headerFont));
					table.addCell(new Phrase("Zusatz", headerFont));

					java.util.List<Race> races = new ArrayList<>(announcement.getRaces());
					Collections.sort(races, new Comparator<Race>() {
						@Override
						public int compare(Race r1, Race r2) {
							return r1.getNumber().compareTo(r2.getNumber());
						}
					});
					DateFormat FORMAT_DAY = new SimpleDateFormat("EEEE");
					DateFormat FORMAT_TIME = new SimpleDateFormat("HH:mm");
					for (Race race : races) {
						String day = "";
						if (race.getDay() != null) {
							Calendar cal = Calendar.getInstance();
							cal.setTime(announcement.getStartDate());
							cal.add(Calendar.DAY_OF_YEAR, race.getDay());
							day = FORMAT_DAY.format(cal.getTime());
						}
						String time = "";
						if (race.getStartTime() != null) {
							time = FORMAT_TIME.format(race.getStartTime());
						}
						table.addCell(new Phrase(day, defaultFont));
						table.addCell(new Phrase(time, defaultFont));
						table.addCell(new Phrase(Integer.toString(race.getNumber()), defaultFont));
						table.addCell(new Phrase(race.getBoatClass().getText() + " " + race.getGender().getText() + " "
								+ race.getAgeType().getText(), defaultFont));
						table.addCell(new Phrase(race.getDistance() + " m", defaultFont));
						table.addCell(new Phrase());
					}
					addToDocument(table);
					return "";
				default:
					return null;
				}
			}
		}

		private Stack<Context> currentContext = new Stack<Context>();

		private StringBuilder currentText = new StringBuilder();

		private final KeywordHandler replacer;

		private SaxHandler(Object object) {
			if (object instanceof Announcement)
				replacer = new AnnouncementKeywordHandler((Announcement) object);
			else
				throw new IllegalArgumentException(object != null ? object.toString() : null);
		}

		@Override
		public InputSource resolveEntity(String s, String s1) throws IOException, SAXException {
			return new InputSource(new StringReader(""));
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			Action action = Action.valueOf(name.toLowerCase());

			Context context = new Context(action, attributes);
			currentContext.push(context);
			switch (action) {
			default:
				break;
			case p:
				closeCurrentParagraph();
				currentParagraph = new Paragraph();
				break;
			case font:
				flushText(context);
				context.font = getFont(attributes.getValue("face"), context.font);
				context.font.setSize(getSize(attributes.getValue("size"), context.font));
				context.font.setColor(getColor(attributes.getValue("color"), context.font));
				break;
			case strong:
			case b:
				flushText(context);
				context.font = new Font(context.font);
				context.font.setStyle(context.font.getStyle() | Font.BOLD);
				break;
			case em:
			case i:
				flushText(context);
				context.font = new Font(context.font);
				context.font.setStyle(context.font.getStyle() | Font.ITALIC);
				break;
			case u:
				flushText(context);
				context.font = new Font(context.font);
				context.font.setStyle(context.font.getStyle() | Font.UNDERLINE);
				break;
			case s:
				flushText(context);
				context.font = new Font(context.font);
				context.font.setStyle(context.font.getStyle() | Font.STRIKETHRU);
				break;
			case sub:
				flushText(context);
				context.textRise = -7;
				break;
			case sup:
				flushText(context);
				context.textRise = 7;
				break;
			case span:
				flushText(context);
				BaseColor bgColor = getColor(getStyle(attributes, "background-color"));
				if (bgColor != null)
					context.bgColor = bgColor;
				BaseColor color = getColor(getStyle(attributes, "color"));
				if (color != null) {
					context.font = new Font(context.font);
					context.font.setColor(color);
				}
				break;
			case div:
				closeCurrentParagraph();
				currentParagraph = new Paragraph();
				currentParagraph.setAlignment(getAlign(attributes));
				break;
			case blockquote:
				closeCurrentParagraph();
				currentIndent += 15;
				break;
			case ul:
				closeCurrentParagraph();
				currentList = new List(false);
				break;
			case ol:
				closeCurrentParagraph();
				currentList = new List(true);
				break;
			case table:
				// schließe den vorherigen Abschnitt nur wenn es sich nicht um
				// eine geschachtelte Tabelle handelt
				if (currentTable.isEmpty())
					closeCurrentParagraph();
				currentTable.push(new Table());
				break;
			case td:
				PdfPCell cell = new PdfPCell();
				Integer colspan = context.getAttValueIgnoreCase("colspan", Integer.class);
				if (colspan != null)
					cell.setColspan(colspan);
				Integer rowspan = context.getAttValueIgnoreCase("rowspan", Integer.class);
				if (rowspan != null)
					cell.setRowspan(rowspan);
				currentTable.peek().cells.add(cell);
				break;
			case img:
				// https://localhost:8443/planner/javax.faces.resource/ckeditor/plugins/smiley/images/regular_smile.png.xhtml?ln=primefaces-extensions&amp;v=6.1.1
				// ist
				// META-INF/resources/primefaces-extensions/ckeditor/plugins/smiley/images/regular_smile.png
				String src = attributes.getValue("src");
				final String resource = "javax.faces.resource";
				int index = src.indexOf(resource);
				if (index != -1) {
					src = src.substring(index + resource.length());
					index = src.indexOf(".xhtml?");
					if (index != -1)
						src = src.substring(0, index);
					InputStream in = getClass().getClassLoader()
							.getResourceAsStream("/META-INF/resources/primefaces-extensions" + src);
					if (in != null) {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						try {
							IOUtils.copy(in, out);
							Image image = Image.getInstance(out.toByteArray());
							getCurrentParagraph().add(image);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (BadElementException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					// TODO
				}
				break;
			}
		}

		private int getAlign(Attributes attributes) {
			String align = attributes.getValue("align");
			if (align != null) {
				switch (Alignment.valueOf(align.toLowerCase())) {
				case LEFT:
					return Element.ALIGN_LEFT;
				case CENTER:
					return Element.ALIGN_CENTER;
				case RIGHT:
					return Element.ALIGN_RIGHT;
				case ANCHOR: // TODO richtig?
					return Element.ALIGN_JUSTIFIED;
				}
			}
			return Element.ALIGN_UNDEFINED;
		}

		private String getStyle(Attributes attributes, String name) {
			String style = attributes.getValue("style");
			if (style == null)
				return null;
			String[] pairs = style.split(";");
			for (int i = 0; i < pairs.length; i++) {
				String[] pair = pairs[i].split(":");
				if (pair[0].equalsIgnoreCase(name))
					return pair[1].trim();
			}
			return null;
		}

		private BaseColor getColor(String value) {
			if (value == null)
				return null;
			if (value.startsWith("rgb(")) {
				String[] v = value.substring(4, value.length() - 1).split(",");
				return new BaseColor(Integer.parseInt(v[0].trim()), Integer.parseInt(v[1].trim()),
						Integer.parseInt(v[2].trim()));
			}
			Color awtColor = Color.decode(value);
			return new BaseColor(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
		}

		private Font getFont(String face, Font currentFont) {
			if (face == null)
				return new Font(currentFont);
			switch (FontFamily.valueOf(face)) {
			default:
				return new Font(currentFont);
			case COURIER:
				return new Font(FontFamily.COURIER, currentFont.getSize(), currentFont.getStyle());
			case HELVETICA:
				return new Font(FontFamily.HELVETICA, currentFont.getSize(), currentFont.getStyle());
			case TIMES_ROMAN:
				return new Font(FontFamily.TIMES_ROMAN, currentFont.getSize(), currentFont.getStyle());
			}
		}

		private float getSize(String size, Font currentFont) {
			if (size == null)
				return currentFont.getSize();
			return 3 * Integer.parseInt(size);
		}

		private BaseColor getColor(String color, Font currentFont) {
			if (color == null)
				return currentFont.getColor();
			return getColor(color);
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			Context context = currentContext.pop();
			String text = replaceSubstitutesSAX(currentText, context).toString();
			switch (context.action) {
			default:
				addText(text, context);
				break;
			case body:
			case p:
				addText(text, context);
				closeCurrentParagraph();
				break;
			case blockquote:
				closeCurrentParagraph();
				currentIndent -= 15;
				break;
			case b:
			case i:
			case u:
			case s:
			case sub:
			case sup:
			case font:
				addText(text, context);
				break;
			case h1:
				createSection(text, 0, context.attributes);
				break;
			case h2:
				createSection(text, 1, context.attributes);
				break;
			case h3:
				createSection(text, 2, context.attributes);
				break;
			case br:
				addText(text, context);
				getCurrentParagraph().add(Chunk.NEWLINE);
				break;
			case hr:
				LineSeparator separator = new LineSeparator();
				separator.setPercentage(100);
				getCurrentParagraph().add(new Chunk(separator));
				break;
			case li:
				Phrase phrase = currentParagraph != null ? currentParagraph : new Phrase();
				if (text.length() > 0)
					phrase.add(new Chunk(text, context.font));
				ListItem item = new ListItem(phrase);
				item.setFont(context.font);
				if (!currentList.isNumbered()) {
					item.setListSymbol(
							new Chunk((char) 183 + "   ", new Font(FontFamily.SYMBOL, context.font.getSize()))); // bullet
				}
				currentList.add(item);
				currentParagraph = null;
				break;
			case ul:
			case ol:
				addToDocument(currentList);
				break;
			case td:
				Table table = currentTable.peek();
				PdfPCell cell = table.getCurrentCell();
				cell.setBackgroundColor(getColor(context.getAttValueIgnoreCase("bgcolor", String.class)));
				cell.setHorizontalAlignment(getAlign(context.attributes));
				if (currentParagraph != null && currentParagraph.getAlignment() != Element.ALIGN_UNDEFINED)
					cell.setHorizontalAlignment(currentParagraph.getAlignment());
				if (cell.getTable() == null && cell.getCompositeElements() == null) {
					cell.setPhrase(currentParagraph != null ? currentParagraph : new Phrase(text, defaultFont));
				}
				currentParagraph = null;
				break;
			case tr:
				table = currentTable.peek();
				if (table.columnNumber == null) {
					int num = 0;
					for (PdfPCell c : table.cells) {
						num += c.getColspan();
					}
					table.columnNumber = num;
				}
				break;
			case table:
				table = currentTable.pop();
				PdfPTable pdfPTable = new PdfPTable(table.columnNumber != null ? table.columnNumber : 1);
				String width = getStyle(context.attributes, "width");
				if (width != null) {
					if (width.endsWith("%"))
						pdfPTable.setWidthPercentage(Integer.parseInt(width.substring(0, width.length() - 1)));
					else if (width.endsWith("px"))
						pdfPTable.setTotalWidth(Integer.parseInt(width.substring(0, width.length() - 2)));
				}
				Integer border = context.getAttValueIgnoreCase("border", Integer.class);
				Integer cellspacing = context.getAttValueIgnoreCase("cellspacing", Integer.class);
				Integer cellpadding = context.getAttValueIgnoreCase("cellpadding", Integer.class);
				for (PdfPCell c : table.cells) {
					if (border == null || border == 0)
						c.disableBorderSide(Rectangle.BOX);
					else
						c.setBorderWidth(border);
					if (cellpadding != null)
						c.setPadding(cellpadding);
					pdfPTable.addCell(c);
				}
				addToDocument(pdfPTable);
				break;
			}
			currentText.setLength(0);
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			switch (currentContext.peek().action) {
			case table:
			case tbody:
			case tr:
			case ol:
			case ul:
				currentText.setLength(0);
				break;
			default:
				currentText.append(ch, start, length);
				break;
			}
		}

		private void addText(String text, Context context) throws SAXException {
			if (text.length() == 0)
				return;
			Chunk chunk = new Chunk(text, context.font);
			chunk.setTextRise(context.textRise);
			if (context.bgColor != null)
				chunk.setBackground(context.bgColor);
			getCurrentParagraph().add(chunk);
		}

		private void flushText(Context context) throws SAXException {
			addText(replaceSubstitutesSAX(currentText, context).toString(), context);
			currentText.setLength(0);
		}

		private StringBuilder replaceSubstitutesSAX(StringBuilder text, Context context) throws SAXException {
			try {
				return replaceSubstitutes(text, context, 0);
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}

		private StringBuilder replaceSubstitutes(StringBuilder text, Context context, int offset) throws Exception {
			Match match = findKeywordMatch(text, offset);
			if (match == null)
				return text;
			String replacement = null;
			switch (match.keyword) {
			default:
				replacement = replacer.replace(text, match);
				if (replacement == null)
					throw new IllegalArgumentException("Unbehandelte Substitution: " + match.keyword);
				break;
			case page:
				replacement = Integer.toString(document.getPageNumber());
				break;
			case pages:
				break;
			case currentDate:
				replacement = DateFormat.getDateInstance(DateFormat.LONG, /* Util.gibBenutzerLocale() */ Locale.GERMAN)
						.format(new Date());
				break;
			case pageBreak:
				// befindet sich das Page-Break-Tag mitten im Text,
				// dann muss der vorherige Text erst dem Dokument hinzugefügt
				// werden
				addText(text.substring(0, match.start), context);
				getCurrentParagraph().add(Chunk.NEXTPAGE);
				text.delete(0, match.end);
				return replaceSubstitutes(text, context, match.start);
			case toc:
				pageNumToc = writer.getPageNumber();
				break;
			}
			text.replace(match.start, match.end, replacement != null ? replacement : "");
			return replaceSubstitutes(text, context, match.start);
		}

		private Match findKeywordMatch(StringBuilder text, int offset) {
			int begin = text.indexOf("{", offset);
			if (begin == -1)
				return null;
			int end = text.indexOf("}", begin);
			if (end == -1)
				return null;
			String key = text.substring(begin + 1, end);
			try {
				Keyword keyword = Keyword.valueOf(key);
				end++;
				return new Match(keyword, begin, end);
			} catch (IllegalArgumentException e) {
				throw new FachlicheException(messages, "generator.unknownKeyword", key);
			}
		}

		/*
		 * Erzeugt ein Kapitel. Um den Effekt, dass Kapitelüberschriften, auf die ein Bericht folgt auf der vorherigen
		 * Seite stehen bleiben, wenn der Platz für den Bericht nicht mehr ausreicht, dürfen die Kapitel nicht sofort
		 * dem Dokument hinzugefügt werden.
		 */
		private void createSection(String text, int index, Attributes attributes) throws SAXException {
			closeCurrentSection(index);
			Chunk chunk = new Chunk(text, hFonts[index]);
			Paragraph paragraph = new Paragraph(chunk);
			paragraph.setAlignment(getAlign(attributes));

			if (index > 0 && currentSection[index - 1] == null)
				throw new SAXException(new FachlicheException(messages, "generator.emptySection"));
			currentSection[index] = index == 0 ? new Chapter(paragraph, ++chapterNumber)
					: currentSection[index - 1].addSection(paragraph);
			currentSection[index].setTriggerNewPage(false);
			currentSection[index].setNumberStyle(Section.NUMBERSTYLE_DOTTED_WITHOUT_FINAL_DOT);
			chunk.setLocalDestination(currentSection[index].getTitle().getContent());
			if (pendingSection == null)
				pendingSection = currentSection[index];
		}
	}

	private static final Font[] hFonts = { new Font(FontFamily.HELVETICA, 14f, Font.BOLD),
			new Font(FontFamily.HELVETICA, 12, Font.BOLD), new Font(FontFamily.HELVETICA, 10f, Font.BOLD) };

	private static final Font defaultFont = new Font(FontFamily.HELVETICA, 8f, Font.NORMAL);

	private static final Font tocFont = new Font(FontFamily.HELVETICA, 8f, Font.NORMAL);

	private static final Font footerFont = new Font(FontFamily.HELVETICA, 6f, Font.NORMAL);

	private final ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle"); // locale

	private int chapterNumber;

	private Section pendingSection;

	private Section[] currentSection = new Section[3];

	private List currentList;

	private Stack<SaxHandler.Table> currentTable = new Stack<SaxHandler.Table>();

	private Paragraph currentParagraph;

	private float currentIndent;

	private Document document;

	private PdfWriter writer;

	// wir merken uns die Seite, auf der das Inhaltsverzeichnis hinzugefügt
	// wurde
	private Integer pageNumToc;

	private boolean isToc;

	private int elementCount;

	/**
	 * Generiert den übermittelten Bericht als PDF.
	 * 
	 * @param announcement
	 *            der Bericht
	 * @param out
	 *            Outputstream
	 * @throws Exception
	 *             Fehler
	 */
	public void generate(final Announcement announcement, OutputStream out) throws Exception {

		ByteArrayOutputStream bos = setUpDocument();

		writer.setPageEvent(new HeaderWriter(null,
				MessageFormat.format(messages.getString("generator.announcement"), announcement.getName())));

		document.open();
		document.addCreator("Wettkampfplaner");

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		SAXParser parser = factory.newSAXParser();
		SaxHandler handler = new SaxHandler(announcement);
		appendText(announcement.getText(), parser, handler);
		closeCurrentSection(0);

		finishDocument(out, bos);
	}

	public void generate(Program program, OutputStream out) throws Exception {

		ByteArrayOutputStream bos = setUpDocument();

		writer.setPageEvent(new HeaderWriter(null,
				MessageFormat.format(messages.getString("generator.program"), program.getAnnouncement().getName())));

		document.open();
		document.addCreator("Wettkampfplaner");

		PdfPTable table = new PdfPTable(new float[] { .1f, .30f, .30f, .30f });
		table.setWidthPercentage(100);
		table.getDefaultCell().disableBorderSide(Rectangle.BOX);

		Font raceFont = new Font(defaultFont);
		raceFont.setStyle(defaultFont.getStyle() | Font.BOLD);

		java.util.List<ProgramRace> races = new ArrayList<>(program.getRaces());
		// Collections.sort(races, new Comparator<Race>() {
		// @Override
		// public int compare(Race r1, Race r2) {
		// return r1.getNumber().compareTo(r2.getNumber());
		// }
		// });
		DateFormat FORMAT_DAY = new SimpleDateFormat("EEEE");
		DateFormat FORMAT_TIME = new SimpleDateFormat("HH:mm");
		for (ProgramRace programRace : races) {

			Race race = programRace.getRace();
			PdfPCell cell = new PdfPCell(
					new Phrase("Rennen " + race.getNumber() + "-" + programRace.getNumber(), raceFont));
			cell.setColspan(2);
			cell.disableBorderSide(Rectangle.BOX);
			cell.setMinimumHeight(20);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase(programRace.getRaceType().getText(), defaultFont));
			cell.setColspan(2);
			cell.disableBorderSide(Rectangle.BOX);
			cell.setMinimumHeight(20);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase(race.getBoatClass().getText() + " " + race.getAgeType().getText() + " "
					+ race.getGender().getText(), defaultFont));
			cell.setColspan(2);
			cell.setBorder(Rectangle.BOTTOM);
			table.addCell(cell);

			cell = new PdfPCell(
					new Phrase(programRace.getIntoFinal() + " " + programRace.getIntoSemiFinal(), defaultFont));
			cell.setBorder(Rectangle.BOTTOM);
			table.addCell(cell);

			cell = new PdfPCell(
					new Phrase(race.getDistance() + " m" + "     " + FORMAT_DAY.format(programRace.getStartTime()) + " "
							+ FORMAT_TIME.format(programRace.getStartTime()), defaultFont));
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			cell.setBorder(Rectangle.BOTTOM);
			table.addCell(cell);

			java.util.List<Team> participants = programRace.getParticipants();
			for (int i = 0; i < participants.size(); i++) {
				Team team = participants.get(i);

				table.addCell(new Phrase(Integer.toString(i + 1), defaultFont));

				java.util.List<TeamMember> members = team.getMembers();
				int normalTeamSize = race.getBoatClass().getMaximalTeamSize();
				int n = Math.min(normalTeamSize, members.size());
				for (int j = 0; j < n; j++) {
					TeamMember member = members.get(j);
					User user = member.getUser();

					if (j == 2)
						table.addCell(new Phrase("", defaultFont));

					if (user != null)
						// TODO alter
						table.addCell(new Phrase(user.getFirstName() + " " + user.getLastName(), defaultFont));
					else
						table.addCell(new Phrase(member.getRemark(), defaultFont));

					if (j == 1)
						table.addCell(new Phrase(team.getClub().getShortName() != null ? team.getClub().getShortName()
								: team.getClub().getName(), defaultFont));
					else if (j == 3)
						table.addCell(new Phrase("", defaultFont));
				}
				if (members.size() == 1) {
					table.addCell(new Phrase("", defaultFont));
					table.addCell(new Phrase(team.getClub().getShortName() != null ? team.getClub().getShortName()
							: team.getClub().getName(), defaultFont));
				}
				if (members.size() > normalTeamSize) {
					for (int j = normalTeamSize; j < members.size(); j++) {
						TeamMember member = members.get(j);
						User user = member.getUser();

						cell = new PdfPCell(
								new Phrase("Ersatz: " + user.getFirstName() + " " + user.getLastName(), defaultFont));
						cell.setColspan(2);
						cell.disableBorderSide(Rectangle.BOX);
						cell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(cell);
						table.addCell(new Phrase("", defaultFont));
						table.addCell(new Phrase("", defaultFont));
						// TODO das klappt bisher nur für einen
					}
				}
			}
		}
		addToDocument(table);

		closeCurrentSection(0);

		finishDocument(out, bos);
	}

	private ByteArrayOutputStream setUpDocument() throws DocumentException {
		document = new Document(); // A4
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writer = PdfWriter.getInstance(document, bos);
		writer.setStrictImageSequence(true);
		writer.setLinearPageMode();
		return bos;
	}

	private void finishDocument(OutputStream out, ByteArrayOutputStream bos)
			throws Exception, IOException, DocumentException {
		if (pageNumToc != null)
			insertToc();

		// wir verhindern so die "The document has no pages"-Exceptions
		if (elementCount == 0)
			throw new FachlicheException(messages, "generator.empty");

		document.close();
		writer.close();

		// füge Seitenzahlen hinzu
		PdfReader reader = new PdfReader(bos.toByteArray());
		PdfStamper stamper = new PdfStamper(reader, out);
		for (int i = 1, n = reader.getNumberOfPages(); i <= n; i++) {
			createFooter(i, n).writeSelectedRows(0, -1, 20, 20, stamper.getOverContent(i));
		}
		stamper.close();
		reader.close();
	}

	private PdfPTable createFooter(int page, int total) {
		PdfPTable table = new PdfPTable(1);
		table.setTotalWidth(555);
		PdfPCell cell = new PdfPCell(
				new Phrase(MessageFormat.format(messages.getString("generator.footer"), page, total), footerFont));
		cell.setFixedHeight(20);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell.setBorder(Rectangle.TOP);
		table.addCell(cell);
		return table;
	}

	private void appendText(String text, SAXParser parser, SaxHandler handler) throws Exception {
		if (text == null)
			text = "";
		text = "<!DOCTYPE html [ <!ENTITY nbsp \"&#160;\">]><html>" + text + "</html>";
		try {
			parser.parse(new InputSource(new StringReader(text)), handler);
		} catch (SAXException e) {
			if (e.getException() == null)
				throw e;
			throw e.getException();
		}
	}

	private void closeCurrentParagraph() throws SAXException {
		if (currentParagraph != null) {
			currentParagraph.setIndentationLeft(currentIndent);
			Element e = currentParagraph;
			currentParagraph = null;
			addToDocument(e);
		}
	}

	private void closeCurrentSection(int depth) throws SAXException {
		closeCurrentParagraph();
		if (pendingSection != null && pendingSection.getNumberDepth() == depth + 1)
			closePendingSection();
		for (int i = 2; i > depth; i--) {
			closeCurrentSection(i);
		}
		if (currentSection[depth] != null) {
			currentSection[depth] = null;
		}
	}

	private void closePendingSection() throws SAXException {
		if (pendingSection != null) {
			addToDocumentDirectly(pendingSection);
			pendingSection = null;
		}
	}

	/**
	 * Fügt das Inhaltsverzeichnis in das Dokument ein.
	 */
	private void insertToc() throws Exception {
		PdfOutline outline = writer.getRootOutline();

		// fügt das Inhaltsverzeichnis hinzu
		isToc = true;
		int numPages = createBookmarks(outline) - 1;

		document.newPage();
		int total = writer.reorderPages(null);
		int[] order = new int[total];
		for (int i = 0; i < total; i++) {
			if (i < pageNumToc) {
				order[i] = i + 1;
			} else if (i == pageNumToc) {
				for (int j = 0; j <= numPages; j++)
					order[i + j] = total - numPages + j;
				i += numPages;
			} else {
				order[i] = i - numPages;
			}
		}
		writer.reorderPages(order);
		isToc = false;
	}

	/**
	 * liefert die Anzahl an Seiten, die das Inhaltsverzeichnis einnimmt
	 */
	private int createBookmarks(PdfOutline outline) throws Exception {
		// um die Anzahl an Seiten, die das Inhaltsverzeichnis einnimmt zu
		// ermitteln, wird es zuerst in ein separates Dokument geschrieben
		Document toc = new Document();
		PdfWriter wr = PdfWriter.getInstance(toc, new ByteArrayOutputStream());
		toc.open();
		int pages = doCreateBookmarks(outline, toc, wr, 0, true);
		doCreateBookmarks(outline, document, writer, pages, false);
		return pages;
	}

	private int doCreateBookmarks(PdfOutline outline, Document toc, PdfWriter wr, int pageOffset, boolean close)
			throws Exception {
		PdfPTable table = new PdfPTable(2);
		table.setWidthPercentage(90);
		table.setWidths(new int[] { 97, 3 });
		processBookmarks(outline.getKids(), table, 0, pageOffset);
		toc.newPage();
		int pageCount = wr.reorderPages(null);
		toc.add(createTocHeading());
		toc.add(table);
		if (close) {
			toc.close();
			wr.close();
		}
		return wr.reorderPages(null) - pageCount;
	}

	private Element createTocHeading() throws DocumentException {
		Phrase phrase = new Paragraph(messages.getString("generator.toc"));
		Font font = new Font(defaultFont);
		font.setStyle(Font.BOLD);
		font.setSize(44);
		phrase.setFont(font);
		PdfPCell cell = new PdfPCell(phrase);
		cell.disableBorderSide(Rectangle.BOX);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setExtraParagraphSpace(15);
		PdfPTable heading = new PdfPTable(1);
		heading.addCell(cell);
		return heading;
	}

	/**
	 * Erzeugt das Inhaltsverzeichnis, indem die Bookmarks rekursiv iteriert werden
	 */
	private void processBookmarks(ArrayList<PdfOutline> outlines, PdfPTable table, int indent, int pageOffset)
			throws Exception {
		if (outlines == null)
			return;
		for (PdfOutline outline : outlines) {
			String title = outline.getTitle();
			int pageNumber = getPageNumber(outline.getPdfDestination().getDirectObject(0));
			pageNumber += pageOffset;

			Chunk titleChunk = new Chunk(title, tocFont);
			titleChunk.setLocalGoto(title);
			Chunk pageChunk = new Chunk(Integer.toString(pageNumber), tocFont);
			pageChunk.setLocalGoto(title);

			// füge die Punkte bis zur Seitenzahl hinzu
			float titelWidth = titleChunk.getWidthPoint();
			float nWidth = pageChunk.getWidthPoint();
			float pointWidth = tocFont.getCalculatedBaseFont(true).getWidthPoint('.', tocFont.getCalculatedSize());
			StringBuilder sb = new StringBuilder((int) ((466 - 10 * indent - titelWidth - nWidth) / pointWidth));
			for (int i = sb.capacity(); i > 0; i--) {
				sb.append('.');
			}
			titleChunk.append(sb.toString());

			// Kapitelüberschrift
			PdfPCell cell = new PdfPCell(new Phrase(titleChunk));
			cell.disableBorderSide(Rectangle.BOX);
			cell.setNoWrap(true);
			cell.setIndent(10 * indent);
			table.addCell(cell);

			// Seitenzahl
			cell = new PdfPCell(new Phrase(pageChunk));
			cell.disableBorderSide(Rectangle.BOX);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			table.addCell(cell);

			processBookmarks(outline.getKids(), table, indent + 1, pageOffset);
		}
	}

	private int getPageNumber(PdfObject reference) {
		for (int i = 1, n = writer.getPageNumber(); i <= n; i++)
			if (writer.getPageReference(i) == reference)
				return i;
		return -1;
	}

	private Paragraph getCurrentParagraph() {
		if (currentParagraph == null)
			currentParagraph = new Paragraph();
		return currentParagraph;
	}

	private <T extends Element> T addToDocument(T element) throws SAXException {
		if (element != null) {
			boolean added = false;
			if (!currentTable.isEmpty()) {
				SaxHandler.Table table = currentTable.peek();
				PdfPCell cell = table.getCurrentCell();
				if (element instanceof PdfPTable) {
					// leider ist setTable nicht public
					PdfPCell newCell = new PdfPCell((PdfPTable) element);
					table.cells.set(table.cells.size() - 1, newCell);
				} else {
					cell.addElement(element);
				}
				added = true;
			} else if (currentParagraph != null) {
				currentParagraph.add(element);
			} else if (currentSection[2] != null) {
				currentSection[2].add(element);
			} else if (currentSection[1] != null) {
				currentSection[1].add(element);
			} else if (currentSection[0] != null) {
				currentSection[0].add(element);
			} else {
				addToDocumentDirectly(element);
				added = true;
			}
			// if (!added && pendingSection == null)
			// addToDocumentDirectly(element);
		}
		return element;
	}

	private void addToDocumentDirectly(Element element) throws SAXException {
		try {
			document.add(element);
			elementCount++;
		} catch (DocumentException e) {
			throw new SAXException(e);
		}
	}
}
