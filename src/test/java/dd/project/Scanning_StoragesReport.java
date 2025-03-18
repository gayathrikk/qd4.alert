package dd.project;
import com.jcraft.jsch.*;
import javax.mail.*;
import javax.mail.internet.*;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
public class Scanning_StoragesReport {
	private static final Map<String, String> MACHINES = new HashMap<>();

	static {
	        MACHINES.put("pp1.humanbrain.in", "/store/nvmestorage/postImageProcessor");
	        MACHINES.put("pp2.humanbrain.in", "/mnt/local/nvmestorage/postImageProcessor");
	        MACHINES.put("pp3.humanbrain.in", "/mnt/local/nvmestorage/postImageProcessor");
	        MACHINES.put("pp4.humanbrain.in", "/mnt/local/nvmestorage/postImageProcessor");
	        MACHINES.put("pp5.humanbrain.in", "/mnt/local/nvmestorage/postImageProcessor");
	        MACHINES.put("pp7.humanbrain.in", "/mnt/local/nvmestorage/postImageProcessor");
	        MACHINES.put("qd4.humanbrain.in", "/mnt/local/nvme2/postImageProcessor");
	    }

	    public static void main(String[] args) {
	        StringBuilder emailContent = new StringBuilder();
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	        String todayDate = sdf.format(new Date());

	        for (Map.Entry<String, String> entry : MACHINES.entrySet()) {
	            String machine = entry.getKey();
	            String directory = entry.getValue();

	            List<String> todayFiles = new ArrayList<>();
	            List<String> pendingFiles = new ArrayList<>();

	            // Connect to each machine and retrieve the files with their dates
	            checkFiles(machine, directory, todayDate, todayFiles, pendingFiles);

	            emailContent.append("================================== ").append(machine).append(" ==================================\n");

	            // Today's files
	            if (!todayFiles.isEmpty()) {
	                emailContent.append("📂 **Today's Files:**\n");
	                emailContent.append("-----------------------------------------------------------------------------------------------------------\n");
	                for (String file : todayFiles) {
	                    emailContent.append(file).append("\n");
	                }
	            } else {
	                emailContent.append("📅 No new files found for today.\n");
	            }

	            // Pending files
	            if (!pendingFiles.isEmpty()) {
	                emailContent.append("\n⚠ **Pending Files from Previous Days:**\n");
	                emailContent.append("------------------------------------------------------------------------------------------------------------------------------\n");
	                for (String file : pendingFiles) {
	                    emailContent.append(file).append("\n");
	                }
	            } else {
	                emailContent.append("\n🎉 **No pending files remaining!**\n");
	            }

	            emailContent.append("\n================================== ***DONE*** ==================================\n\n");
	        }

	        // Sending email alert
	        sendEmailAlert(emailContent.toString());
	    }

	    private static void checkFiles(String machine, String directory, String todayDate, List<String> todayFiles, List<String> pendingFiles) {
	        JSch jsch = new JSch();
	        com.jcraft.jsch.Session session = null;
	        
	        try {
	            String user = "hbp";
	            String host = machine;
	            String password = "Health#123";  // Use your correct password
	            int port = 22;
	            session = jsch.getSession(user, host, port);
	            session.setPassword(password);
	            session.setConfig("StrictHostKeyChecking", "no");
	            session.connect();

	            Channel channel = session.openChannel("exec");
	            ((ChannelExec) channel).setCommand("find " + directory + " -type f -exec ls -lh {} \\;");  // List files with details
	            channel.setInputStream(null);
	            ((ChannelExec) channel).setErrStream(System.err);
	            InputStream in = channel.getInputStream();
	            channel.connect();
	            byte[] tmp = new byte[1024];
	            StringBuilder output = new StringBuilder();

	            while (true) {
	                while (in.available() > 0) {
	                    int i = in.read(tmp, 0, 1024);
	                    if (i < 0) break;
	                    output.append(new String(tmp, 0, i));
	                }
	                if (channel.isClosed()) {
	                    if (in.available() > 0) continue;
	                    break;
	                }
	                try {
	                    Thread.sleep(1000);
	                } catch (Exception ee) {
	                    ee.printStackTrace();
	                }
	            }

	            String[] lines = output.toString().split("\n");

	            for (String line : lines) {
	                String[] parts = line.split("\\s+");
	                if (parts.length > 5) {
	                    String fileName = parts[8];
	                    String fileDate = parts[5] + " " + parts[6] + " " + parts[7];  // Getting the date from the file details
	                    
	                    if (isTodayFile(fileDate, todayDate)) {
	                        todayFiles.add(fileName + " (Date: " + fileDate + ")");
	                    } else {
	                        pendingFiles.add(fileName + " (Date: " + fileDate + ")");
	                    }
	                }
	            }

	            channel.disconnect();
	            session.disconnect();
	        } catch (Exception e) {
	            e.printStackTrace();
	            System.out.println("Error while checking files on machine " + machine + ": " + e.getMessage());
	        }
	    }

	    // Helper method to check if the file date matches today's date
	    private static boolean isTodayFile(String fileDate, String todayDate) {
	        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy");
	        return sdf.format(new Date()).equals(fileDate);
	    }

	    private static void sendEmailAlert(String messageBody) {
	        String to = "divya.d@htic.iitm.ac.in";  // Change this to your recipient email
	        String from = "gayathri@htic.iitm.ac.in";  // Your email
	        String host = "smtp.gmail.com";  // Use the correct SMTP server (Gmail in this case)
	        Properties properties = System.getProperties();
	        properties.put("mail.smtp.host", host);
	        properties.put("mail.smtp.port", "465");
	        properties.put("mail.smtp.ssl.enable", "true");
	        properties.put("mail.smtp.auth", "true");

	        javax.mail.Session session = javax.mail.Session.getInstance(properties, new javax.mail.Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication("gayathri@htic.iitm.ac.in", "Gayu@0918"); // Your credentials
	            }
	        });

	        try {
	            MimeMessage message = new MimeMessage(session);
	            message.setFrom(new InternetAddress(from));
	            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
	            message.setSubject("Scanning Machines Storage Report");
	            message.setText("File Report for " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ":\n\n" + messageBody);
	            System.out.println("Sending...");
	            Transport.send(message);
	            System.out.println("Email sent successfully!");
	        } catch (MessagingException mex) {
	            mex.printStackTrace();
	        }
	    }
	}

