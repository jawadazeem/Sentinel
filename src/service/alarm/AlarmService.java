package service.alarm;

import alarm.Alarm;
import infrastructure.logger.Logger;
import infrastructure.repository.AlarmLogRepository;

import java.util.List;

public class AlarmService {
    AlarmLogRepository alarmLogRepository;
    Logger logger;

    public AlarmService(AlarmLogRepository alarmLogRepository, Logger logger) {
        this.alarmLogRepository = alarmLogRepository;
        this.logger = logger;
    }

    public void save(Alarm alarm) {
        alarmLogRepository.save(alarm);
    }

    public List<Alarm> loadAll() {
        return alarmLogRepository.loadAll();
    }

    public List<Alarm> loadAllActiveAlarms() {
        return alarmLogRepository.loadAllActiveAlarms();
    }
}
