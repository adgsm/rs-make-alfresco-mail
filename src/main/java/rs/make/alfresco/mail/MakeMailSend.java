package rs.make.alfresco.mail;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.alfresco.util.Pair;
import org.apache.log4j.Logger;

import rs.make.alfresco.globalproperties.GlobalProperties;

public class MakeMailSend {
	private final String SMTP_AUTH = "mail.smtp.auth";
	private final String SMTP_USERNAME = "mail.username";
	private final String SMTP_PASSWORD = "mail.password";
	private final String SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
	private final String SMTP_HOST = "mail.host";
	private final String SMTP_PORT = "mail.smtp.port";
	private final String REPLY_TO = "momcilo@dzunic.net"; // (comma separated list) TODO set in properties and use it from in there as above

	protected GlobalProperties globalProperties;
	public GlobalProperties getGlobalProperties() {
		return globalProperties;
	}
	public void setGlobalProperties( GlobalProperties globalProperties ) {
		this.globalProperties = globalProperties;
	}

	private static Logger logger = Logger.getLogger( MakeMailSend.class );

	public void init( String addressListTo , String addressListCc , String addressListBcc , String subject , String body , Map<Pair<String,String>,byte[]> attachments , boolean flagged ) throws Exception{
		Properties mailServerProperties = new Properties();
		mailServerProperties = System.getProperties();
		logger.debug( "Retreiving global properties and setting mailer." );
		mailServerProperties.put( "mail.smtp.auth" , globalProperties.getProperty( SMTP_AUTH ) );
		mailServerProperties.put( "mail.smtp.starttls.enable" , globalProperties.getProperty( SMTP_STARTTLS_ENABLE ) );
		mailServerProperties.put( "mail.smtp.host" , globalProperties.getProperty( SMTP_HOST ) );
		mailServerProperties.put( "mail.smtp.port" , globalProperties.getProperty( SMTP_PORT ) );
		Session session = Session.getInstance( mailServerProperties ,
			new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					String username = null;
					String password = null;
					try {
						username =  globalProperties.getProperty( SMTP_USERNAME );
						password = globalProperties.getProperty( SMTP_PASSWORD );
					} catch (Exception e) {
						e.printStackTrace();
					}
					return new PasswordAuthentication( username , password );
				}
			}
		);
		session.setDebug( false );

		logger.debug( "Composing message." );
		Message message = new MimeMessage( session );
		message.setSentDate( new Date() );
		message.setFlag( Flag.FLAGGED , flagged );
		message.setReplyTo( InternetAddress.parse( REPLY_TO ) );
		message.setFrom( new InternetAddress( globalProperties.getProperty( SMTP_USERNAME ) ) );

		if( addressListTo != null ) message.setRecipients( Message.RecipientType.TO , InternetAddress.parse( addressListTo ) );
		if( addressListCc != null ) message.setRecipients( Message.RecipientType.CC , InternetAddress.parse( addressListCc ) );
		if( addressListBcc != null ) message.setRecipients( Message.RecipientType.BCC , InternetAddress.parse( addressListBcc ) );
		if( subject != null ) message.setSubject( subject );

		logger.debug( String.format( "Set - To: %s, Cc: %s, Bcc: %s." , addressListTo , addressListCc , addressListBcc ) );
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText( body , "utf-8" , "html" );
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart( messageBodyPart );

		logger.debug( "Checking attachments." );
		if( attachments != null ){
			for( Entry<Pair<String,String>,byte[]> attachment : attachments.entrySet() ){
				MimeBodyPart messageAttachmentPart = new MimeBodyPart();
				DataHandler dataHandler = new DataHandler( attachment.getValue() , attachment.getKey().getSecond() );
				messageAttachmentPart.setDataHandler( dataHandler );
				messageAttachmentPart.setFileName( attachment.getKey().getFirst() );
				multipart.addBodyPart( messageAttachmentPart );
			}
		}

		message.setContent( multipart );

		logger.debug( "Sending email." );
		Transport.send( message );
	}
}
