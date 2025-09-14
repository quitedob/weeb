package com.web.schedule;


//import com.web.service.MessageService;
//import com.web.service.UserService;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.time.LocalDate;
//
//@Component
//public class ExpiredClearTask {
//
//    @Resource
//    MessageService messageService;
//
//    @Resource
//    UserService userService;
//
//    @Value("${linyu.expires}")
//    int expirationDays;
//
//
//    @Scheduled(cron = "0 0 0 * * ?")
//    public void deleteExpiredContent() {
//        LocalDate expirationDate = LocalDate.now().minusDays(expirationDays);
//        messageService.deleteExpiredMessages(expirationDate);
//        userService.deleteExpiredUsers(expirationDate);
//    }
//}
//
//
