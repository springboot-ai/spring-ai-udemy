package com.llm.tool_calling.currenttime;

import java.time.LocalDateTime;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;

public class DateTimeTools {

  private static final Logger log = LoggerFactory.getLogger(DateTimeTools.class);

  @Tool(description = "Get the current date and time in the user's timezone")
  public String getCurrentDateTime() {
    log.info("DateTime tools is invoked - getCurrentDateTime ");
    return LocalDateTime
        .now()
        .atZone(TimeZone.getDefault().toZoneId())
        .toString();
  }
}
