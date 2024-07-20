package xyz.mattring.duet;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ChatFile {
    private static final String PATH = System.getProperty("user.home") + "/duet/ChatFile_%d.txt";
    static final String MSG_TEMPLATE_WITH_HEADER = "%s at %s:\n%s\n";

    static String todayYYYYMMDD() {
        return LocalDate.now().toString().replace("-", "");
    }

    static int todayAsInt() {
        return Integer.parseInt(todayYYYYMMDD());
    }

    static String now() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return now.format(formatter);
    }

    static Optional<String> parseTimestampFromMessage(String message) {
        String timestamp = null;
        if (message != null && message.contains(":")) {
            String frontPartWithTimestamp = message.substring(0, message.indexOf(":"));
            if (frontPartWithTimestamp.contains(" at ")) {
                timestamp = frontPartWithTimestamp.split(" at ")[1];
            }
        }
        return Optional.ofNullable(timestamp);
    }

    static Long timestampToLong(String timestamp) {
        return Long.parseLong(timestamp.replace("-", ""));
    }

    static TimestampedMsg msgStringToTimestampedMsg(String message) {
        Optional<String> maybeTimestamp = parseTimestampFromMessage(message);
        return maybeTimestamp.map(s -> new TimestampedMsg(message, timestampToLong(s)))
                .orElseGet(() -> new TimestampedMsg(message, timestampToLong(now())));
    }

    static List<String> readMessagesFromFile(File file) {
        if (!file.exists()) {
            return Collections.emptyList();
        }
        List<String> sections = new LinkedList<>();
        StringBuilder currentSection = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals("_NEW_MESSAGE_")) {
                    if (!currentSection.isEmpty()) {
                        sections.add(currentSection.toString().trim());
                        currentSection = new StringBuilder();
                    }
                } else {
                    currentSection.append(line).append("\n");
                }
            }
            if (!currentSection.isEmpty()) {
                sections.add(currentSection.toString().trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sections;
    }

    static void writeMessagesToFile(List<TimestampedMsg> sections, File targetFile) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(targetFile))) {
            for (int i = 0; i < sections.size(); i++) {
                bw.write(sections.get(i).msg());
                bw.newLine();
                if (i < sections.size() - 1) {
                    bw.write("_NEW_MESSAGE_");
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int currentDay;
    List<TimestampedMsg> todaysMessages;
    final ExecutorService exec;
    final ScheduledExecutorService sched;

    public ChatFile() {
        final File chatFileDir = new File(PATH).getParentFile();
        chatFileDir.mkdirs();
        System.out.println("chatFileDir: " + chatFileDir.getAbsolutePath());
        currentDay = todayAsInt();
        todaysMessages = loadTodaysMessages();
        exec = Executors.newSingleThreadExecutor();
        sched = Executors.newSingleThreadScheduledExecutor();
        // add scheduled task for every 10 minutes
        sched.scheduleWithFixedDelay(
                this::checkFileDay, 15, 15, TimeUnit.MINUTES);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            exec.shutdownNow();
            sched.shutdownNow();
            writeMessagesToFile(todaysMessages, getMessagesFile());
        }));
    }

    void checkFileDay() {
        try {
            exec.submit(this::closeOldAndOpenNewChatFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void closeOldAndOpenNewChatFile() {
        try {
            writeMessagesToFile(todaysMessages, getMessagesFile());
            currentDay = todayAsInt();
            todaysMessages = loadTodaysMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<TimestampedMsg> loadTodaysMessages() {
        return readMessagesFromFile(
                getMessagesFile()).stream().map(ChatFile::msgStringToTimestampedMsg)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    boolean isNewDay() {
        return currentDay < todayAsInt();
    }

    File getMessagesFile() {
        return new File(PATH.formatted(currentDay));
    }

    public List<TimestampedMsg> getTodaysMessages() {
        return new ArrayList<>(todaysMessages);
    }

    public void addMessage(final String user, final String message) {
        exec.submit(() -> {
            try {
                String nowStamp = now();
                String fullMsg = String.format(MSG_TEMPLATE_WITH_HEADER, user, nowStamp, message);
                todaysMessages.add(new TimestampedMsg(fullMsg, timestampToLong(nowStamp)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
