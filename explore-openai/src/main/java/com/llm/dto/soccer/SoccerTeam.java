package com.llm.dto.soccer;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"team", "players"})
public record SoccerTeam(String team, List<String> players) {
}
