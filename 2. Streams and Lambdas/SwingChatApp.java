import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.*;

public class SwingChatApp extends JFrame {
    private GenericStorage<ChatMessage> chatRepo;

    private JTextArea chatDisplayArea;
    private JTextField senderField;
    private JTextField messageField;
    private JButton sendButton;
    private JButton analyticsButton;

    public SwingChatApp() {
        chatRepo = new GenericStorage<>();
        seedData();

        // Window Setup
        setTitle("NexCom GUI Client");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header
        JLabel headerLabel = new JLabel("NexCom Enterprise Chat", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Calibri", Font.BOLD, 18));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(headerLabel, BorderLayout.NORTH);

        // Chat History
        chatDisplayArea = new JTextArea();
        chatDisplayArea.setEditable(false);
        chatDisplayArea.setFont(new Font("Calibri", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatDisplayArea);
        add(scrollPane, BorderLayout.CENTER);

        // Inputs & Buttons
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));

        // Input Row
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("User:"));
        senderField = new JTextField("Admin", 8);
        inputPanel.add(senderField);

        inputPanel.add(new JLabel("Message:"));
        messageField = new JTextField(20);
        inputPanel.add(messageField);

        bottomPanel.add(inputPanel);

        // Button Row
        JPanel buttonPanel = new JPanel(new FlowLayout());
        sendButton = new JButton("Send Message");
        analyticsButton = new JButton("Run Stream Analytics");

        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);

        buttonPanel.add(sendButton);
        buttonPanel.add(analyticsButton);
        bottomPanel.add(buttonPanel);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());

        BiFunction<String, Integer, String> summaryCreator = (name, count) -> name + " sent " + count + " msgs";

        analyticsButton.addActionListener(e -> showAnalyticsPopup(summaryCreator));
        messageField.addActionListener(e -> sendMessage());

        refreshDisplay();
    }

    private void sendMessage() {
        String sender = senderField.getText().trim();
        String content = messageField.getText().trim();

        if (sender.isEmpty() || content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Sender and Message cannot be empty!", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        ChatMessage msg = new ChatMessage(sender, content, time);

        chatRepo.add(msg);

        messageField.setText("");
        refreshDisplay();
    }

    private void refreshDisplay() {
        StringBuilder sb = new StringBuilder();
        List<ChatMessage> allMessages = chatRepo.getAll();
        for (ChatMessage msg : allMessages) {
            sb.append(msg.toString()).append("\n");
        }
        chatDisplayArea.setText(sb.toString());
    }

    private void showAnalyticsPopup(BiFunction<String, Integer, String> summaryTool) {
        if (chatRepo.getAll().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data to analyze.");
            return;
        }

        Function<Integer, String> activityLevel = (count) -> {
            if (count > 5)
                return "High Traffic";
            else if (count > 0)
                return "Low Traffic";
            else
                return "No Traffic";
        };

        List<String> longMessages = chatRepo.stream()
                .filter(m -> m.getMessageContent().length() > 5)
                .map(ChatMessage::getMessageContent)
                .collect(Collectors.toList());

        Set<String> uniqueUsers = chatRepo.stream()
                .map(ChatMessage::getSender)
                .collect(Collectors.toSet());

        List<String> sortedBySender = chatRepo.stream()
                .sorted((m1, m2) -> m1.getSender().compareTo(m2.getSender()))
                .map(ChatMessage::toString)
                .collect(Collectors.toList());

        int totalChars = chatRepo.stream()
                .map(m -> m.getMessageContent().length())
                .reduce(0, (subtotal, element) -> subtotal + element);

        Map<String, Long> postsPerUser = chatRepo.stream()
                .collect(Collectors.groupingBy(ChatMessage::getSender, Collectors.counting()));

        // Build Report
        StringBuilder report = new StringBuilder();
        report.append("Analytics Report\n");
        report.append("Status: ").append(activityLevel.apply(chatRepo.getAll().size())).append("\n\n");

        report.append("1. Total Chars: ").append(totalChars).append("\n");
        report.append("2. Unique Users: ").append(uniqueUsers.size()).append("\n");
        report.append("3. Long Msgs (>5 chars): ").append(longMessages.size()).append("\n");
        report.append("4. Posts per User: ").append(postsPerUser).append("\n");

        String topUser = uniqueUsers.iterator().next();
        report.append("5. [Two-Arg Lambda]: ").append(summaryTool.apply(topUser, postsPerUser.get(topUser).intValue()));

        JOptionPane.showMessageDialog(this, report.toString(), "Stream Analytics", JOptionPane.INFORMATION_MESSAGE);
    }

    private void seedData() {
        chatRepo.add(new ChatMessage("System", "Ready", "08:00"));
        chatRepo.add(new ChatMessage("Alice", "Hello World", "08:05"));
        chatRepo.add(new ChatMessage("Bob", "Hi", "08:06"));
        chatRepo.add(new ChatMessage("Alice", "Streams are fun", "08:10"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SwingChatApp().setVisible(true);
        });
    }
}