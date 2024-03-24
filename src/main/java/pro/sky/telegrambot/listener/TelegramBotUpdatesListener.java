package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import pro.sky.telegrambot.service.TelegramBotSender;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final Pattern INCOMING_MESSAGE_PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
    private final DateTimeFormatter NOTIFICATION_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository, TelegramBot telegramBot, TelegramBotSender telegramBotSender) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
        this.telegramBotSender = telegramBotSender;
    }

    private final NotificationTaskRepository notificationTaskRepository;


    private final TelegramBot telegramBot;

    private final TelegramBotSender telegramBotSender;

    private final String WELCOME_MESSAGE = "Greetings Stranger";
    private final String SUCCESSFULLY_SAVED_RESPONSE = "Ваше напоминание успешно сохранено";


    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            String message = update.message().text();
            Long chatId = update.message().chat().id();
            if (message.equals("/start")) {
                logger.info("message received: " + message);
                telegramBotSender.send(chatId, WELCOME_MESSAGE);
            } else {
                Matcher matcher = INCOMING_MESSAGE_PATTERN.matcher(message);
                if (matcher.matches()) {
                    logger.info("new message accepted: "  +  message);

                    String rawDateTime = matcher.group(1);
                    String notificationText = matcher.group(3);

                    NotificationTask notificationTask = new NotificationTask(
                            chatId,
                            notificationText,
                            LocalDateTime.parse(rawDateTime, NOTIFICATION_DATE_TIME_FORMAT)
                    );
                    notificationTaskRepository.save(notificationTask);

                    telegramBotSender.send(chatId, SUCCESSFULLY_SAVED_RESPONSE);


                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
