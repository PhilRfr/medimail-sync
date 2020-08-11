package fr.philr.medimail.api;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MedimailMessage implements Comparable<MedimailMessage> {
	final String title;
	final int messageKey;
	final String senderEmail;
	final String date;
	String mboxMessage;
	final boolean opened;
	String corps;
	Map<String, byte[]> files;
	private Date javaDate;
	final String recipient;

	@Override
	public int hashCode() {
		return messageKey;
	}
	
	public MedimailMessage(String title, int messageKey, String senderEmail, String date,
			boolean opened, String recipient) {
		super();
		this.title = title;
		this.messageKey = messageKey;
		this.senderEmail = senderEmail;
		this.recipient = recipient;
		this.date = date;
		this.opened = opened;
		this.files = new HashMap<>();
		this.corps = "Non ouvert";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			javaDate = format.parse(date);
		} catch (ParseException e) {
			javaDate = new Date();
		}
		mboxMessage = "";
	}

	public boolean isOpened() {
		return opened;
	}

	public void download(MedimailSession session, Path target) {
		String url = "https://medimail.mipih.fr/?m=open&idk=" + messageKey;
		String html = session.getUrl(url);
		Document doc = Jsoup.parse(html);
		Elements corps = doc.select("#corps");
		this.corps = corps.select("#kvp_content").html();
		Elements params = corps.select("#kvp_param");
		for (Element subel : params) {
			String title = subel.select("h3").text().toLowerCase(Locale.FRANCE);
			if (title != null && title.contains("ocument")) {
				Elements links = subel.select("a");
				for (Element link : links) {
					String fileName = link.text();
					String fileUrl = link.attr("href");
					byte[] content = session.getBytes(fileUrl);
					files.put(fileName, content);
				}
				break;
			}
		}
		try {
			MimeMessage message = new MimeMessage((Session) null);
			message.setFrom(InternetAddress.parse(this.senderEmail)[0]);
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient+".from.medimail.mipih.fr"));
			message.setSubject(title);
			message.setSentDate(javaDate);
			// create the message part
			MimeBodyPart content = new MimeBodyPart();
			// fill message
			content.setContent(this.corps, "text/html;charset=utf8");
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(content);
			// add attachments
			for (Entry<String, byte[]> attachFile : files.entrySet()) {
				MimeBodyPart attachment = new MimeBodyPart();
				String mimeType = URLConnection.guessContentTypeFromName(attachFile.getKey());
				mimeType = mimeType == null ? "application/octet-stream" : mimeType;
				DataSource source = new ByteArrayDataSource(attachFile.getValue(), mimeType);
				attachment.setDataHandler(new DataHandler(source));
				attachment.setFileName(attachFile.getKey());
				multipart.addBodyPart(attachment);
			}
			// integration
			message.setContent(multipart);
			// store file
			FileOutputStream fos = new FileOutputStream(target.toFile());
			message.writeTo(fos);
		} catch (MessagingException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}

	@Override
	public int compareTo(MedimailMessage arg) {
		return this.javaDate.compareTo(arg.javaDate);
	}
	
	@Override
	public String toString() {
		return this.title;
	}

	public String getFileName() {
		String base = this.messageKey + ".eml";
		return base.replaceAll("[^a-zA-Z éèçàùÉÈÇÀÙâôûîêÂÊÛÎÔ\\-0-9-_\\.]", "_");
	}

}