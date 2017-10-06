package org.planner.ui.util;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.planner.eo.Announcement;
import org.planner.ui.util.text.TextFormat.Keyword;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.tidy.Lexer;
import org.w3c.tidy.Node;
import org.w3c.tidy.Out;
import org.w3c.tidy.OutJavaImpl;
import org.w3c.tidy.PPrint;
import org.w3c.tidy.Tidy;
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

/**
 * Erzeugt einen Bericht, der aus Textbestandteilen sowie Übersichtsdiagrammen besteht. Die Klasse ist nicht
 * Thread-safe. Es muss deshalb für jeden Request eine neue Instanz erzeugt werden.
 * 
 * @author Uwe Voigt, IBM
 */
public class BerichtGenerator {
	private class HeaderLogo extends PdfPageEventHelper {
		private Image kundenLogo;

		private Image hdiLogo;

		private HeaderLogo(byte[] imageData) throws Exception {
			final int dim = 50;
			if (imageData != null) {
				kundenLogo = Image.getInstance(imageData);
				kundenLogo.scaleToFit(dim, dim);
				kundenLogo.setAbsolutePosition(20, PageSize.A4.getTop() - kundenLogo.getScaledHeight() - 20);
			}
			hdiLogo = Image.getInstance(getClass().getResource("/images/hdi_logo.png"));
			hdiLogo.scaleToFit(dim, dim);
			hdiLogo.setAbsolutePosition(PageSize.A4.getRight() - hdiLogo.getScaledWidth() - 20,
					PageSize.A4.getTop() - hdiLogo.getScaledHeight() - 20);
		}

		@Override
		public void onStartPage(PdfWriter writer, Document document) {
			if (isToc)
				return;
			try {
				// Rücke den gesamten Inhalt etwas weiter nach unten, damit es
				// zu keiner Kollision mit den Logos kommt
				document.add(Chunk.NEWLINE);
				document.add(Chunk.NEWLINE);
				document.add(Chunk.NEWLINE);
			} catch (DocumentException e) {
				throw new IllegalArgumentException(e);
			}
		}

		@Override
		public void onEndPage(PdfWriter writer, Document document) {
			if (isToc)
				return;
			try {
				if (kundenLogo != null)
					writer.getDirectContent().addImage(kundenLogo);
				writer.getDirectContent().addImage(hdiLogo);
			} catch (DocumentException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	private enum Action {
		html, head, title, body, //
		h1, h2, h3, //
		br, //
		blockquote, //
		div, span, //
		table, tbody, tr, td, //
		ul, ol, li, //
		font, //
		strong, b, em, i, u, //
		p, img
	}

	private class SaxHandler extends DefaultHandler {
		private class Context {
			private Action action;

			private Attributes attributes;

			private BaseColor bgColor;

			private Font font = defaultFont;

			private Context(Action action, Attributes attributes) {
				this.action = action;
				this.attributes = new AttributesImpl(attributes);
				if (!currentContext.isEmpty()) {
					Context parent = currentContext.peek();
					bgColor = parent.bgColor;
					font = parent.font;
				}
			}

			String getAttValueIgnoreCase(String name) {
				for (int i = 0, n = attributes.getLength(); i < n; i++) {
					String qn = attributes.getQName(i);
					if (name.equalsIgnoreCase(qn))
						return attributes.getValue(i);
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

		private Stack<Context> currentContext = new Stack<Context>();

		private StringBuilder currentText = new StringBuilder();

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
			case span:
				flushText(context);
				BaseColor bgColor = getColor(getStyle(attributes, "background-color"));
				if (bgColor != null)
					context.bgColor = bgColor;
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
				currentTable.peek().cells.add(new PdfPCell());
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
				cell.setBackgroundColor(getColor(context.getAttValueIgnoreCase("bgcolor")));
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
				if (table.columnNumber == null)
					table.columnNumber = table.cells.size();
				break;
			case table:
				table = currentTable.pop();
				PdfPTable pdfPTable = new PdfPTable(table.columnNumber);
				for (PdfPCell c : table.cells) {
					pdfPTable.addCell(c);
				}
				addToDocument(pdfPTable);
				break;
			}
			currentText.setLength(0);
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			currentText.append(ch, start, length);
		}

		private void addText(String text, Context context) throws SAXException {
			if (text.length() == 0)
				return;
			Chunk chunk = new Chunk(text, context.font);
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
				return replaceSubstitutes(text, context);
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}

		private StringBuilder replaceSubstitutes(StringBuilder text, Context context) throws Exception {
			int begin = text.indexOf("{");
			if (begin == -1)
				return text;
			int end = text.indexOf("}", begin);
			if (end == -1)
				return text;
			String key = text.substring(begin + 1, end);
			Keyword keyword = Keyword.valueOf(key);
			end++;
			String replacement = null;
			switch (keyword) {
			default:
				throw new IllegalArgumentException("Unbehandelte Substitution: " + key);

				// case accountNumbers:
				// StringBuilder sb = new StringBuilder();
				// for (FirmaTO f :
				// RisikoVerwaltenGFOGFO.getInstance().findeFirmen(kundeId)) {
				// if (sb.length() > 0)
				// sb.append(", ");
				// sb.append(f.getMitglVersNr());
				// }
				// replacement = sb.toString();
				// break;
				// case groupName:
				// replacement = getKonzern().getNameMaske();
				// break;
				// case sumInsurance:
				// BirtDokumentSuchParameterTO params = new
				// BirtDokumentSuchParameterTO();
				// params.setKundeId(kundeId);
				// params.setSpracheId(ReportingDZO.getInstance().findeSpracheId(Util.gibBenutzerSprache()).intValue());
				// Zahlenwert summe = new Zahlenwert();
				// for (RisikotorteTO to :
				// ReportingDZO.getInstance().findeRisikotorte(params)) {
				// if (to.getFvs() != null)
				// summe = summe.add(to.getFvs());
				// }
				// replacement =
				// DecimalFormat.getNumberInstance(Util.gibBenutzerLocale()).format(summe.toNumber())
				// + " Mio €";
				// break;
				// case currentDate:
				// replacement = DateFormat.getDateInstance(DateFormat.LONG,
				// Util.gibBenutzerLocale()).format(new Date());
				// break;
				// case currentMonth:
				// replacement = new SimpleDateFormat("MMMM yyyy",
				// Util.gibBenutzerLocale()).format(new Date());
				// break;
				// case currentYear:
				// replacement = new SimpleDateFormat("yyyy",
				// Util.gibBenutzerLocale()).format(new Date());
				// break;
				// case personInChargeHGI:
				// replacement = getKonzern().getBetreuerHgi();
				// break;
				// case personInChargeTech:
				// replacement = getKonzern().getBetreuerTechn();
				// break;
				// case riskManager:
				// Long riskmanager = getKonzern().getRiskmanager();
				// if (riskmanager != null) {
				// BenutzerTO benutzer =
				// BerechtigungenVerwaltenGFOGFO.getInstance().gibBenutzer(riskmanager);
				// replacement = benutzer.getVorname() + " " +
				// benutzer.getName();
				// }
				// break;
				// case pageBreak:
				// // befindet sich das Page-Break-Tag mitten im Text,
				// // dann muss der vorherige Text erst dem Dokument hinzugefügt
				// // werden
				// addText(text.substring(0, begin), context);
				// getCurrentParagraph().add(Chunk.NEXTPAGE);
				// text.delete(0, end);
				// return replaceSubstitutes(text, context);
				// case toc:
				// pageNumToc = writer.getPageNumber();
				// break;
			}
			// text.replace(begin, end, replacement != null ? replacement : "");
			// return replaceSubstitutes(text, context);
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
				throw new SAXException("TODO");// new
												// FachlicheException(Messages.getString("BerichtGenerator.0")));
			currentSection[index] = index == 0 ? new Chapter(paragraph, ++chapterNumber)
					: currentSection[index - 1].addSection(paragraph);
			currentSection[index].setTriggerNewPage(false);
			currentSection[index].setNumberStyle(Section.NUMBERSTYLE_DOTTED_WITHOUT_FINAL_DOT);
			chunk.setLocalDestination(currentSection[index].getTitle().getContent());
			if (pendingSection == null)
				pendingSection = currentSection[index];
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(BerichtGenerator.class);

	private static final Font[] hFonts = { new Font(FontFamily.HELVETICA, 14f, Font.BOLD),
			new Font(FontFamily.HELVETICA, 12, Font.BOLD), new Font(FontFamily.HELVETICA, 10f, Font.BOLD) };

	private static final Font defaultFont = new Font(FontFamily.HELVETICA, 8f, Font.NORMAL);

	private static final Font tocFont = new Font(FontFamily.HELVETICA, 8f, Font.NORMAL);

	private static final Font footerFont = new Font(FontFamily.HELVETICA, 6f, Font.NORMAL);

	private int chapterNumber;

	private Section pendingSection;

	private Section[] currentSection = new Section[3];

	private List currentList;

	private Stack<SaxHandler.Table> currentTable = new Stack<SaxHandler.Table>();

	private Paragraph currentParagraph;

	private float currentIndent;

	private Document document;

	private PdfWriter writer;

	// private KonzernTO konzern;

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
	public void generate(Announcement announcement, OutputStream out) throws Exception {

		document = new Document(); // A4
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writer = PdfWriter.getInstance(document, bos);
		writer.setStrictImageSequence(true);
		writer.setLinearPageMode();

		try {
			// DokumentTO dokument = RisikoVerwaltenGFOGFO.getInstance()
			// .leseAttachmentKonzern(DownloadTyp.BildKonzern.ordinal(),
			// kundeId, null, null);
			// writer.setPageEvent(new HeaderLogo(null/* dokument.getData() */));
		} catch (FachlicheException e) {
			// kein Kunden-Logo
			writer.setPageEvent(new HeaderLogo(null));
		}

		document.open();
		document.addCreator("Wettkampfplaner");

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		SAXParser parser = factory.newSAXParser();
		SaxHandler handler = new SaxHandler();
		appendText(announcement.getText(), parser, handler);
		closeCurrentSection(0);

		if (pageNumToc != null)
			insertToc();

		// wir verhindern so die "The document has no pages"-Exceptions
		if (elementCount == 0)
			throw new FachlicheException(null, null);// TODO Messages.getString("BerichtGenerator.3"));

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
		table.getDefaultCell().setFixedHeight(20);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
		table.getDefaultCell().disableBorderSide(Rectangle.BOX);
		// table.addCell(new Phrase(Messages.getString("BerichtGenerator.2",
		// page, total), footerFont));
		return table;
	}

	private void appendText(String text, SAXParser parser, SaxHandler handler) throws Exception {
		// IE produziert keinen XHTML-Code. Deshalb wandeln wir den Text mittels Tidy
		if (text == null)
			return;
		Tidy tidy = new Tidy();
		tidy.setWraplen(0);
		tidy.setXHTML(true);
		tidy.setInputEncoding("UTF-8");
		tidy.setOutputEncoding("UTF-8");
		tidy.setTidyMark(false);
		tidy.setErrout(new PrintWriter(new ByteArrayOutputStream()));
		ByteArrayOutputStream o = new ByteArrayOutputStream(text.length());
		Node root = tidy.parse(new ByteArrayInputStream(text.getBytes("UTF8")), o);
		Out out = new OutJavaImpl(tidy.getConfiguration(), tidy.getInputEncoding(), o) {
			@Override
			public void newline() {
				//
			}
		};
		PPrint p = new PPrint(tidy.getConfiguration());
		p.printTree(out, (short) 0, 0, new Lexer(null, null, null), root);

		byte[] bytes = o.toByteArray();
		// ignoriere leere Texte
		if (bytes.length > 0) {
			try {
				parser.parse(new InputSource(new InputStreamReader(new ByteArrayInputStream(bytes), "UTF8")), handler);
			} catch (SAXException e) {
				if (e.getException() instanceof FachlicheException || e.getException() instanceof TechnischeException)
					throw e.getException();
				// LogUtil.handleException(e, LOG, "Fehler bei der Textanalyse",
				// o.toString());
			}
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
		Phrase phrase = new Paragraph("");// Messages.getString("BerichtGenerator.4"));
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
			if (!added && pendingSection == null)
				addToDocumentDirectly(element);
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
