package xyz.mattring.duet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import xyz.mattring.usrmgt.UsrMgt;

import java.util.*;

@Controller
public class AppController {

    private static final String USERNAME = "username";
    private static final String TOKEN = "token";
    private static final long NINE_MINS_MILLIS = 1000 * 60 * 9;
    private final UsrMgt usrMgt;
    private final ChatFile chatFile;
    private final Map<String, String> usersToLoginTimestampsMap;

    public AppController() {
        usrMgt = new UsrMgt("Duet", null, NINE_MINS_MILLIS);
        chatFile = new ChatFile();
        usersToLoginTimestampsMap = new HashMap<>();
    }

    private List<TimestampedMsg> getChatMsgs() {
        return chatFile.getTodaysMessages();
    }

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(USERNAME) String username,
                        @RequestParam("password") String password,
                        HttpServletRequest request,
                        HttpServletResponse response) {

        Optional<String> maybeToken = usrMgt.tryLogin(username + '|' + password);
        if (maybeToken.isPresent()) {
            HttpSession session = request.getSession();
            session.setAttribute(USERNAME, username);
            session.setAttribute(TOKEN, maybeToken.get());
            usersToLoginTimestampsMap.put(username, ChatFile.now());
            return "redirect:/chat"; // Redirect to the /chat endpoint
        } else {
            HttpSession session = request.getSession();
            session.setAttribute("error", "Invalid username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // return HTTP 401 error
            return "error"; // return error page
        }
    }

    @GetMapping("/chat")
    public String chat(Model model, HttpServletRequest request) {
        final HttpSession session = request.getSession();
        final String token = (String) session.getAttribute(TOKEN);
        if (usrMgt.validateToken(token, true)) {
            List<TimestampedMsg> chatMsgs = getChatMsgs();
            final String currentUser = (String) session.getAttribute(USERNAME);
            chatMsgs = addLastLoginInfoToMsgs(currentUser, chatMsgs); // add last login info
            chatMsgs.sort(Comparator.comparingLong(TimestampedMsg::timestamp).reversed()); // sort reverse for newest first
            model.addAttribute("chatMsgs", chatMsgs);
            model.addAttribute("bgColorPicker", new MsgBgColorPicker(currentUser));
            return "chat";
        } else {
            session.invalidate();
            return "login";
        }
    }

    List<TimestampedMsg> addLastLoginInfoToMsgs(final String currentUser, final List<TimestampedMsg> chatMsgs) {
        final List<TimestampedMsg> lastLoginInfoMsgs = new LinkedList<>();
        for (Map.Entry<String, String> userStampEntry : usersToLoginTimestampsMap.entrySet()) {
            if (!currentUser.equals(userStampEntry.getKey())) {
                final String timestamp = userStampEntry.getValue();
                final String msg = userStampEntry.getKey() + " last logged in at " + userStampEntry.getValue();
                lastLoginInfoMsgs.add(new TimestampedMsg(msg, ChatFile.timestampToLong(timestamp)));
            }
        }
        if (lastLoginInfoMsgs.isEmpty()) {
            return chatMsgs;
        } else {
            lastLoginInfoMsgs.addAll(chatMsgs);
            return lastLoginInfoMsgs;
        }
    }

    @PostMapping("/newMsg")
    public String newMessage(@RequestParam("message") String message, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(TOKEN);
        if (usrMgt.validateToken(token, true)) {
            String username = (String) session.getAttribute(USERNAME);
            chatFile.addMessage(username, message);
            return "redirect:/chat"; // Redirect back to the chat page
        } else {
            session.invalidate();
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.invalidate();
        return "login";
    }

    @GetMapping("/refresh")
    public String refresh() {
        return "redirect:/chat"; // Redirect back to the chat page
    }
}
