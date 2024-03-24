package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.job.NotificationSenderJob;
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

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository, NotificationSenderJob notificationSenderJob, TelegramBot telegramBot, TelegramBotSender telegramBotSender) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.notificationSenderJob = notificationSenderJob;
        this.telegramBot = telegramBot;
        this.telegramBotSender = telegramBotSender;
    }

    private final NotificationTaskRepository notificationTaskRepository;
    private final NotificationSenderJob notificationSenderJob;


    private final TelegramBot telegramBot;

    private final TelegramBotSender telegramBotSender;

    private final String WELCOME_MESSAGE = "Greetings Stranger";
    private final String CORRECT_MESSAGE_FORMAT = "дд.мм.гггг чч:мм Текст напоминания";
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
            } else if (message.equals("/clearAll")) {
                notificationTaskRepository.deleteAll();
                telegramBotSender.send(chatId, "Все записи удалены");

            } else if (message.equals("/showAll")) {
                List<NotificationTask> tasks = notificationSenderJob.getAllTasks();
                telegramBotSender.send(chatId, tasks.toString());
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

                } else {
                    logger.info("recieved message with incorrect format: "  +  message);
                    telegramBotSender.send(chatId, "Напопинание должно соотвествовать формату: " + CORRECT_MESSAGE_FORMAT);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
