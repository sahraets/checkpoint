package com.checkpoint.backend.games;

public record GameSummary(
		int id,
		String title,
		String releaseDate,
		Integer platformId,
		String boxArtUrl) {
}
