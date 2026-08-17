package com.checkpoint.backend.games;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class GamesServiceTests {

	private static final String BASE_URL = "https://api.thegamesdb.test/v1";

	private RestClient.Builder builder;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		this.builder = RestClient.builder();
		this.server = MockRestServiceServer.bindTo(this.builder).build();
	}

	private GamesService serviceWithKey(String apiKey) {
		return new GamesService(this.builder, BASE_URL, apiKey);
	}

	@Test
	void rejectedApiKeyBecomesBadGatewayWithUpstreamReason() {
		server.expect(requestTo(Matchers.containsString("/Games/ByGameName")))
				.andRespond(withStatus(HttpStatus.FORBIDDEN)
						.contentType(MediaType.APPLICATION_JSON)
						.body("""
								{"code":403,"status":"Invalid API key was provided.",
								 "remaining_monthly_allowance":0}
								"""));

		assertThatThrownBy(() -> serviceWithKey("bad-key").search("zelda"))
				.isInstanceOf(ResponseStatusException.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ResponseStatusException.class))
				.satisfies(ex -> {
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
					assertThat(ex.getReason())
							.contains("rejected the API key")
							.contains("Invalid API key was provided.");
				});

		server.verify();
	}

	@Test
	void upstreamServerErrorBecomesBadGateway() {
		server.expect(requestTo(Matchers.containsString("/Games/ByGameName")))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		assertThatThrownBy(() -> serviceWithKey("good-key").search("zelda"))
				.isInstanceOf(ResponseStatusException.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ResponseStatusException.class))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));

		server.verify();
	}

	@Test
	void blankApiKeyFailsBeforeCallingUpstream() {
		assertThatThrownBy(() -> serviceWithKey("").search("zelda"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("not configured");

		// No upstream request should have been attempted.
		server.verify();
	}

	@Test
	void successfulSearchMapsGamesAndFrontBoxArt() {
		server.expect(requestTo(Matchers.containsString("/Games/ByGameName")))
				.andRespond(withSuccess("""
						{
						  "status": "Success",
						  "data": {
						    "games": [
						      {"id": 1, "game_title": "Zelda", "release_date": "1986-02-21", "platform": 4},
						      {"id": 2, "game_title": "Zelda II", "release_date": null, "platform": null}
						    ]
						  },
						  "include": {
						    "boxart": {
						      "base_url": {"thumb": "https://cdn.test/thumb/"},
						      "data": {
						        "1": [
						          {"side": "back", "filename": "boxart/back/1.jpg"},
						          {"side": "front", "filename": "boxart/front/1.jpg"}
						        ]
						      }
						    }
						  }
						}
						""", MediaType.APPLICATION_JSON));

		List<GameSummary> results = serviceWithKey("good-key").search("zelda");

		assertThat(results).hasSize(2);

		assertThat(results.get(0).id()).isEqualTo(1);
		assertThat(results.get(0).title()).isEqualTo("Zelda");
		assertThat(results.get(0).releaseDate()).isEqualTo("1986-02-21");
		assertThat(results.get(0).platformId()).isEqualTo(4);
		// "front" is picked over the "back" image that appears first in the array.
		assertThat(results.get(0).boxArtUrl()).isEqualTo("https://cdn.test/thumb/boxart/front/1.jpg");

		// No box art entry for this id, so the URL stays null rather than breaking the row.
		assertThat(results.get(1).title()).isEqualTo("Zelda II");
		assertThat(results.get(1).boxArtUrl()).isNull();

		server.verify();
	}
}