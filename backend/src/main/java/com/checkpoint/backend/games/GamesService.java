package com.checkpoint.backend.games;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import tools.jackson.databind.JsonNode;

/**
 * Thin wrapper around TheGamesDB's v1 API. Only what the frontend needs
 * (search by name, with box art) is exposed here.
 */
@Service
public class GamesService {

	private final RestClient restClient;
	private final String apiKey;

	public GamesService(
			RestClient.Builder restClientBuilder,
			@Value("${thegamesdb.base-url}") String baseUrl,
			@Value("${thegamesdb.api-key}") String apiKey) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
		this.apiKey = apiKey;
	}

	public List<GameSummary> search(String query) {
		if (apiKey.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"TheGamesDB API key is not configured on the backend");
		}

		JsonNode root;
		try {
			root = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/Games/ByGameName")
							.queryParam("apikey", apiKey)
							.queryParam("name", query)
							.queryParam("include", "boxart")
							.build())
					.retrieve()
					.body(JsonNode.class);
		}
		catch (RestClientResponseException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, upstreamReason(ex), ex);
		}

		if (root == null || !"Success".equals(root.path("status").asText())) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TheGamesDB request failed");
		}

		JsonNode games = root.path("data").path("games");
		JsonNode boxartData = root.path("include").path("boxart").path("data");
		String thumbBase = root.path("include").path("boxart").path("base_url").path("thumb").asText(null);

		List<GameSummary> results = new ArrayList<>();
		for (JsonNode game : games) {
			int id = game.path("id").asInt();
			String title = game.path("game_title").asText(null);
			String releaseDate = game.path("release_date").asText(null);
			Integer platformId = game.hasNonNull("platform") ? game.path("platform").asInt() : null;
			String boxArtUrl = findFrontBoxArt(boxartData.path(String.valueOf(id)), thumbBase);

			results.add(new GameSummary(id, title, releaseDate, platformId, boxArtUrl));
		}

		return results;
	}

	/**
	 * Turns an upstream error into something actionable. TheGamesDB puts a
	 * human-readable explanation in the body's "status" field, e.g.
	 * {@code {"code":403,"status":"Invalid API key was provided."}}.
	 */
	private String upstreamReason(RestClientResponseException ex) {
		String detail = null;
		try {
			JsonNode body = ex.getResponseBodyAs(JsonNode.class);
			if (body != null) {
				detail = body.path("status").asText(null);
			}
		}
		catch (RuntimeException ignored) {
			// Body was not JSON we can read; fall back to the status code alone.
		}

		HttpStatusCode status = ex.getStatusCode();
		String prefix = (status.value() == 401 || status.value() == 403)
				? "TheGamesDB rejected the API key"
				: "TheGamesDB request failed (" + status.value() + ")";

		return (detail == null || detail.isBlank()) ? prefix : prefix + ": " + detail;
	}

	private String findFrontBoxArt(JsonNode imagesForGame, String thumbBase) {
		if (!imagesForGame.isArray() || thumbBase == null) {
			return null;
		}
		for (JsonNode image : imagesForGame) {
			if ("front".equals(image.path("side").asText())) {
				return thumbBase + image.path("filename").asText();
			}
		}
		return null;
	}
}
