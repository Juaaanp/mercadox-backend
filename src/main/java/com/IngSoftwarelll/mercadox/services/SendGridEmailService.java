package com.IngSoftwarelll.mercadox.services;


import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.IngSoftwarelll.mercadox.models.Purchase;
import com.IngSoftwarelll.mercadox.models.PurchaseItem;
import com.IngSoftwarelll.mercadox.services.interfaces.EmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SendGridEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailService.class);

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name}")
    private String fromName;

    private final SendGrid sendGrid;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Async
    public void sendPurchaseConfirmation(Purchase purchase, String contactEmail) {

        log.info("Sending purchase confirmation email for purchase ID: {}", purchase.getId());

        try {

            log.info("Recipient email determined: {}", contactEmail);

            if (contactEmail == null || contactEmail.isBlank()) {
                log.error("No recipient email found for purchase ID: {}", purchase.getId());
                return;
            }

            String subject = String.format("✅ Confirmación de Compra - Orden #%d", purchase.getId());
            String htmlContent = buildPurchaseConfirmationHtml(purchase);

            log.info("Attempting to send email to: {}", contactEmail);

            boolean sent = sendEmail(contactEmail, subject, htmlContent);

            if (sent) {
                log.info("Purchase confirmation email sent successfully to: {}", contactEmail);
            } else {
                log.error("Failed to send purchase confirmation email to: {}", contactEmail);
            }

        } catch (Exception e) {
            log.error("Error sending purchase confirmation email for purchase ID: {}",
                    purchase.getId(), e);
        }
    }

    private boolean sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            Content content = new Content("text/html", htmlContent);

            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            int statusCode = response.getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                log.debug("Email sent successfully. Status: {}", statusCode);
                return true;
            } else {
                log.error("SendGrid returned error. Status: {}, Body: {}",
                        statusCode, response.getBody());
                return false;
            }

        } catch (IOException e) {
            log.error("IOException while sending email to: {}", toEmail, e);
            return false;
        }
    }

    /**
     * Construye el HTML del email de confirmación de compra
     */
    private String buildPurchaseConfirmationHtml(Purchase purchase) {
    StringBuilder html = new StringBuilder();

    html.append("<!DOCTYPE html>");
    html.append("<html lang='es'>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
    html.append("<style>");
    html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
    html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #051A2E; background-color: #E0E0E0; }");
    html.append(".container { max-width: 600px; margin: 20px auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
    html.append(".header { background: #051A2E; color: #FF9900; padding: 40px 20px; text-align: center; }");
    html.append(".header h1 { font-size: 28px; margin-bottom: 10px; }");
    html.append(".header p { font-size: 16px; opacity: 0.9; }");
    html.append(".content { padding: 30px; }");
    html.append(".order-info { background: #051A2E; padding: 20px; border-radius: 8px; margin-bottom: 20px; color: #E0E0E0; }");
    html.append(".order-info p { margin: 8px 0; font-size: 14px; }");
    html.append(".order-info strong { color: #FF9900; }");
    html.append(".products-title { font-size: 20px; font-weight: bold; margin: 30px 0 20px; color: #051A2E; }");
    html.append(".product-item { background: #E0E0E0; border: 2px solid #FF9900; border-radius: 8px; padding: 20px; margin-bottom: 15px; }");
    html.append(".product-name { font-size: 18px; font-weight: bold; color: #051A2E; margin-bottom: 10px; }");
    html.append(".product-price { font-size: 16px; color: #051A2E; margin-bottom: 15px; }");
    html.append(".code-section { background: #FF9900; padding: 15px; border-radius: 6px; margin: 10px 0; color: #051A2E; }");
    html.append(".code-label { font-size: 12px; font-weight: bold; text-transform: uppercase; margin-bottom: 5px; }");
    html.append(".code-value { font-family: 'Courier New', monospace; font-size: 16px; font-weight: bold; color: #051A2E; word-break: break-all; }");
    html.append(".footer { background: #051A2E; padding: 20px; text-align: center; border-top: 1px solid #E0E0E0; }");
    html.append(".footer p { font-size: 14px; color: #E0E0E0; margin: 5px 0; }");
    html.append(".footer a { color: #FF9900; text-decoration: none; }");
    html.append(".divider { height: 1px; background: #E0E0E0; margin: 20px 0; }");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class='container'>");

    // Header
    html.append("<div class='header'>");
    html.append("<h1>¡Gracias por tu compra! 🎉</h1>");
    html.append(String.format("<p>Orden #%d</p>", purchase.getId()));
    html.append("</div>");

    // Content
    html.append("<div class='content'>");

    // Order Info
    html.append("<div class='order-info'>");
    html.append(String.format("<p><strong>📅 Fecha:</strong> %s</p>", purchase.getCreatedAt().format(DATE_FORMATTER)));
    html.append(String.format("<p><strong>💰 Total Pagado:</strong> $%,.0f COP</p>", purchase.getTotal()));
    html.append("</div>");

    // Products
    html.append("<h2 class='products-title'>📦 Tus Productos Digitales</h2>");

    for (PurchaseItem item : purchase.getItems()) {
        html.append("<div class='product-item'>");
        html.append(String.format("<div class='product-name'>%s</div>", escapeHtml(item.getProduct().getName())));
        html.append(String.format("<div class='product-price'>Precio: $%,.0f COP</div>", item.getPriceAtPurchase()));

        if (item.getDeliveredCode() != null) {
            html.append("<div class='code-section'>");
            html.append("<div class='code-label'>🔑 Tu Código</div>");
            html.append(String.format("<div class='code-value'>%s</div>", escapeHtml(item.getDeliveredCode())));
            html.append("</div>");
        }
        html.append("</div>");
    }

    html.append("<div class='divider'></div>");

    // Important Note
    html.append("<div style='background:#FF9900;padding:15px;border-radius:6px;color:#051A2E;'>");
    html.append("<p style='margin:0;'><strong>⚠️ Importante:</strong> Guarda este email en un lugar seguro. ");
    html.append("Contiene información importante para acceder a tus productos digitales.</p>");
    html.append("</div>");

    html.append("</div>");

    // Footer
    html.append("<div class='footer'>");
    html.append("<p style='margin-top:15px;font-size:12px;'>© 2026 Mercadox. Todos los derechos reservados.</p>");
    html.append("</div>");

    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    return html.toString();
}


    /**
     * Escapa caracteres HTML para prevenir XSS
     */
    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

}
