package fr.philr.medimail.api;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.CredentialException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.dongliu.requests.Parameter;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class MedimailSession {

	private static Pattern PAGINATION_NUMBERS = Pattern.compile("Page (\\d+) sur (\\d+)");
	private static Pattern TITLE_ID = Pattern.compile("https://medimail.mipih.fr/\\?m=liste&f=(\\d+)&o=title");
	private static Pattern KVP_ID = Pattern.compile("\\d+");
	private final Session session;
	private final Set<MedimailMessage> messages;
	private final String username;
	private final String password;

	public Set<MedimailMessage> getMessages() {
		return messages;
	}

	public MedimailSession(String username, String password) throws CredentialException {
		session = Requests.session();
		messages = new HashSet<>();
		this.username = username;
		this.password = password;
		connect();
	}

	public Session getSession() {
		return session;
	}

	public String getUrl(String url) {
		return session.get(url).send().readToText();
	}

	public byte[] getBytes(String url) {
		return session.get(url).send().readToBytes();
	}

	private void extractMessages(Elements trs, boolean opened) {
		for (Element tr : trs.select("tr")) {
			String title = "";
			String sender = "";
			String date = "";
			int rank = 0;
			int messageId = -1;
			for (Element td : tr.select("td.middle")) {
				switch (rank) {
				case 1:
					title = td.text();
					break;
				case 2:
					sender = td.text();
					break;
				default:
					Matcher matcher = KVP_ID.matcher(td.attr("onclick"));
					matcher.find();
					messageId = Integer.parseInt(matcher.group(0));
					break;
				}
				rank++;
			}
			for (Element td : tr.select("td.right")) {
				date = td.text();
				break;
			}
			MedimailMessage mm = new MedimailMessage(title, messageId, sender, date, opened, username);
			messages.add(mm);
		}

	}

	public void fetchMessageHeaders() {
		String html = session.get("https://medimail.mipih.fr/").send().readToText();
		Document doc = Jsoup.parse(html);
		String pages = doc.select("#pagination").text();
		Matcher m = PAGINATION_NUMBERS.matcher(pages);
		m.find();
		int pageMax = Integer.parseInt(m.group(2));
		int folderID = -1;
		Elements allHref = doc.select("a[href]");
		for (Element link : allHref) {
			String url = link.attr("href");
			Matcher m1 = TITLE_ID.matcher(url);
			m1.find();
			if (m1.matches()) {
				folderID = Integer.parseInt(m1.group(1));
				break;
			}
		}
		for (int i = 1; i <= pageMax; i++) {
			System.out.println("Traitement de la page " + i + "...");
			String url = String.format("https://medimail.mipih.fr/?m=liste&f=%d&o=date&p=%d", folderID, i);
			String messagesList = session.get(url).send().readToText();
			Document pageDoc = Jsoup.parse(messagesList);
			Elements unopenMessages = pageDoc.select("tr.unopen");
			Elements openMessages = pageDoc.select("tr.open");
			extractMessages(unopenMessages, false);
			extractMessages(openMessages, true);
		}
	}
	
	

	public void connect() throws CredentialException {
		String result = session.post("https://medimail.mipih.fr/")
				.body(Parameter.of("login", username), Parameter.of("password", password)).send().readToText();
		if (result.contains("Echec de l\\'authentification.") || result.contains("Session invalide")
				|| result.contains("Le login doit être une adresse mail valide."))
			throw new CredentialException("Informations de connexion invalides.");
	}
}
