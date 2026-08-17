package com.checkpoint.backend.games;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GamesController {

	private final GamesService gamesService;

	public GamesController(GamesService gamesService) {
		this.gamesService = gamesService;
	}

	@GetMapping("/search")
	public List<GameSummary> search(@RequestParam String q) {
		return gamesService.search(q);
	}
}
