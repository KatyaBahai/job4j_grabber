package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {
    private static Connection connection;

    public static void main(String[] args) {
        try {
            List<Long> store = new ArrayList<>();
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("store", store);
            data.put("connection", getConnection("db/liquibase.properties"));
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(getInterval("rabbit.properties")))
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
            System.out.println(store);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection(String path) {
        try (InputStream in = new FileInputStream(path)) {
            Properties property = new Properties();
            property.load(in);
            Class.forName(property.getProperty("driver-class-name"));
            connection = DriverManager.getConnection(
                    property.getProperty("url"),
                    property.getProperty("username"),
                    property.getProperty("password"));



        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static String getInterval(String path) {
        String interval = "";
        try (InputStream in = AlertRabbit.class.getClassLoader()
                .getResourceAsStream(path)) {
            Properties properties = new Properties();
            properties.load(in);
            interval = properties.getProperty("rabbit.interval");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return interval;
    }

    public static class Rabbit implements Job {

        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            List<Long> store = (List<Long>) context.getJobDetail().getJobDataMap().get("store");
            Long date = System.currentTimeMillis();
            store.add(date);
            Connection connection = (Connection) context.getJobDetail().getJobDataMap().get("connection");
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("INSERT INTO rabbit(created_date) VALUES(%s);", date);
                statement.execute(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
