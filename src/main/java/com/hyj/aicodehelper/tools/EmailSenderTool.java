package com.hyj.aicodehelper.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 邮件发送工具类
 * 支持普通文本邮件和HTML格式邮件
 */
@Slf4j
@Component
public class EmailSenderTool {


    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String defaultFrom;
    
    @Value("${email.default-receiver}")
    private String defaultReceiver;



    public EmailSenderTool(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    /**
     * 发送普通文本邮件
     *
     * @param to 收件人邮箱地址（多个用逗号分隔）
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果信息
     */
    @Tool(name = "sendPlainEmail", value = """
            Sends a plain text email to the specified recipients.
            Use this tool when the user wants to send a simple text email.
            The method requires three parameters:
            1. to: recipient email addresses (separate multiple addresses with commas)
            2. subject: email subject line
            3. content: the text content of the email
            If no recipient is specified, uses the default receiver from configuration.
            Returns a confirmation message if successful, or an error message if failed.
            """)
    public String sendPlainEmail(
            @P(value = "recipient email addresses (comma-separated for multiple)") String to,
            @P(value = "email subject") String subject,
            @P(value = "email content") String content) {
        
        try {
            // 如果收件人为空，使用默认收件人
            String recipient = to != null && !to.trim().isEmpty() ? to : defaultReceiver;
            
            if (recipient == null || recipient.trim().isEmpty()) {
                return "Error: No recipient specified and no default receiver configured";
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(defaultFrom);
            message.setTo(recipient.split(",")); // 支持多个收件人
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            
            String result = String.format("✅ Email sent successfully!\n" +
                    "📨 To: %s\n" +
                    "📝 Subject: %s\n" +
                    "📊 Status: Delivered", recipient, subject);
            
            log.info("Plain email sent to: {}, subject: {}", recipient, subject);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to send plain email", e);
            return String.format("❌ Failed to send email: %s", e.getMessage());
        }
    }
    
    /**
     * 发送HTML格式邮件
     *
     * @param to 收件人邮箱地址（多个用逗号分隔）
     * @param subject 邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 发送结果信息
     */
    @Tool(name = "sendHtmlEmail", value = """
            Sends an HTML formatted email to the specified recipients.
            Use this tool when the user wants to send rich content emails with HTML formatting.
            The method requires three parameters:
            1. to: recipient email addresses (separate multiple addresses with commas)
            2. subject: email subject line
            3. htmlContent: the HTML content of the email
            If no recipient is specified, uses the default receiver from configuration.
            Returns a confirmation message if successful, or an error message if failed.
            """)
    public String sendHtmlEmail(
            @P(value = "recipient email addresses (comma-separated for multiple)") String to,
            @P(value = "email subject") String subject,
            @P(value = "HTML email content") String htmlContent) {
        
        try {
            // 如果收件人为空，使用默认收件人
            String recipient = to != null && !to.trim().isEmpty() ? to : defaultReceiver;
            
            if (recipient == null || recipient.trim().isEmpty()) {
                return "Error: No recipient specified and no default receiver configured";
            }
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(defaultFrom);
            helper.setTo(recipient.split(",")); // 支持多个收件人
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true表示内容为HTML
            
            mailSender.send(message);
            
            String result = String.format("✅ HTML email sent successfully!\n" +
                    "📨 To: %s\n" +
                    "📝 Subject: %s\n" +
                    "🎨 Format: HTML\n" +
                    "📊 Status: Delivered", recipient, subject);
            
            log.info("HTML email sent to: {}, subject: {}", recipient, subject);
            return result;
            
        } catch (MessagingException e) {
            log.error("Failed to send HTML email", e);
            return String.format("❌ Failed to send HTML email: %s", e.getMessage());
        }
    }
    
    /**
     * 发送带有附件的邮件（简易版）
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果信息（注：当前版本仅提示，实际需要文件上传支持）
     */
    @Tool(name = "sendEmailWithAttachment", value = """
            Sends an email with attachments. 
            NOTE: This is a simplified version that returns instructions for the full implementation.
            For actual file attachments, additional file handling would be required.
            Use this when the user mentions sending files or attachments via email.
            """)
    public String sendEmailWithAttachment(
            @P(value = "recipient email address") String to,
            @P(value = "email subject") String subject,
            @P(value = "email content") String content) {
        
        String recipient = to != null && !to.trim().isEmpty() ? to : defaultReceiver;
        
        if (recipient == null || recipient.trim().isEmpty()) {
            return "Error: No recipient specified and no default receiver configured";
        }
        
        // 这里返回提示信息，实际实现需要文件上传和处理
        return String.format("""
                📎 Email with attachment setup complete (demonstration mode):
                
                Recipient: %s
                Subject: %s
                Content: %s
                
                Note: To implement actual file attachments:
                1. Add file upload endpoint to your application
                2. Save uploaded files temporarily
                3. Use MimeMessageHelper.addAttachment() method
                4. Attach files from server storage
                
                For now, use sendPlainEmail or sendHtmlEmail for sending emails without attachments.
                """, recipient, subject, content);
    }
    
    /**
     * 异步发送邮件（后台发送，立即返回）
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 任务提交确认信息
     */
    @Tool(name = "sendEmailAsync", value = """
            Sends an email asynchronously in the background.
            Use this tool when the user wants to send an email without waiting for the send operation to complete.
            This method returns immediately with a confirmation that the email task has been queued.
            Recommended for non-critical emails where immediate feedback isn't required.
            """)
    public String sendEmailAsync(
            @P(value = "recipient email address") String to,
            @P(value = "email subject") String subject,
            @P(value = "email content") String content) {
        
        try {
            String recipient = to != null && !to.trim().isEmpty() ? to : defaultReceiver;
            
            if (recipient == null || recipient.trim().isEmpty()) {
                return "Error: No recipient specified and no default receiver configured";
            }
            
            // 异步发送邮件
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(defaultFrom);
                    message.setTo(recipient.split(","));
                    message.setSubject(subject);
                    message.setText(content);
                    
                    mailSender.send(message);
                    log.info("Async email sent to: {}, subject: {}", recipient, subject);
                } catch (Exception e) {
                    log.error("Failed to send async email", e);
                }
            });
            
            return String.format("""
                    ⏳ Email queued for background sending!
                    
                    📧 Details:
                    • To: %s
                    • Subject: %s
                    • Status: Processing in background
                    
                    You'll receive no further confirmation, but the email will be sent shortly.
                    """, recipient, subject);
            
        } catch (Exception e) {
            log.error("Failed to queue async email", e);
            return String.format("❌ Failed to queue email for sending: %s", e.getMessage());
        }
    }
    
    /**
     * 发送邮件到默认收件人（简化版）
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果信息
     */
    @Tool(name = "sendEmailToDefault", value = """
            Sends an email to the default recipient configured in the system.
            Use this tool when the user doesn't specify a recipient but wants to send an email.
            This is useful for notifications, alerts, or system-generated emails.
            Returns a confirmation message if successful, or an error message if failed.
            """)
    public String sendEmailToDefault(
            @P(value = "email subject") String subject,
            @P(value = "email content") String content) {
        
        if (defaultReceiver == null || defaultReceiver.trim().isEmpty()) {
            return """
                   ❌ Error: No default recipient configured.
                   
                   Please configure 'email.default-receiver' in application.yml
                   Or use sendPlainEmail/sendHtmlEmail with explicit recipient address.
                   """;
        }
        
        return sendPlainEmail(defaultReceiver, subject, content);
    }
}