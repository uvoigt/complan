package org.planner.ui.util;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;

import javax.inject.Inject;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.planner.eo.Address;
import org.planner.eo.Announcement;
import org.planner.eo.Club;
import org.planner.eo.Placement;
import org.planner.eo.Program;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRace.RaceType;
import org.planner.eo.ProgramRaceTeam;
import org.planner.eo.Race;
import org.planner.eo.TeamMember;
import org.planner.eo.User;
import org.planner.ui.beans.Messages;
import org.planner.ui.beans.announcement.RenderBean;
import org.planner.ui.util.converter.PlacementConverter;
import org.planner.ui.util.text.TextFormat.Keyword;
import org.planner.util.LogUtil.FachlicheException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

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
import com.itextpdf.text.api.Indentable;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDiv;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPRow;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPTableEvent;
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
public class BerichtGenerator {

	private class HeaderWriter extends PdfPageEventHelper {
		private Image logo;
		private PdfPTable header;

		private HeaderWriter(byte[] imageData, PdfPCell title) throws Exception {
			final int dim = 50;
			if (imageData != null) {
				logo = Image.getInstance(imageData);
				logo.scaleToFit(dim, dim);
				logo.setAbsolutePosition(20, PageSize.A4.getTop() - logo.getScaledHeight() - 20);
			}
			header = new PdfPTable(1);
			header.setTotalWidth(555);
			header.addCell(title);
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

	private class QualificationIndicator implements PdfPCellEvent, PdfPTableEvent {
		private float unitsOn;
		private float phase;
		private String caption;
		private float captionRow;
		private BaseFont captionFont;

		QualificationIndicator(RaceType forRace, BaseFont captionFont) {
			switch (forRace) {
			default:
				break;
			case semiFinal:
				unitsOn = 3;
				phase = 3;
				caption = messages.get("results.semiFinal");
				break;
			case finalA:
				unitsOn = 1;
				phase = 1;
				caption = messages.get("results.finalA");
				break;
			}
			this.captionFont = captionFont;
		}

		@Override
		public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
			PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
			canvas.saveState();
			canvas.setLineDash(unitsOn, phase);
			canvas.moveTo(position.getLeft(), position.getBottom());
			canvas.lineTo(position.getLeft() + position.getWidth(), position.getBottom());
			canvas.stroke();
			canvas.restoreState();
			captionRow = position.getBottom();
		}

		@Override
		public void tableLayout(PdfPTable table, float[][] widths, float[] heights, int headerRows, int rowStart,
				PdfContentByte[] canvases) {
			PdfContentByte canvas = canvases[PdfPTable.TEXTCANVAS];
			canvas.saveState();
			canvas.beginText();
			int fontSize = 5;
			canvas.setFontAndSize(captionFont, fontSize);
			float width = captionFont.getWidthPoint(caption, fontSize);
			// da die Zeilen nach dem Hinzufügen durch colspans geändert werden,
			// kann getLastCompletedRow nicht verwendet werden
			float lastWidth = computeLastColumnWidth(table.getRows());
			float lastx = widths[0][widths[0].length - 1];
			float x = (lastx + lastWidth - width) / 2;
			canvas.moveText(x, captionRow + fontSize / 2);
			canvas.showText(caption);
			canvas.endText();
			canvas.restoreState();
		}

		private float computeLastColumnWidth(ArrayList<PdfPRow> rows) {
			for (PdfPRow row : rows) {
				PdfPCell[] cells = row.getCells();
				PdfPCell cell = cells[cells.length - 1];
				if (cell != null)
					return cell.getWidth();
			}
			return 0;
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

			private <T> T getAttributeValue(String name, Class<T> type) {
				for (int i = 0, n = attributes.getLength(); i < n; i++) {
					String qn = attributes.getQName(i);
					if (name.equalsIgnoreCase(qn)) {
						return getValueInstance(attributes.getValue(i), type);
					}
				}
				return null;
			}

			private <T> T getStyleValue(String name, Class<T> type) {
				String style = getAttributeValue("style", String.class);
				if (style == null)
					return getValueInstance(null, type);
				String[] pairs = style.split(";");
				for (int i = 0; i < pairs.length; i++) {
					String[] pair = pairs[i].split(":");
					if (pair[0].trim().equalsIgnoreCase(name)) {
						return getValueInstance(pair[1].trim(), type);
					}
				}
				return getValueInstance(null, type);
			}

			@SuppressWarnings("unchecked")
			private <T> T getValueInstance(String s, Class<T> type) {
				if (type == Font.class)
					return (T) getFont(s, font);
				if (type == BaseColor.class)
					return (T) getColor(s);
				if (s == null)
					return null;
				try {
					return type.getConstructor(String.class).newInstance(s);
				} catch (Exception e) {
					throw new IllegalArgumentException("Lesen des Wertes von " + s, e);
				}
			}

			private Float getSizeValue(String size) {
				if (size == null)
					return null;
				if (size.endsWith("px"))
					size = size.substring(0, size.length() - 2);
				return Float.valueOf(size);
			}

			private float getFontSize(String size, Font currentFont) {
				if (size == null)
					return currentFont.getSize();
				return getSizeValue(size);
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
				String[] parts = face.split(",");
				if (parts.length > 1) {
					for (String string : parts) {
						Font font = getFont(string, currentFont);
						if (font != null)
							return font;
					}
				}
				try {
					switch (FontFamily.valueOf(face.toUpperCase())) {
					default:
						return new Font(currentFont);
					case COURIER:
						return new Font(FontFamily.COURIER, currentFont.getSize(), currentFont.getStyle());
					case HELVETICA:
						return new Font(FontFamily.HELVETICA, currentFont.getSize(), currentFont.getStyle());
					case TIMES_ROMAN:
						return new Font(FontFamily.TIMES_ROMAN, currentFont.getSize(), currentFont.getStyle());
					}
				} catch (IllegalArgumentException e) {
					return null;
				}
			}

			private int getAlignment(String alignment) {
				if (alignment == null)
					alignment = getStyleValue("text-align", String.class);
				if (alignment != null) {
					switch (Alignment.valueOf(alignment.toUpperCase())) {
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

			private void processStyle() {
				BaseColor bgColor = getStyleValue("background-color", BaseColor.class);
				if (bgColor != null)
					this.bgColor = bgColor;
				font = getStyleValue("font-family", Font.class);
				font.setSize(getFontSize(getStyleValue("font-size", String.class), font));
				BaseColor color = getStyleValue("color", BaseColor.class);
				if (color != null) {
					font = new Font(font);
					font.setColor(color);
				}
			}

			private void applyStyle(Element e) {
				if (e instanceof Rectangle)
					((Rectangle) e).setBackgroundColor(bgColor);
				if (e instanceof PdfDiv) {
					((PdfDiv) e).setBackgroundColor(bgColor);
					((PdfDiv) e).setTextAlignment(getAlignment(null));
					// ((PdfDiv) e).setPercentageWidth(100f);
				}
				if (e instanceof PdfPCell) {
					((PdfPCell) e).setHorizontalAlignment(getAlignment(null));
					if (currentElement instanceof Paragraph
							&& ((Paragraph) currentElement).getAlignment() != Element.ALIGN_UNDEFINED)
						((PdfPCell) e).setHorizontalAlignment(((Paragraph) currentElement).getAlignment());
				}
				if (e instanceof PdfPTable) {
					PdfPTable t = (PdfPTable) e;
					for (PdfPRow row : t.getRows()) {
						for (PdfPCell cell : row.getCells()) {
							if (cell != null && cell.getBackgroundColor() == null)
								cell.setBackgroundColor(bgColor);
						}
					}
				}
			}
		}

		private class Table {
			private ArrayList<PdfPCell> cells = new ArrayList<>();

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
					Club club = announcement.getClub();
					return club != null && club.getAddress().getHomepage() != null
							? announcement.getClub().getAddress().getHomepage()
							: "";
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

					// im Falle einer Neuanlage der Ausschreibung existiren noch keine Rennen!
					if (announcement.getRaces() != null) {
						ArrayList<Race> races = new ArrayList<>(announcement.getRaces());
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
							table.addCell(new Phrase(race.getBoatClass().getText() + " " + race.getGender().getText()
									+ " " + race.getAgeType().getText(), defaultFont));
							table.addCell(new Phrase(race.getDistance() + " m", defaultFont));
							table.addCell(new Phrase());
						}
					}
					addToDocument(table);
					return "";
				default:
					return null;
				}
			}
		}

		private Stack<Context> currentContext = new Stack<>();

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
				closeCurrentElement();
				currentElement = new Paragraph();
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
				context.processStyle();
				break;
			case div:
				flushText(context);
				closeCurrentElement();
				currentElement = new PdfDiv();
				context.processStyle();
				break;
			case blockquote:
				closeCurrentElement();
				currentIndent += 15;
				break;
			case ul:
				closeCurrentElement();
				currentList = new List(false);
				break;
			case ol:
				closeCurrentElement();
				currentList = new List(true);
				break;
			case table:
				// schließe den vorherigen Abschnitt nur wenn es sich nicht um
				// eine geschachtelte Tabelle handelt
				flushText(context);
				if (currentTable.isEmpty())
					closeCurrentElement();
				currentTable.push(new Table());
				context.processStyle();
				break;
			case td:
				PdfPCell cell = new PdfPCell();
				Integer colspan = context.getAttributeValue("colspan", Integer.class);
				if (colspan != null)
					cell.setColspan(colspan);
				Integer rowspan = context.getAttributeValue("rowspan", Integer.class);
				if (rowspan != null)
					cell.setRowspan(rowspan);
				currentTable.peek().cells.add(cell);
				context.processStyle();
				break;
			case img:
				// https://localhost:8443/planner/javax.faces.resource/ckeditor/plugins/smiley/images/regular_smile.png.xhtml?ln=primefaces-extensions&amp;v=6.1.1
				// ist
				// META-INF/resources/primefaces-extensions/ckeditor/plugins/smiley/images/regular_smile.png
				String src = context.getAttributeValue("src", String.class);
				final String resource = "javax.faces.resource";
				int index = src.indexOf(resource);
				if (index != -1) {
					src = src.substring(index + resource.length());
					index = src.indexOf(".xhtml?");
					if (index != -1)
						src = src.substring(0, index);
					addImage(context, getClass().getClassLoader()
							.getResourceAsStream("/META-INF/resources/primefaces-extensions" + src));
				} else if (src.startsWith("data:")) {
					index = src.indexOf("base64,");
					if (index != -1) {
						byte[] bytes = Base64.getDecoder().decode(src.substring(index + 7));
						addImage(context, new ByteArrayInputStream(bytes));
					}
				}
				break;
			}
		}

		private void addImage(Context context, InputStream in) {
			if (in == null)
				return;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				IOUtils.copy(in, out);
				Image image = Image.getInstance(out.toByteArray());
				Float width = context.getAttributeValue("width", Float.class);
				Float height = context.getAttributeValue("height", Float.class);
				if (height != null || width != null) {
					image.scaleAbsolute(width != null ? width : image.getWidth(),
							height != null ? height : image.getHeight());
				} else {
					width = context.getSizeValue(context.getStyleValue("width", String.class));
					height = context.getSizeValue(context.getStyleValue("height", String.class));
					Float border = context.getSizeValue(context.getStyleValue("border-width", String.class));
					if (height != null || width != null)
						image.scaleAbsolute(width != null ? width : image.getWidth(),
								height != null ? height : image.getHeight());
					if (border != null) {
						image.setBorderWidth(border);
						image.setBorder(Rectangle.BOX);
					}

				}
				addToCurrentElement(image);
			} catch (Exception e) {
				throw new IllegalArgumentException("Problem with adding an image", e);
			}
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
				closeCurrentElement();
				break;
			case div:
				addText(text, context);
				context.applyStyle(currentElement);
				break;
			case blockquote:
				closeCurrentElement();
				currentIndent -= 15;
				break;
			case b:
			case i:
			case u:
			case s:
			case sub:
			case sup:
				// case span:
				// addText(text, context);
				// break;
			case h1:
				createSection(text, 0, context);
				break;
			case h2:
				createSection(text, 1, context);
				break;
			case h3:
				createSection(text, 2, context);
				break;
			case br:
				addText(text, context);
				addToCurrentElement(Chunk.NEWLINE);
				break;
			case hr:
				LineSeparator separator = new LineSeparator();
				separator.setPercentage(100);
				addToCurrentElement(new Chunk(separator));
				break;
			case li:
				if (currentElement != null && !(currentElement instanceof Phrase))
					closeCurrentElement();
				Phrase phrase = currentElement instanceof Phrase ? (Phrase) currentElement : new Phrase();
				if (text.length() > 0)
					phrase.add(new Chunk(text, context.font));
				ListItem item = new ListItem(phrase);
				item.setFont(context.font);
				if (!currentList.isNumbered()) {
					item.setListSymbol(
							new Chunk((char) 183 + "   ", new Font(FontFamily.SYMBOL, context.font.getSize()))); // bullet
				}
				currentList.add(item);
				currentElement = null;
				break;
			case ul:
			case ol:
				addToDocument(currentList);
				break;
			case td:
				Table table = currentTable.peek();
				PdfPCell cell = table.getCurrentCell();
				context.applyStyle(cell);
				if (cell.getTable() == null && cell.getCompositeElements() == null) {
					cell.setPhrase(
							currentElement instanceof Phrase ? (Phrase) currentElement : new Phrase(text, defaultFont));
				}
				currentElement = null;
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
				String width = context.getStyleValue("width", String.class);
				if (width != null) {
					if (width.endsWith("%"))
						pdfPTable.setWidthPercentage(Integer.parseInt(width.substring(0, width.length() - 1)));
					else if (width.endsWith("px"))
						pdfPTable.setTotalWidth(Integer.parseInt(width.substring(0, width.length() - 2)));
				}
				Integer border = context.getAttributeValue("border", Integer.class);
				Integer cellspacing = context.getAttributeValue("cellspacing", Integer.class);
				Integer cellpadding = context.getAttributeValue("cellpadding", Integer.class);
				String align = context.getAttributeValue("align", String.class);
				if (align != null)
					pdfPTable.setHorizontalAlignment(context.getAlignment(align));
				for (PdfPCell c : table.cells) {
					if (border == null || border == 0)
						c.disableBorderSide(Rectangle.BOX);
					else
						c.setBorderWidth(border);
					if (cellpadding != null)
						c.setPadding(cellpadding);
					pdfPTable.addCell(c);
				}
				context.applyStyle(pdfPTable);
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
			addToCurrentElement(chunk);
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
				addToCurrentElement(Chunk.NEXTPAGE);
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
				throw new FachlicheException(messages.getBundle(), "generator.unknownKeyword", key);
			}
		}

		/*
		 * Erzeugt ein Kapitel. Um den Effekt, dass Kapitelüberschriften, auf die ein Bericht folgt auf der vorherigen
		 * Seite stehen bleiben, wenn der Platz für den Bericht nicht mehr ausreicht, dürfen die Kapitel nicht sofort
		 * dem Dokument hinzugefügt werden.
		 */
		private void createSection(String text, int index, Context context) throws SAXException {
			closeCurrentSection(index);
			Chunk chunk = new Chunk(text, hFonts[index]);
			Paragraph paragraph = new Paragraph(chunk);
			paragraph.setAlignment(context.getAlignment(null));

			if (index > 0 && currentSection[index - 1] == null)
				throw new SAXException(new FachlicheException(messages.getBundle(), "generator.emptySection"));
			currentSection[index] = index == 0 ? new Chapter(paragraph, ++chapterNumber)
					: currentSection[index - 1].addSection(paragraph);
			currentSection[index].setTriggerNewPage(false);
			currentSection[index].setNumberStyle(Section.NUMBERSTYLE_DOTTED_WITHOUT_FINAL_DOT);
			chunk.setLocalDestination(currentSection[index].getTitle().getContent());
			if (pendingSection == null)
				pendingSection = currentSection[index];
		}
	}

	private abstract class RaceTableCreator {
		protected final String messageKey;
		protected final int[] titleSpans;
		protected PdfPTable table;

		protected RaceTableCreator(String messageKey, int... titleSpans) {
			this.messageKey = messageKey;
			this.titleSpans = titleSpans;
		}

		public void reset() {
			table = createTable();
		}

		protected abstract PdfPTable createTable();

		protected abstract void addRow(ProgramRace programRace, Race race, Font italic);

		protected PdfPTable createTable(float[] widths) {
			PdfPTable table = new PdfPTable(widths);
			table.setWidthPercentage(100);
			// für calculateHeights
			table.setTotalWidth((PageSize.A4.getWidth() - document.leftMargin() - document.rightMargin())
					* table.getWidthPercentage() / 100);
			table.getDefaultCell().disableBorderSide(Rectangle.BOX);
			table.getDefaultCell().setPadding(3);
			return table;
		}

		void addHeader(Font raceFont, DateFormat dfDay, DateFormat dfTime, ProgramRace programRace, Race race) {
			PdfPCell cell = new PdfPCell(new Phrase(renderer.renderRaceTitle(programRace), raceFont));
			cell.setColspan(titleSpans[0]);
			cell.disableBorderSide(Rectangle.BOX);
			cell.setMinimumHeight(20);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase(renderer.renderRaceNumber(programRace), defaultFont));
			cell.setColspan(titleSpans[1]);
			cell.disableBorderSide(Rectangle.BOX);
			cell.setMinimumHeight(20);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase(race.getDistance() + " m", defaultFont));
			cell.setColspan(titleSpans[2]);
			cell.disableBorderSide(Rectangle.BOX);
			cell.setMinimumHeight(20);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase(race.getBoatClass().getText() + " " + race.getAgeType().getText() + " "
					+ race.getGender().getText(), defaultFont));
			cell.setColspan(titleSpans[0]);
			cell.setBorder(Rectangle.BOTTOM);
			table.addCell(cell);

			String raceMode = renderer.renderRaceMode(programRace);
			cell = new PdfPCell(new Phrase(raceMode, defaultFont));
			cell.setColspan(titleSpans[1]);
			cell.setNoWrap(true);
			cell.setBorder(Rectangle.BOTTOM);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase(
					dfDay.format(programRace.getStartTime()) + " " + dfTime.format(programRace.getStartTime()),
					defaultFont));
			cell.setColspan(titleSpans[2]);
			cell.setBorder(Rectangle.BOTTOM);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			table.addCell(cell);
		}

		protected void addTeams(ProgramRaceTeam team, Race race, Font italic) {
			PdfPTable teamTable = new PdfPTable(2);
			teamTable.getDefaultCell().disableBorderSide(Rectangle.BOX);
			teamTable.getDefaultCell().setPadding(0);
			java.util.List<TeamMember> members = team.getMembers();
			int normalTeamSize = race.getBoatClass().getMaximalTeamSize();
			int m = Math.min(normalTeamSize, members.size());
			if (m == 1)
				teamTable.getDefaultCell().setColspan(2);
			int col = 0;
			for (int j = 0; j < m; j++) {
				TeamMember member = members.get(j);
				User user = member.getUser();

				if (member.getRemark() != null) {
					teamTable.addCell(new Phrase(member.getRemark(), italic));
				} else {
					String userName = user.getFirstName() + " " + user.getLastName();
					String ageGroup = renderer.renderAgeGroup(user);
					if (ageGroup != null)
						userName += " " + ageGroup;
					if (!team.getClub().getId().equals(user.getClub().getId()))
						userName += " (" + user.getClub().getShortNameOrName() + ")";
					teamTable.addCell(new Phrase(userName, defaultFont));
				}
				col++;
			}
			if (members.size() > normalTeamSize) {
				for (int j = normalTeamSize; j < members.size(); j++) {
					TeamMember member = members.get(j);
					User user = member.getUser();

					teamTable.addCell(new Phrase(messages.format("programs.replacement", user.getName()), italic));
					col++;
				}
			}
			if ((col % 2) != 0)
				teamTable.addCell("");

			table.getDefaultCell().setColspan(2);
			table.addCell(teamTable);
			table.getDefaultCell().setColspan(1);

			table.addCell(new Phrase(team.getClub().getShortNameOrName(), defaultFont));
		}

		protected void addFollowUpHint(ProgramRace programRace, Font italic, int colspan) {
			String hint = renderer.renderFollowUpHint(programRace);
			if (StringUtils.isNotEmpty(hint)) {
				PdfPCell cell = new PdfPCell(new Phrase(hint, italic));
				cell.setColspan(colspan);
				cell.disableBorderSide(Rectangle.BOX);
				table.addCell(cell);
			}
		}
	}

	private class ProgramCellCreator extends RaceTableCreator {
		ProgramCellCreator() {
			super("program", 2, 1, 1);
		}

		@Override
		protected PdfPTable createTable() {
			return createTable(new float[] { .03f, .35f, .35f, .27f });
		}

		@Override
		protected void addRow(ProgramRace programRace, Race race, Font italic) {
			Collection<ProgramRaceTeam> participants = programRace.getParticipants();
			if (participants == null)
				return;
			for (ProgramRaceTeam raceTeam : participants) {
				table.addCell(new Phrase(Integer.toString(raceTeam.getLane()), defaultFont));
				addTeams(raceTeam, race, italic);
			}
			addFollowUpHint(programRace, italic, 4);
		}
	}

	private class ResultCellCreator extends RaceTableCreator {
		private BaseFont captionFont;
		private Font deficitFont;

		ResultCellCreator() {
			super("result", 3, 1, 2);
			captionFont = defaultFont.getCalculatedBaseFont(false);
			deficitFont = new Font(defaultFont);
			deficitFont.setSize(defaultFont.getSize() / 1.2f);
			deficitFont.setColor(BaseColor.GRAY);
		}

		@Override
		protected PdfPTable createTable() {
			return createTable(new float[] { .03f, .03f, .31f, .31f, .26f, .06f });
		}

		@Override
		protected void addRow(ProgramRace programRace, Race race, Font italic) {
			java.util.List<Placement> placements = programRace.getPlacements();
			if (placements == null)
				return;
			for (int i = 0; i < placements.size(); i++) {
				Placement placement = placements.get(i);
				ProgramRaceTeam raceTeam = placement.getTeam();

				if (renderer.isSeparatorRendered(programRace, placement, i + 1)) {
					QualificationIndicator indicator = new QualificationIndicator(placement.getQualifiedFor(),
							captionFont);
					table.getDefaultCell().setCellEvent(indicator);
					table.setTableEvent(indicator);
				} else {
					table.getDefaultCell().setCellEvent(null);
				}

				table.addCell(new Phrase(Integer.toString(placement.getPosition()), defaultFont));
				table.addCell(new Phrase(Integer.toString(raceTeam.getLane()), defaultFont));

				addTeams(raceTeam, race, italic);

				Phrase phrase;
				if (placement.getTime() != null) {
					phrase = new Phrase(placementConverter.getAsString(null, null, placement.getTime()), defaultFont);
					phrase.add(Chunk.NEWLINE);
					Long deficit = renderer.computeDeficit(programRace, placement);
					if (deficit > 0)
						phrase.add(new Phrase("+" + placementConverter.getAsString(null, null, deficit), deficitFont));
					else
						phrase.add(new Chunk("\r", deficitFont));
				} else {
					phrase = new Phrase("", defaultFont);
				}
				table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
				table.addCell(phrase);
				table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
			}
			table.getDefaultCell().setCellEvent(null);
			addFollowUpHint(programRace, italic, 6);
		}
	}

	private final Font[] hFonts = { new Font(FontFamily.HELVETICA, 14f, Font.BOLD),
			new Font(FontFamily.HELVETICA, 12, Font.BOLD), new Font(FontFamily.HELVETICA, 10f, Font.BOLD) };

	private final Font defaultFont = new Font(FontFamily.HELVETICA, 8f, Font.NORMAL);

	private final Font tocFont = new Font(FontFamily.HELVETICA, 8f, Font.NORMAL);

	private final Font footerFont = new Font(FontFamily.HELVETICA, 6f, Font.NORMAL);

	@Inject
	private Messages messages;

	@Inject
	private RenderBean renderer;

	@Inject
	private PlacementConverter placementConverter;

	private int chapterNumber;

	private Section pendingSection;

	private Section[] currentSection = new Section[3];

	private List currentList;

	private Stack<SaxHandler.Table> currentTable = new Stack<>();

	private Element currentElement;

	private float currentIndent;

	private Document document;

	private PdfWriter writer;

	// wir merken uns die Seite, auf der das Inhaltsverzeichnis hinzugefügt wurde
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

		ByteArrayOutputStream bos = setUpDocument(new Document());

		PdfPCell title = new PdfPCell(
				new Phrase(messages.format("generator.announcement", announcement.getName()), defaultFont));
		title.setBorder(Rectangle.BOTTOM);
		writer.setPageEvent(new HeaderWriter(null, title));

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
		generateFromProgram(program, new ProgramCellCreator(), out);
	}

	public void generateResult(Program program, OutputStream out) throws Exception {
		generateFromProgram(program, new ResultCellCreator(), out);
	}

	private void generateFromProgram(Program program, final RaceTableCreator creator, OutputStream out)
			throws Exception {

		ByteArrayOutputStream bos = setUpDocument(new Document(PageSize.A4, 36, 36, 60, 36));
		creator.reset();

		PdfPCell title = createProgramTitle(creator.messageKey, program.getAnnouncement().getName(),
				program.getAnnouncement().getStartDate());

		HeaderWriter headerWriter = new HeaderWriter(null, title);
		writer.setPageEvent(headerWriter);

		document.open();
		document.addCreator("Wettkampfplaner");

		Font raceFont = new Font(defaultFont);
		raceFont.setStyle(defaultFont.getStyle() | Font.BOLD);
		Font italic = new Font(defaultFont);
		italic.setStyle(defaultFont.getStyle() | Font.ITALIC);

		DateFormat dfDay = new SimpleDateFormat("EEEE");
		DateFormat dfTime = new SimpleDateFormat("HH:mm");

		Calendar currentStartTime = null;

		for (ProgramRace programRace : program.getRaces()) {

			if (currentStartTime != null) {
				int previousDay = currentStartTime.get(Calendar.DAY_OF_YEAR);
				currentStartTime.setTime(programRace.getStartTime());
				if (currentStartTime.get(Calendar.DAY_OF_YEAR) > previousDay) {
					addToDocument(creator.table);
					addToDocument(Chunk.NEXTPAGE);
					creator.reset();
					headerWriter.header.deleteLastRow();
					headerWriter.header.addCell(createProgramTitle(creator.messageKey,
							program.getAnnouncement().getName(), currentStartTime.getTime()));
				}
			} else {
				currentStartTime = Calendar.getInstance();
				currentStartTime.setTime(programRace.getStartTime());
			}

			Race race = programRace.getRace();

			int numRows = creator.table.getRows().size();

			creator.addHeader(raceFont, dfDay, dfTime, programRace, race);
			creator.addRow(programRace, race, italic);

			float tableHeight = creator.table.calculateHeights();
			if (tableHeight / (document.top() - document.bottomMargin()) > 1) {
				while (creator.table.getRows().size() > numRows)
					creator.table.deleteLastRow();
				addToDocument(creator.table);
				addToDocument(Chunk.NEXTPAGE);
				creator.reset();
				creator.addHeader(raceFont, dfDay, dfTime, programRace, race);
				creator.addRow(programRace, race, italic);
			}
		}
		addToDocument(creator.table);

		closeCurrentSection(0);

		finishDocument(out, bos);
	}

	private PdfPCell createProgramTitle(String messageKey, String name, Date date) {
		PdfPCell title = new PdfPCell();
		title.setBorder(Rectangle.BOTTOM);
		Paragraph titleParagraph = new Paragraph(16, messages.format("generator." + messageKey, name), defaultFont);
		// paragraph.setSpacingBefore(16);
		title.addElement(titleParagraph);
		titleParagraph = new Paragraph(16, new SimpleDateFormat("EEEE, dd.MMMM yyyy").format(date), defaultFont);
		// paragraph.setSpacingAfter(16);
		title.addElement(titleParagraph);
		title.setPaddingTop(16);
		title.setPaddingBottom(8);
		return title;
	}

	private ByteArrayOutputStream setUpDocument(Document document) throws DocumentException {
		this.document = document;
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
			throw new FachlicheException(messages.getBundle(), "generator.empty");

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
				new Phrase(MessageFormat.format(messages.get("generator.footer"), page, total), footerFont));
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

	private void closeCurrentElement() throws SAXException {
		if (currentElement instanceof Indentable)
			((Indentable) currentElement).setIndentationLeft(currentIndent);
		Element e = currentElement;
		currentElement = null;
		addToDocument(e);
	}

	private void closeCurrentSection(int depth) throws SAXException {
		closeCurrentElement();
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
		Phrase phrase = new Paragraph(messages.get("generator.toc"));
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

	@SuppressWarnings("unchecked")
	private Element addToCurrentElement(Element child) {
		if (currentElement == null)
			currentElement = new Paragraph();
		if (currentElement instanceof java.util.List)
			((java.util.List<Object>) currentElement).add(child);
		else if (currentElement instanceof PdfDiv)
			((PdfDiv) currentElement).addElement(child);
		return currentElement;
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
			} else if (currentElement != null) {
				addToCurrentElement(element);
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
