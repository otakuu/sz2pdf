package sz2pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

import sz2pdf.beans.SzType;

public class MainSz2Pdf {

	final static Logger LOGGER = Logger.getLogger(MainSz2Pdf.class);
	private static String recipents;
	private static Properties props;
	private static String googleEmail;
	private static String googlePassword;
	private static String filepathMini;
	// private static String filepathAll;

	public static void main(final String[] args) {

		try {
			LOGGER.info("*************************");
			LOGGER.info("***** sz2pdf started ****");
			LOGGER.info("*************************");

			// get properties
			LOGGER.info("****** Properties *******");

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
			LOGGER.info("google password: " + appProps.getProperty("googlePassword"));
			LOGGER.info("google drive folderId: " + appProps.getProperty("googleFolderId"));

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
			loginAndGetPdfSZ.setUsername(appProps.getProperty("sz_user"));
			loginAndGetPdfSZ.setPassword(appProps.getProperty("sz_password"));
			loginAndGetPdfSZ.setEditionId(Integer.parseInt(appProps.getProperty("editionId")));
			loginAndGetPdfSZ.setEditionString(appProps.getProperty("editionString"));
			loginAndGetPdfSZ.setPath(appProps.getProperty("pdfPath"));
			loginAndGetPdfSZ.getSecureSessionIdAndPages();

			int initDelay = 1;

			for (int i = 1; i <= 10; i++) { // 10 retries

				initDelay = (int) Math.ceil(initDelay * 1.6);

				try {
					filepathMini = loginAndGetPdfSZ.getPdf(SzType.Mini);
					// filepathAll = loginAndGetPdfSZ.getPdf(SzType.All);

					LOGGER.info("Try to get Pdf. Nbr of attempt: " + i + "/10");
					LOGGER.info("Pdf generated: " + filepathMini + " (" + new File(filepathMini).length() / 1000000
							+ " MB)");

					if (new File(filepathMini).length() / 1000000 < 1) {

						LOGGER.error("Filesize is zero, retry in " + initDelay + " min: ");
						Thread.sleep(initDelay * 60 * 1000); // 30 min

					} else {
						break;
					}
				} catch (Exception ex) {
					LOGGER.error("Error, retry in 30min: ", ex);
					Thread.sleep(initDelay * 60 * 1000); // 30 min
				}

			}

			// after 10 retries still 0? then exit
			if (new File(filepathMini).length() / 1000000 < 1) {
				return;
			}

			// compress
			// compressPdf(filepathAll);
			//
			// // gmail can only send 25MB
			// if ((new File(filepathAll).length() / 1000000) > 25) { // max. 25
			// MB
			// // upload to google drive
			// UploadGoogleDrive myUploadGoogleDrive = new
			// UploadGoogleDrive(filepathAll,
			// appProps.getProperty("googleFolderId"));
			// LOGGER.info("Google Drive Link: " +
			// myUploadGoogleDrive.getUploadFileLink());
			// sendMailWithLink(myUploadGoogleDrive.getUploadFileLink());
			// } else {
			// // send pdf
			// File file = new File(filepathAll);
			// sendMailWithPdf(file, "Solothurner Zeitung PDF");
			// }

			File fileMini = new File(filepathMini);
			sendMailWithPdf(fileMini, "Solothurner Zeitung PDF - Mini");

			LOGGER.info("*************************");
			LOGGER.info("******* Email sent ******");
			LOGGER.info("*************************");

			// delete files
			// Path path = Paths.get(filepathAll);
			// Files.delete(path);

			Path path = Paths.get(filepathMini);
			Files.delete(path);

		} catch (

		Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

	}

	private static void compressPdf(String filepath) throws IOException, FileNotFoundException, DocumentException {
		PdfReader reader = new PdfReader(new FileInputStream(filepath));
		PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(filepath), PdfWriter.VERSION_1_5);
		stamper.setFullCompression();
		stamper.close();

		LOGGER.info("Pdf compressed: " + filepath + " (" + new File(filepath).length() / 1000 + " KB)");

	}

	private static void sendMailWithLink(String uploadFileLink)
			throws MessagingException, UnsupportedEncodingException {

		Session session = Session.getInstance(props, null);
		MimeMessage message = new MimeMessage(session);

		// Create the email addresses involved
		InternetAddress from = new InternetAddress(googleEmail);
		message.setSubject("Solothurner Zeitung PDF");
		from.setPersonal("sz2pdf Service");
		message.setFrom(from);

		String[] recList = recipents.split(",");
		for (String rec : recList) {
			message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(rec));
		}

		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText(uploadFileLink);

		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);
		message.setContent(multipart);

		// Send message
		Transport transport = session.getTransport("smtp");
		transport.connect("smtp.gmail.com", googleEmail, googlePassword);
		LOGGER.info("Sending mail: " + transport.toString());
		transport.sendMessage(message, message.getAllRecipients());

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
