package org.planner.ui.util;

import java.io.IOException;
import java.io.OutputStream;

import org.planner.eo.Address;
import org.planner.eo.Announcement;
import org.planner.eo.Location;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.DocumentFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfCreator {

	public static void createAnnouncent(Announcement announcement, OutputStream out)
			throws DocumentException, IOException {
		Document doc = new Document();
		PdfWriter.getInstance(doc, out);
		doc.open();
		doc.add(new Chunk(""));
		doc.addTitle("Regattaausschreibung " + announcement.getName());
		BaseFont font = DocumentFont.createFont();
		// font.set
		PdfPTable table = new PdfPTable(2);
		table.addCell(new PdfPCell(new Phrase("Regattaname:")));
		table.addCell(announcement.getName());
		table.addCell(new PdfPCell(new Phrase("Regattaort:")));
		table.addCell(getLocationName(announcement.getLocation()));
		table.addCell(new PdfPCell(new Phrase("Homepage:")));
		table.addCell(getLocationName(announcement.getAnnouncer()));
		doc.add(table);
		doc.close();
	}

	private static String getLocationName(Location location) {
		Address address = location.getAddress();
		return getAddressString(address != null ? address : location.getClub().getAddress());
	}

	private static String getAddressString(Address address) {
		StringBuilder sb = new StringBuilder();
		return sb.append(address.getCity().getName()).append(" ").append(address.getStreet()).append(" ")
				.append(address.getNumber()).toString();
	}
}
