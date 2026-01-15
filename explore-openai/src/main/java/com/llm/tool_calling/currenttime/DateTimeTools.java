package com.llm.tool_calling.currenttime;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;

public class DateTimeTools {

  private static final Logger log = LoggerFactory.getLogger(DateTimeTools.class);

  @Tool(description = "Get the current date and time in the user's timezone")
  public String getCurrentDateTime(String timeZone) {
    log.info("DateTime tools is invoked - getCurrentDateTime timeZone: {} ", timeZone);

    try {
      var zoneId = ZoneId.of(timeZone);
      var zonedDateTime = ZonedDateTime.now(zoneId);
      return zonedDateTime.toString();
    } catch(Exception e) {
      log.error("Invalid timezone provided: {} ", timeZone, e);
      return "Invalid Time Zone: " + timeZone;
    }
//    return LocalDateTime
//        .now()
//        .atZone(TimeZone.getDefault().toZoneId())
//        .toString();
  }
}
