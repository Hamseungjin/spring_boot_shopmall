package com.hsj.service;

import com.hsj.entity.EventLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogFileExporter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_HEADER =
            "id,event_type,member_id,session_id,page_url,referrer_url," +
            "target_id,target_type,metadata,ip_address,user_agent,duration_ms,created_at";

    public void exportToCsv(List<EventLog> logs, Writer writer) throws IOException {
        try (PrintWriter pw = new PrintWriter(writer)) {
            pw.println(CSV_HEADER);

            for (EventLog eventLog : logs) {
                pw.println(String.join(",",
                        str(eventLog.getId()),
                        str(eventLog.getEventType()),
                        str(eventLog.getMemberId()),
                        escapeCsv(eventLog.getSessionId()),
                        escapeCsv(eventLog.getPageUrl()),
                        escapeCsv(eventLog.getReferrerUrl()),
                        str(eventLog.getTargetId()),
                        escapeCsv(eventLog.getTargetType()),
                        escapeCsv(eventLog.getMetadata()),
                        escapeCsv(eventLog.getIpAddress()),
                        escapeCsv(eventLog.getUserAgent()),
                        str(eventLog.getDurationMs()),
                        eventLog.getCreatedAt() != null ? eventLog.getCreatedAt().format(FORMATTER) : ""
                ));
            }

            pw.flush();
        }

        log.info("이벤트 로그 CSV 내보내기 완료: {}건", logs.size());
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
