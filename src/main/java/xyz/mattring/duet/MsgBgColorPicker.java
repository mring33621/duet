package xyz.mattring.duet;

public class MsgBgColorPicker {
    private final String currentUser;

    public MsgBgColorPicker(String currentUser) {
        this.currentUser = currentUser;
    }

    public String pickColor(String msgContent) {
        if (msgContent.startsWith(currentUser)) {
            return "LavenderBlush";
        } else if (msgContent.contains("last logged in at")) {
            return "LightYellow";
        } else {
            return "Lavender"; // other user
        }
    }
}
