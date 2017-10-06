package org.planner.business;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.MissingResourceException;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Message.RecipientType;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;
import org.planner.dao.PlannerDao;
import org.planner.eo.Role;
import org.planner.eo.Role_;
import org.planner.eo.User;
import org.planner.model.Suchkriterien;
import org.planner.util.CommonMessages;
import org.planner.util.LogUtil;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RegistryImpl {

	private static final Logger LOG = LoggerFactory.getLogger(RegistryImpl.class);

	@Inject
	private Messages messages;

	@Inject
	private PlannerDao plannerDao;

	@Inject
	private MasterDataServiceImpl masterData;

	@Resource(lookup = "java:jboss/mail/Default")
	private Session session;

	public String resetPassword(String token, String password) throws TechnischeException {
		try {
			User user = authenticate(token);
			if (user == null)
				return null;
			MessageDigest sha = MessageDigest.getInstance("SHA-512");
			byte[] digest = sha.digest(password.getBytes("UTF8"));
			// mit crlf ist JBoss etwas pissig!
			user.setPassword(new String(Base64.getMimeEncoder(76, new byte[] { '\n' }).encode(digest), "UTF8"));
			user.setToken(null);
			user.setTokenExpires(null);
			if (LOG.isInfoEnabled())
				LOG.info("Neues Passwort für " + user + " gesetzt.");
			// der Caller ist "anonymous", deshalb hier der Benutzer
			plannerDao.save(user, user.getUserId());

			String emailText = getFormattedMessage("email.passwordchanged.html", user.getFirstName(),
					user.getLastName());

			String subject = messages.getMessage("email.passwordchanged.subject");
			sendEmail(user, subject, emailText);

			return user.getUserId();
		} catch (Exception e) {
			LogUtil.handleException(e, LOG, "Fehler beim Dekodieren des Sicherheitstokens", token);
			return null;
		}
	}

	public User authenticate(String token) {
		if (token == null)
			return null;
		byte[] decoded = null;
		try {
			decoded = Base64.getDecoder().decode(token.getBytes("UTF8"));
		} catch (Exception e) {
			LOG.error("Fehler beim Dekodieren des Sicherheitstokens", e);
			return null;
		}
		int paddingLength = decoded[0];
		int idLength = decoded[paddingLength + 1];
		long id = readLong(decoded, paddingLength + 2, idLength);
		User user = plannerDao.getById(User.class, id);
		if (user == null || user.getToken() == null || user.getTokenExpires() == null)
			return null;

		byte[] hash = shaHash(user.getUserId(), user.getToken(), user.getTokenExpires());
		byte[] hashFromToken = new byte[64]; // digest length sha-512
		System.arraycopy(decoded, paddingLength + idLength + 2, hashFromToken, 0, hashFromToken.length);
		if (!Arrays.equals(hash, hashFromToken))
			return null;

		if (System.currentTimeMillis() > user.getTokenExpires()) {
			if (LOG.isInfoEnabled())
				LOG.info("Die Gültigkeit des Passwort-Reset- bzw. Registrations-Links für " //
						+ user + " ist abgelaufen.");
			plannerDao.saveToken(user.getId(), null, null);
			return null;
		}
		return user;
	}

	public String sendRegister(String email, String resetUrl) {
		User user = masterData.getUserByUserId(email, false);
		if (user != null) {
			// Ein Benutzer, der sich bereits angemeldet hatte, kann sich nicht
			// noch einmal registrieren
			if (user.getPassword() != null)
				return getFormattedMessage("user.exists", email);

			if (user.getToken() != null) {
				Long expires = user.getTokenExpires();
				long millisLeft = expires != null ? expires.longValue() - System.currentTimeMillis() : 0;
				if (millisLeft > 0)
					return getFormattedMessage("email.register.alreadysent",
							CommonMessages.niceTimeString((int) (millisLeft / 1000 / 60)));
			}
		} else {
			user = new User();
			user.setUserId(email);
			user.setEmail(email);
			user.setFirstName("");
			user.setLastName("");
			Suchkriterien krit = new Suchkriterien();
			krit.addFilter(Role_.role.getName(), "Sportwart");
			krit.setExact(true);
			krit.setIgnoreCase(false);
			List<Role> liste = plannerDao.search(Role.class, krit, null).getListe();
			// falls die Rolle nicht existiert, kann der Benutzer nichts machen
			if (liste.size() > 0) {
				Role role = liste.get(0);
				user.getRoles().add(role);
			}
			user.setClub(null);
			// Der Club muss nach der Erstanmeldung im Profil gesetzt werden
			// für den Sportwart gilt, dass er sich
			// auch seinen Verein anlegen muss
			plannerDao.save(user, email);
		}

		String token = UUID.randomUUID().toString();
		Long expiryDate = storeToken(user.getId(), token, 120);
		String emailToken = createEmailToken(user, token, expiryDate);

		String subject = messages.getMessage("email.register.subject");
		String emailText = getFormattedMessage("email.register.html",
				DateFormat.getDateTimeInstance().format(expiryDate), resetUrl + "?t=" + emailToken);

		sendEmail(user, subject, emailText);
		return getFormattedMessage("email.register.sent", email);
	}

	public String sendPasswortReset(String userId, String resetUrl) {
		User user = masterData.getUserByUserId(userId, false);
		return user != null ? internalSendPasswordReset(user, resetUrl) : messages.getMessage("user.unknown");
	}

	public String sendPasswortReset(Long id, String resetUrl) {
		if (id == null)
			return null;
		User user = plannerDao.getById(User.class, id);
		if (user == null)
			return null;
		return internalSendPasswordReset(user, resetUrl);
	}

	private String internalSendPasswordReset(User user, String resetUrl) {
		if (user.getToken() != null) {
			// es existiert bereits ein noch gültiges Sicherheitstoken.
			Long expires = user.getTokenExpires();
			long millisLeft = expires != null ? expires.longValue() - System.currentTimeMillis() : 0;
			if (millisLeft > 0)
				return getFormattedMessage("email.passwordreset.alreadysent",
						CommonMessages.niceTimeString((int) (millisLeft / 1000 / 60)));
		}
		String token = UUID.randomUUID().toString();
		Long expiryDate = storeToken(user.getId(), token, 120);
		String emailToken = createEmailToken(user, token, expiryDate);

		String emailText = getFormattedMessage("email.passwordreset.html", user.getFirstName(), user.getLastName(),
				DateFormat.getDateTimeInstance().format(expiryDate), resetUrl + "?t=" + emailToken);

		String subject = messages.getMessage("email.passwordreset.subject");
		sendEmail(user, subject, emailText);
		return getFormattedMessage("email.passwordreset.sent", user.getEmail());
	}

	private Long storeToken(Long userId, String token, int expiryMinutes) {
		// aktualisiere den User
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, expiryMinutes);
		Long tokenExpires = cal.getTime().getTime();
		plannerDao.saveToken(userId, token, tokenExpires);
		return tokenExpires;
	}

	private String createEmailToken(User user, String token, Long expiryDate) {
		String emailToken = null;
		try {
			/*
			 * Das Token hat folgende Zusammensetzung: - Länge der
			 * Random-Padding-Section (1 byte) - Random-Padding Section - Länge
			 * der User-ID in Bytes (1 byte) - User-ID - SHA-Hash (64 byte)
			 */
			byte[] id = new byte[8];
			// hänge die User-ID an das Token, um eine Identifikation zu
			// erleichtern
			// man könnte sonst auch alle gespeicherten Tokens durchprobieren
			int idLength = writeLong(id, user.getId());
			byte[] hash = shaHash(user.getUserId(), token, expiryDate);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int paddingLength = (int) (50 * Math.random());
			for (int i = 0; i < paddingLength; i++) {
				out.write((byte) (256 * Math.random()));
			}
			byte[] padding = out.toByteArray();
			byte[] tokenBytes = new byte[1 + padding.length + 1 + idLength + hash.length];
			tokenBytes[0] = (byte) paddingLength;
			System.arraycopy(padding, 0, tokenBytes, 1, padding.length);
			tokenBytes[padding.length + 1] = (byte) idLength;
			System.arraycopy(id, id.length - idLength, tokenBytes, padding.length + 2, idLength);
			System.arraycopy(hash, 0, tokenBytes, padding.length + 2 + idLength, hash.length);
			emailToken = new String(Base64.getEncoder().encode(tokenBytes), "UTF8");
			emailToken = URLEncoder.encode(emailToken, "UTF8");
		} catch (Exception e) {
			LogUtil.handleException(e, LOG, "Fehler beim Erzeugen des Sicherheitstokens", user);
		}
		return emailToken;
	}

	private String getFormattedMessage(String key, Object... arguments) {
		if (key.endsWith(".html")) {
			InputStream in = getClass().getClassLoader().getResourceAsStream("/" + key);
			if (in == null)
				throw new MissingResourceException("Can't find resource for key " + key, null, key);
			StringBuilder sb = new StringBuilder();
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				char buf[] = new char[500];
				for (int c; (c = r.read(buf)) != -1;) {
					sb.append(new String(buf, 0, c));
				}
			} catch (IOException e) {
				LOG.error("Cannot read html ressource " + key, e);
			}
			return MessageFormat.format(sb.toString(), arguments);
		} else {
			return messages.getFormattedMessage(key, arguments);
		}
	}

	private void sendEmail(User recipient, String subject, String msg) throws TechnischeException, FachlicheException {
		if (recipient.getEmail() == null)
			throw new FachlicheException(messages.getResourceBundle(), "no.email.stored", recipient.getUserId());
		try {
			MimeMessage message = new MimeMessage(session);
			message.setSubject(subject);
			MimeMultipart multipart = new MimeMultipart();
			MimeBodyPart bodyPart = new MimeBodyPart();
			bodyPart.setText(msg, "UTF-8", "html");
			multipart.addBodyPart(bodyPart);
			message.setContent(multipart);
			if (Boolean.getBoolean("devmode"))
				message.setFrom(new InternetAddress("uwe_ewald@yahoo.com"));
			else
				message.setFrom(new InternetAddress("noreply@planner.com", "Noreply Planner"));
			if (StringUtils.isNotBlank(recipient.getFirstName()) || StringUtils.isNotBlank(recipient.getLastName())) {
				message.addRecipient(RecipientType.TO, new InternetAddress(recipient.getEmail(),
						recipient.getFirstName() + " " + recipient.getLastName()));
			} else {
				message.addRecipient(RecipientType.TO, new InternetAddress(recipient.getEmail()));
			}
			Transport.send(message);
			if (LOG.isInfoEnabled())
				LOG.info("E-Mail mit Subject: " + subject + " an " + recipient.getEmail() + " versandt.");
		} catch (SendFailedException e) {
			throw new FachlicheException(messages.getResourceBundle(), "email.send.error", recipient.getEmail());
		} catch (Exception e) {
			LogUtil.handleException(e, LOG, "Fehler beim E-Mail-Versand", recipient);
		}
	}

	private byte[] shaHash(String userId, String token, long tokenExpires) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-512");
			// erzeuge eine Hashwert über ein Random-Token, den Anmeldenamen
			// sowie den Ablaufzeitpunkt
			return sha.digest((token + userId + tokenExpires).getBytes("UTF8"));
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Erstellen des SHA-Digest", e);
		}
	}

	private int writeLong(byte[] buf, long v) {
		buf[0] = (byte) (v >>> 56);
		buf[1] = (byte) (v >>> 48);
		buf[2] = (byte) (v >>> 40);
		buf[3] = (byte) (v >>> 32);
		buf[4] = (byte) (v >>> 24);
		buf[5] = (byte) (v >>> 16);
		buf[6] = (byte) (v >>> 8);
		buf[7] = (byte) (v >>> 0);
		for (int i = 0; i < buf.length; i++) {
			if (buf[i] > 0)
				return buf.length - i;
		}
		return buf.length;
	}

	private long readLong(byte[] buf, int offset, int length) {
		long result = 0;
		for (int i = offset + length - 1, x = 0; i >= offset; i--, x += 8) {
			result += (buf[i] & 255) << x;
		}
		return result;
	}
}
