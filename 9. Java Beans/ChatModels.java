
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChatModels {

    /* Represents a user in the chat system */
    public static class User implements Serializable {
        private String username;
        private String status;

        public User() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /* Represents a single message sent between users */
    public static class Message implements Serializable {
        private String sender;
        private String text;
        private long timestamp;

        public Message() {
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /* Represents a group chat or message channel */
    public static class ChatRoom implements Serializable {
        private String roomName;
        private List<User> participants = new ArrayList<>();

        public ChatRoom() {
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public List<User> getParticipants() {
            return participants;
        }

        public void setParticipants(List<User> participants) {
            this.participants = participants;
        }
    }

    /* The entry point to test the JavaBeans */
    public static void main(String[] args) {
        // Create a User bean
        User user1 = new User();
        user1.setUsername("Sridhar");
        user1.setStatus("Online");

        // Create a Message bean
        Message msg = new Message();
        msg.setSender(user1.getUsername());
        msg.setText("Hello, this is a JavaBean test!");
        msg.setTimestamp(System.currentTimeMillis());

        // Create a ChatRoom bean and add the user
        ChatRoom room = new ChatRoom();
        room.setRoomName("General Lobby");
        room.getParticipants().add(user1);

        // Display results
        System.out.println("Room: " + room.getRoomName());
        System.out.println(msg.getSender() + ": " + msg.getText());
        System.out.println("User Status: " + user1.getStatus());
    }
}