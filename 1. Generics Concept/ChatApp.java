import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

interface IDataRepository<T> {
    void add(T item);

    T get(int index);

    int search(T item);

    List<T> getAll();
}

class GenericStorage<T> implements IDataRepository<T> {
    private ArrayList<T> storageList;

    public GenericStorage() {
        this.storageList = new ArrayList<>();
    }

    @Override
    public void add(T item) {
        storageList.add(item);
        System.out.println("Success: Item added to storage.");
    }

    @Override
    public T get(int index) {
        if (index >= 0 && index < storageList.size()) {
            return storageList.get(index);
        }
        return null;
    }

    @Override
    public int search(T item) {
        return storageList.indexOf(item);
    }

    public List<Integer> searchByMessageContent(String content) {
        List<Integer> foundIndices = new ArrayList<>();

        String searchLower = content.toLowerCase();

        for (int i = 0; i < storageList.size(); i++) {
            T item = storageList.get(i);

            if (item instanceof ChatMessage) {
                ChatMessage message = (ChatMessage) item;

                String messageContentLower = message.getMessageContent().toLowerCase();
                if (messageContentLower.contains(searchLower)) {
                    foundIndices.add(i);
                }
            }
        }
        return foundIndices;
    }

    @Override
    public List<T> getAll() {
        return storageList;
    }
}

class DisplayUtils {
    public static <E> void displayList(List<E> list, String listName) {
        System.out.println("\n Displaying: " + listName + " (Total: " + list.size() + ")");
        if (list.isEmpty()) {
            System.out.println(" (Empty List) ");
        } else {
            for (int i = 0; i < list.size(); i++) {
                System.out.println("[" + i + "] " + list.get(i));
            }
        }
        System.out.println("-----------------------------------");
    }
}

class ChatMessage {
    private String sender;
    private String messageContent;
    private String timestamp;

    public ChatMessage(String sender, String messageContent, String timestamp) {
        this.sender = sender;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ChatMessage other = (ChatMessage) obj;
        return sender.equals(other.sender) && messageContent.equals(other.messageContent);
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + messageContent;
    }

    public String getMessageContent() {
        return messageContent;
    }
}

public class ChatApp {
    private static void handleSearch(GenericStorage<ChatMessage> chatRepo, Scanner scanner) {
        System.out.println("\nSearch Options");
        System.out.println("1. Search by Sender and Content");
        System.out.println("2. Search by Content");
        System.out.print("Enter search choice: ");

        String searchChoiceStr = scanner.nextLine();
        int searchChoice = -1;
        try {
            searchChoice = Integer.parseInt(searchChoiceStr);
        } catch (NumberFormatException e) {
            System.out.println("\n*** Invalid input. Returning to main menu. ***");
            return;
        }

        switch (searchChoice) {
            case 1:
                System.out.print("Enter Sender Name for Search: ");
                String searchSender = scanner.nextLine();
                System.out.print("Enter Message Content for Search: ");
                String searchContent = scanner.nextLine();

                ChatMessage searchTarget = new ChatMessage(searchSender, searchContent, "ignored");
                int foundIndex = chatRepo.search(searchTarget);

                if (foundIndex != -1) {
                    System.out.println(">> Result (Sender + Content): Found at index " + foundIndex + ". Details: "
                            + chatRepo.get(foundIndex));
                } else {
                    System.out.println(">> Result (Sender + Content): Message not found.");
                }
                break;

            case 2:
                System.out.print("Enter Message Content for Search (Case-insensitive Substring): ");
                String contentOnly = scanner.nextLine();

                List<Integer> foundIndices = chatRepo.searchByMessageContent(contentOnly);

                if (!foundIndices.isEmpty()) {
                    System.out.println(
                            "\n>>> Found " + foundIndices.size() + " matches for content: '" + contentOnly + "'");
                    System.out.println("-----------------------------------");
                    for (int index : foundIndices) {
                        System.out.println("[INDEX: " + index + "] " + chatRepo.get(index));
                    }
                    System.out.println("-----------------------------------");
                } else {
                    System.out.println(">> Result: No messages found matching that content.");
                }
                break;

            default:
                System.out.println("\n*** Invalid search choice. ***");
        }
    }

    public static void main(String[] args) {
        GenericStorage<ChatMessage> chatRepo = new GenericStorage<>();
        Scanner scanner = new Scanner(System.in);
        int choice = -1;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        chatRepo.add(new ChatMessage("Admin", "Welcome to NexCom!", "09:00 AM"));
        chatRepo.add(new ChatMessage("User1", "Hello everyone!", "09:05 AM"));
        chatRepo.add(new ChatMessage("User2", "Is the server up?", "09:10 AM"));
        chatRepo.add(new ChatMessage("System", "SERVER UP. Logging completed.", "09:15 AM"));
        chatRepo.add(new ChatMessage("Bot", "Hello everyone!", "09:20 AM"));
        chatRepo.add(new ChatMessage("User3", "Is the server up?", "09:25 AM"));

        System.out.println("\nGeneric Chat Repository Application");

        while (choice != 0) {
            System.out.println("\n\n\n\n\n\n-----------------------------------");
            System.out.println("Select an operation:");
            System.out.println("1. Add New Chat Message");
            System.out.println("2. Retrieve Message by Index");
            System.out.println("3. Search for a Message");
            System.out.println("4. Display All Messages");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            try {
                String input = scanner.nextLine();
                if (input.isEmpty())
                    continue;
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("\n*** Invalid input. Please enter a number. ***");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.print("Enter Sender Name: ");
                    String sender = scanner.nextLine();
                    System.out.print("Enter Message Content: ");
                    String content = scanner.nextLine();

                    String timestamp = LocalTime.now().format(timeFormatter);

                    ChatMessage newMsg = new ChatMessage(sender, content, timestamp);
                    chatRepo.add(newMsg);
                    break;

                case 2:
                    DisplayUtils.displayList(chatRepo.getAll(), "Current Chat History");
                    System.out.print("Enter Index to Retrieve: ");
                    try {
                        int index = Integer.parseInt(scanner.nextLine());
                        ChatMessage retrievedMsg = chatRepo.get(index);
                        if (retrievedMsg != null) {
                            System.out.println(">> Retrieved: " + retrievedMsg);
                        } else {
                            System.out.println("*** Error: Index is out of bounds or list is empty. ***");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("\n*** Invalid index format. ***");
                    }
                    break;

                case 3:
                    handleSearch(chatRepo, scanner);
                    break;

                case 4:
                    DisplayUtils.displayList(chatRepo.getAll(), "Full Chat History");
                    break;

                case 5:
                    System.out.println("\nAre you sure you want to exit? (yes/no): ");
                    String confirmExit = scanner.nextLine().trim().toLowerCase();
                    if (confirmExit.equals("yes") || confirmExit.equals("y")) {
                        System.out.println("Exiting application. Goodbye!");
                        return;
                    } else {
                        choice = -1;
                    }
                    break;

                default:
                    System.out.println("\n*** Invalid choice. Please select an option from the menu. ***");
            }
        }

        scanner.close();
    }
}