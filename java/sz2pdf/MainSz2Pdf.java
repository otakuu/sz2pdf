package sz2pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import sz2pdf.beans.SzType;

public class MainSz2Pdf {

	final static Logger LOGGER = Logger.getLogger(MainSz2Pdf.class);
	private static String recipents;
	private static Properties props;
	private static String googleEmail;
	private static String googlePassword;
	private static String filepathMini;

	public static void main(final String[] args) {

		try {
			LOGGER.info("**************************");
			LOGGER.info("***** sz2pdf started *****");
			LOGGER.info("**************************");

			// get properties
			LOGGER.info("******* Properties *******");

			String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
			String appConfigPath = rootPath + "config.properties";

			Properties appProps = new Properties();

			appProps.load(new FileInputStream(appConfigPath));

			LOGGER.info("pdfPath: " + appProps.getProperty("pdfPath"));
			LOGGER.info("sz_user: " + appProps.getProperty("sz_user"));
			LOGGER.info("editionId: " + appProps.getProperty("editionId"));
			LOGGER.info("editionString: " + appProps.getProperty("editionString"));
			LOGGER.info("recipients: " + appProps.getProperty("recipients"));
			LOGGER.info("google email: " + appProps.getProperty("googleEmail"));
			LOGGER.info("google password: ******");

			googleEmail = appProps.getProperty("googleEmail");
			googlePassword = appProps.getProperty("googlePassword");

			props = System.getProperties();
			props.put("mail.smtp.starttls.enable", true);
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.user", appProps.getProperty("googleEmail"));
			props.put("mail.smtp.password", appProps.getProperty("googlePassword"));
			props.put("mail.smtp.port", "587");
			props.put("mail.smtp.auth", true);

			recipents = appProps.getProperty("recipients");

			// get pdf
			final LoginAndGetSzPdf loginAndGetPdfSZ = new LoginAndGetSzPdf();
			loginAndGetPdfSZ.setBaseUrl(appProps.getProperty("baseUrl"));
			loginAndGetPdfSZ.setLoginUrl(appProps.getProperty("loginUrl"));
			loginAndGetPdfSZ.setUsername(appProps.getProperty("sz_user"));
			loginAndGetPdfSZ.setPassword(appProps.getProperty("sz_password"));
			loginAndGetPdfSZ.setEditionId(Integer.parseInt(appProps.getProperty("editionId")));
			loginAndGetPdfSZ.setEditionString(appProps.getProperty("editionString"));
			loginAndGetPdfSZ.setPath(appProps.getProperty("pdfPath"));

			int initDelay = 1;

			for (int i = 1; i <= 10; i++) { // 10 retries

				initDelay = (int) Math.ceil(initDelay * 1.6);

				try {

					loginAndGetPdfSZ.getSecureSessionIdAndPages();
					filepathMini = loginAndGetPdfSZ.getPdf(SzType.Mini);

					LOGGER.info("Try to get Pdf. Nbr of attempt: " + i + "/10");
					LOGGER.info("Pdf generated: " + filepathMini + " (" + new File(filepathMini).length() / 1000000
							+ " MB)");

					if (new File(filepathMini).length() / 1000000 < 1) {

						LOGGER.error("Filesize is zero, retry in " + initDelay + " min: ");
						Thread.sleep(initDelay * 60 * 1000);

					} else {
						break;
					}
				} catch (Exception ex) {
					LOGGER.error("Error, retry in " + initDelay + "min: ", ex);
					Thread.sleep(initDelay * 60 * 1000);
				}

			}

			// after 10 retries still 0? then exit
			if (new File(filepathMini).length() / 1000000 < 1) {
				return;
			}

			File fileMini = new File(filepathMini);
			sendMailWithPdf(fileMini, "Solothurner Zeitung PDF - Mini");

			LOGGER.info("**************************");
			LOGGER.info("******* Email sent *******");
			LOGGER.info("**************************");

			// delete files
			Path path = Paths.get(filepathMini);
			Files.delete(path);

		} catch (

		Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

	}

	private static void sendMailWithPdf(File file, String subject) throws MessagingException, IOException {

		Session session = Session.getInstance(props, null);
		MimeMessage message = new MimeMessage(session);

		// Create the email addresses involved
		InternetAddress from = new InternetAddress(googleEmail);
		message.setSubject(subject);
		from.setPersonal("sz2pdf Service");
		message.setFrom(from);

		String[] recList = recipents.split(",");
		for (String rec : recList) {
			message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(rec));
		}

		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText("Enjoy your journal!");

		Calendar myDate = Calendar.getInstance(); // set this up however you need it.
		int dow = myDate.get(Calendar.DAY_OF_WEEK);

		// amtsblatt
		if (dow == Calendar.FRIDAY) {
			String url = "https://www.so.ch/fileadmin/internet/staatskanzlei/stk-regierungsdienste/pdf/amtsblatt/Amtsblatt_"
					+ myDate.get(Calendar.WEEK_OF_YEAR) + "-" + myDate.get(Calendar.YEAR) + ".pdf";
			LOGGER.info("Amtsblatt Url: " + url);
			messageBodyPart.setText("Amtsblatt online: " + url);
		}

		// Add attachment
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);

		messageBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(file.getAbsolutePath());
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(file.getName());
		multipart.addBodyPart(messageBodyPart);
		message.setContent(multipart);

		// Send message
		Transport transport = session.getTransport("smtp");
		transport.connect("smtp.gmail.com", googleEmail, googlePassword);
		LOGGER.info("Sending mail: " + transport.toString());
		transport.sendMessage(message, message.getAllRecipients());

	}

}
