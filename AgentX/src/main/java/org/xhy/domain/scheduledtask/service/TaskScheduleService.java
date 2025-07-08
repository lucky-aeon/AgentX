package org.xhy.domain.scheduledtask.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.model.RepeatConfig;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** 任务调度服务 处理定时任务的调度逻辑 */
@Service
public class TaskScheduleService {

    /** 时间匹配容差（分钟） */
    private static final long TIME_TOLERANCE_MINUTES = 1L;

    /** 一周的天数 */
    private static final int DAYS_IN_WEEK = 7;

    /** 时间单位常量 */
    private static final String TIME_UNIT_DAYS = "DAYS";
    private static final String TIME_UNIT_WEEKS = "WEEKS";
    private static final String TIME_UNIT_MONTHS = "MONTHS";

    /** 计算任务的下次执行时间
     * @param task 定时任务
     * @param currentTime 当前时间
     * @return 下次执行时间，如果任务不需要再执行返回null */
    public LocalDateTime calculateNextExecuteTime(ScheduledTaskEntity task, LocalDateTime currentTime) {
        RepeatConfig config = task.getRepeatConfig();
        if (config == null) {
            return null;
        }

        RepeatType repeatType = task.getRepeatType();

        switch (repeatType) {
            case NONE :
                // 一次性任务，如果还没有执行过且执行时间在未来，返回执行时间
                LocalDateTime executeTime = config.getExecuteDateTime();
                if (executeTime != null && task.getLastExecuteTime() == null && executeTime.isAfter(currentTime)) {
                    return executeTime;
                }
                return null;

            case DAILY :
                return calculateDailyNextTime(config, currentTime);

            case WEEKLY :
                return calculateWeeklyNextTime(config, currentTime);

            case MONTHLY :
                return calculateMonthlyNextTime(config, currentTime);

            case WORKDAYS :
                return calculateWorkdaysNextTime(config, currentTime);

            case CUSTOM :
                return calculateCustomNextTime(config, currentTime);

            default :
                return null;
        }
    }

    /** 检查任务是否应该在指定时间执行
     * @param task 定时任务
     * @param checkTime 检查时间
     * @return 是否应该执行 */
    public boolean shouldExecuteAt(ScheduledTaskEntity task, LocalDateTime checkTime) {
        RepeatConfig config = task.getRepeatConfig();
        if (config == null || config.getExecuteDateTime() == null) {
            return false;
        }

        // 检查是否已经到了执行时间
        LocalDateTime executeTime = config.getExecuteDateTime();
        if (checkTime.isBefore(executeTime)) {
            return false;
        }

        // 检查上次执行时间，避免重复执行
        LocalDateTime lastExecuteTime = task.getLastExecuteTime();
        if (lastExecuteTime != null && !checkTime.isAfter(lastExecuteTime.plusMinutes(TIME_TOLERANCE_MINUTES))) {
            return false;
        }

        RepeatType repeatType = task.getRepeatType();

        switch (repeatType) {
            case NONE :
                // 一次性任务，只在首次执行时间执行
                return lastExecuteTime == null && checkTime.isAfter(executeTime.minusMinutes(TIME_TOLERANCE_MINUTES))
                        && checkTime.isBefore(executeTime.plusMinutes(TIME_TOLERANCE_MINUTES));

            case DAILY :
                return shouldExecuteDaily(config, checkTime);

            case WEEKLY :
                return shouldExecuteWeekly(config, checkTime);

            case MONTHLY :
                return shouldExecuteMonthly(config, checkTime);

            case WORKDAYS :
                return shouldExecuteWorkdays(config, checkTime);

            case CUSTOM :
                return shouldExecuteCustom(config, checkTime);

            default :
                return false;
        }
    }

    private LocalDateTime calculateDailyNextTime(RepeatConfig config, LocalDateTime currentTime) {
        LocalDateTime executeTime = config.getExecuteDateTime();
        LocalTime time = executeTime.toLocalTime();

        LocalDateTime nextTime = currentTime.toLocalDate().atTime(time);
        if (nextTime.isBefore(currentTime) || nextTime.equals(currentTime)) {
            nextTime = nextTime.plusDays(1);
        }

        return nextTime;
    }

    private LocalDateTime calculateWeeklyNextTime(RepeatConfig config, LocalDateTime currentTime) {
        List<Integer> weekdays = config.getWeekdays();
        if (weekdays == null || weekdays.isEmpty()) {
            return null;
        }

        LocalDateTime executeTime = config.getExecuteDateTime();
        LocalTime time = executeTime.toLocalTime();

        // 找到下一个符合条件的星期几
        LocalDateTime nextTime = currentTime.toLocalDate().atTime(time);
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            LocalDateTime candidate = nextTime.plusDays(i);
            int dayOfWeek = candidate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

            if (weekdays.contains(dayOfWeek) && candidate.isAfter(currentTime)) {
                return candidate;
            }
        }

        return null;
    }

    private LocalDateTime calculateMonthlyNextTime(RepeatConfig config, LocalDateTime currentTime) {
        Integer monthDay = config.getMonthDay();
        if (monthDay == null) {
            return null;
        }

        LocalDateTime executeTime = config.getExecuteDateTime();
        LocalTime time = executeTime.toLocalTime();

        LocalDateTime nextTime = currentTime.toLocalDate().withDayOfMonth(monthDay).atTime(time);
        if (nextTime.isBefore(currentTime) || nextTime.equals(currentTime)) {
            nextTime = nextTime.plusMonths(1);
        }

        return nextTime;
    }

    private LocalDateTime calculateWorkdaysNextTime(RepeatConfig config, LocalDateTime currentTime) {
        LocalDateTime executeTime = config.getExecuteDateTime();
        LocalTime time = executeTime.toLocalTime();

        LocalDateTime nextTime = currentTime.toLocalDate().atTime(time);

        // 找到下一个工作日
        while (true) {
            if (nextTime.isAfter(currentTime) && isWorkday(nextTime)) {
                return nextTime;
            }
            nextTime = nextTime.plusDays(1);
        }
    }

    private LocalDateTime calculateCustomNextTime(RepeatConfig config, LocalDateTime currentTime) {
        Integer interval = config.getInterval();
        String timeUnit = config.getTimeUnit();
        LocalDateTime endDateTime = config.getEndDateTime();

        if (interval == null || timeUnit == null) {
            return null;
        }

        // 检查是否已经超过截止时间
        if (endDateTime != null && currentTime.isAfter(endDateTime)) {
            return null;
        }

        LocalDateTime executeTime = config.getExecuteDateTime();
        LocalDateTime nextTime = executeTime;

        // 计算下一次执行时间
        while (nextTime.isBefore(currentTime) || nextTime.equals(currentTime)) {
            switch (timeUnit.toUpperCase()) {
                case TIME_UNIT_DAYS :
                    nextTime = nextTime.plusDays(interval);
                    break;
                case TIME_UNIT_WEEKS :
                    nextTime = nextTime.plusWeeks(interval);
                    break;
                case TIME_UNIT_MONTHS :
                    nextTime = nextTime.plusMonths(interval);
                    break;
                default :
                    return null;
            }
        }

        // 检查是否超过截止时间
        if (endDateTime != null && nextTime.isAfter(endDateTime)) {
            return null;
        }

        return nextTime;
    }

    private boolean shouldExecuteDaily(RepeatConfig config, LocalDateTime checkTime) {
        LocalTime executeTime = config.getExecuteDateTime().toLocalTime();
        LocalTime checkTimeOnly = checkTime.toLocalTime();

        return Math.abs(ChronoUnit.MINUTES.between(executeTime, checkTimeOnly)) <= TIME_TOLERANCE_MINUTES;
    }

    private boolean shouldExecuteWeekly(RepeatConfig config, LocalDateTime checkTime) {
        List<Integer> weekdays = config.getWeekdays();
        if (weekdays == null || weekdays.isEmpty()) {
            return false;
        }

        int dayOfWeek = checkTime.getDayOfWeek().getValue();
        if (!weekdays.contains(dayOfWeek)) {
            return false;
        }

        return shouldExecuteDaily(config, checkTime);
    }

    private boolean shouldExecuteMonthly(RepeatConfig config, LocalDateTime checkTime) {
        Integer monthDay = config.getMonthDay();
        if (monthDay == null) {
            return false;
        }

        if (checkTime.getDayOfMonth() != monthDay) {
            return false;
        }

        return shouldExecuteDaily(config, checkTime);
    }

    private boolean shouldExecuteWorkdays(RepeatConfig config, LocalDateTime checkTime) {
        if (!isWorkday(checkTime)) {
            return false;
        }

        return shouldExecuteDaily(config, checkTime);
    }

    private boolean shouldExecuteCustom(RepeatConfig config, LocalDateTime checkTime) {
        Integer interval = config.getInterval();
        String timeUnit = config.getTimeUnit();
        LocalDateTime endDateTime = config.getEndDateTime();

        if (interval == null || timeUnit == null) {
            return false;
        }

        // 检查是否已经超过截止时间
        if (endDateTime != null && checkTime.isAfter(endDateTime)) {
            return false;
        }

        LocalDateTime executeTime = config.getExecuteDateTime();

        // 计算从开始时间到检查时间的间隔
        long totalUnits;
        switch (timeUnit.toUpperCase()) {
            case TIME_UNIT_DAYS :
                totalUnits = ChronoUnit.DAYS.between(executeTime.toLocalDate(), checkTime.toLocalDate());
                break;
            case TIME_UNIT_WEEKS :
                totalUnits = ChronoUnit.WEEKS.between(executeTime.toLocalDate(), checkTime.toLocalDate());
                break;
            case TIME_UNIT_MONTHS :
                totalUnits = ChronoUnit.MONTHS.between(executeTime.toLocalDate(), checkTime.toLocalDate());
                break;
            default :
                return false;
        }

        // 检查是否是执行周期的倍数
        if (totalUnits % interval != 0) {
            return false;
        }

        return shouldExecuteDaily(config, checkTime);
    }

    private boolean isWorkday(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}