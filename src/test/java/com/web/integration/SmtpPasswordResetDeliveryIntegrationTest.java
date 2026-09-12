package com.web.integration;

import com.web.service.impl.SmtpPasswordResetDeliveryService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Uses the actual Jakarta Mail SMTP client and a loopback-only test server; no external delivery occurs. */
@Timeout(15)
class SmtpPasswordResetDeliveryIntegrationTest {
    @Test
    void smtpTransportDeliversConfiguredSenderRecipientAndDecodedResetLink() throws Exception {
        try (LoopbackSmtp server = new LoopbackSmtp(false)) {
            SmtpPasswordResetDeliveryService delivery = delivery(server.port(), "reset@example.invalid");
            assertTrue(delivery.isAvailable());
            delivery.send("recipient@example.invalid", "test_nonce-with-dash");

            ReceivedMail received = server.result();
            assertEquals("MAIL FROM:<reset@example.invalid>", received.senderCommand());
            assertEquals(List.of("RCPT TO:<recipient@example.invalid>"), received.recipientCommands());
            assertNotNull(received.message());
            MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(received.message().getBytes(StandardCharsets.ISO_8859_1)));
            assertEquals("reset@example.invalid", ((InternetAddress) message.getFrom()[0]).getAddress());
            assertEquals("recipient@example.invalid", ((InternetAddress) message.getAllRecipients()[0]).getAddress());
            assertEquals("WEEB 密码重置", message.getSubject());
            String body = (String) message.getContent();
            assertTrue(body.contains("15分钟"));
            assertTrue(body.contains("https://weeb.example.invalid/reset-password?source=email&token=test_nonce-with-dash"));
        }
    }

    @Test
    void smtpRecipientFailurePropagatesInsteadOfPretendingDeliverySucceeded() throws Exception {
        try (LoopbackSmtp server = new LoopbackSmtp(true)) {
            SmtpPasswordResetDeliveryService delivery = delivery(server.port(), "reset@example.invalid");
            assertThrows(MailSendException.class, () -> delivery.send("rejected@example.invalid", "test-only-nonce"));
            ReceivedMail received = server.result();
            assertEquals(List.of("RCPT TO:<rejected@example.invalid>"), received.recipientCommands());
            assertNull(received.message(), "A rejected recipient must never receive a message body");
        }
    }

    @Test
    void missingSenderOrMailTransportIsExplicitlyUnavailable() {
        var empty = new StaticListableBeanFactory();
        var noTransport = new SmtpPasswordResetDeliveryService(empty.getBeanProvider(JavaMailSender.class),
                "https://weeb.example.invalid/reset-password", "reset@example.invalid");
        assertFalse(noTransport.isAvailable());
        assertThrows(IllegalStateException.class, () -> noTransport.send("recipient@example.invalid", "test-only-nonce"));
        assertFalse(delivery(1, "").isAvailable());
    }

    private static SmtpPasswordResetDeliveryService delivery(int port, String from) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("127.0.0.1");
        sender.setPort(port);
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", "false");
        properties.setProperty("mail.smtp.starttls.enable", "false");
        properties.setProperty("mail.smtp.connectiontimeout", "3000");
        properties.setProperty("mail.smtp.timeout", "3000");
        properties.setProperty("mail.smtp.writetimeout", "3000");
        properties.setProperty("mail.smtp.localhost", "localhost");
        sender.setJavaMailProperties(properties);
        var beans = new StaticListableBeanFactory();
        beans.addBean("mailSender", sender);
        return new SmtpPasswordResetDeliveryService(beans.getBeanProvider(JavaMailSender.class),
                "https://weeb.example.invalid/reset-password?source=email", from);
    }

    private record ReceivedMail(String senderCommand, List<String> recipientCommands, String message) { }

    private static final class LoopbackSmtp implements AutoCloseable {
        private final ServerSocket server;
        private final ExecutorService executor;
        private final Future<ReceivedMail> captured;
        private volatile Socket connection;

        private LoopbackSmtp(boolean rejectRecipient) throws Exception {
            server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            server.setSoTimeout(5000);
            executor = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "weeb-loopback-smtp-test");
                thread.setDaemon(true);
                return thread;
            });
            captured = executor.submit(() -> receive(rejectRecipient));
        }

        int port() { return server.getLocalPort(); }
        ReceivedMail result() throws Exception { return captured.get(5, TimeUnit.SECONDS); }

        private ReceivedMail receive(boolean rejectRecipient) throws Exception {
            try (Socket socket = server.accept()) {
                connection = socket;
                socket.setSoTimeout(5000);
                var input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
                var output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
                reply(output, "220 localhost test SMTP");
                String from = null;
                List<String> recipients = new ArrayList<>();
                String message = null;
                String line;
                while ((line = input.readLine()) != null) {
                    String command = line.toUpperCase(Locale.ROOT);
                    if (command.startsWith("EHLO") || command.startsWith("HELO")) {
                        reply(output, "250 localhost");
                    } else if (command.startsWith("MAIL FROM:")) {
                        from = line;
                        reply(output, "250 sender accepted");
                    } else if (command.startsWith("RCPT TO:")) {
                        recipients.add(line);
                        reply(output, rejectRecipient ? "550 recipient rejected by fixture" : "250 recipient accepted");
                    } else if (command.equals("DATA")) {
                        reply(output, "354 end data with a dot");
                        StringBuilder body = new StringBuilder();
                        while ((line = input.readLine()) != null && !line.equals(".")) {
                            body.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                        }
                        message = body.toString();
                        reply(output, "250 message accepted locally");
                    } else if (command.equals("QUIT")) {
                        reply(output, "221 closing connection");
                        break;
                    } else if (command.equals("RSET") || command.equals("NOOP")) {
                        reply(output, "250 OK");
                    } else {
                        reply(output, "500 unsupported test SMTP command");
                    }
                }
                return new ReceivedMail(from, List.copyOf(recipients), message);
            }
        }

        private static void reply(BufferedWriter output, String line) throws Exception {
            output.write(line + "\r\n");
            output.flush();
        }

        @Override
        public void close() throws Exception {
            server.close();
            if (connection != null) connection.close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        }
    }
}
