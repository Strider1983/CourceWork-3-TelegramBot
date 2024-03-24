package pro.sky.telegrambot.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import pro.sky.telegrambot.service.TelegramBotSender;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationSenderJob {
    private Logger logger = LoggerFactory.getLogger(NotificationSenderJob.class);
    private final NotificationTaskRepository notificationTaskRepository;
    private final TelegramBotSender telegramBotSender;

    public NotificationSenderJob(NotificationTaskRepository notificationTaskRepository, TelegramBotSender telegramBotSender) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBotSender = telegramBotSender;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void sendNotifications() {
        LocalDateTime currentDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        logger.info("Notification job started fore date time {}", currentDateTime);
        List<NotificationTask> allByNotificationDateTime = notificationTaskRepository.findAllByNotificationDateTime(currentDateTime);
        logger.info("Notification job has found {} relevant notifications", allByNotificationDateTime.size());
        for (NotificationTask notificationTask : allByNotificationDateTime) {
            logger.info("Processing notification task {}", notificationTask);
            telegramBotSender.send(
                    notificationTask.getChatId(),
                    "Напоминание!" + notificationTask.getMessage()
            );
        }
        logger.info("Notification job finished fore date time {}", currentDateTime);


    };
    public List<NotificationTask> getAllTasks() {
        return notificationTaskRepository.findAll();

    }

}
