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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class AppController {

    private final UsrMgt usrMgt;
    private final ChatFile chatFile;
    private final List<String> msgs;

    public AppController() {
        usrMgt = new UsrMgt("Duet", null);
        chatFile = new ChatFile();
        msgs = new ArrayList<>(Arrays.asList("Msg1\n", "Msg2\n", "Msg3\n"));
    }

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpServletRequest request,
                        HttpServletResponse response) {

        Optional<String> maybeToken = usrMgt.tryLogin(username + '|' + password);
        if (maybeToken.isPresent()) {
            return "redirect:/chat"; // Redirect to the /chat endpoint
        } else {
            HttpSession session = request.getSession();
            session.setAttribute("error", "Invalid username or password");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // return HTTP 401 error
            return "error"; // return error page
        }
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        List<String> chatMsgs = getChatMsgs();
        model.addAttribute("chatMsgs", chatMsgs);
        return "chat";
    }

    private List<String> getChatMsgs() {
        return msgs;
    }

    @PostMapping("/newMsg")
    public String newMessage(@RequestParam("message") String message) {
        msgs.add(message);
        return "redirect:/chat"; // Redirect back to the chat page
    }
}
