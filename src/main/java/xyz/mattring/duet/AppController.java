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

import java.util.List;
import java.util.Optional;

@Controller
public class AppController {

    private static final String USERNAME = "username";
    private static final String TOKEN = "token";
    private static final long NINE_MINS_MILLIS = 1000 * 60 * 9;
    private final UsrMgt usrMgt;
    private final ChatFile chatFile;

    public AppController() {
        usrMgt = new UsrMgt("Duet", null, NINE_MINS_MILLIS);
        chatFile = new ChatFile();
    }

    private List<String> getChatMsgs() {
        return chatFile.getTodaysMessages().reversed(); // newest first
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
            List<String> chatMsgs = getChatMsgs();
            model.addAttribute("chatMsgs", chatMsgs);
            return "chat";
        } else {
            session.invalidate();
            return "login";
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
}
