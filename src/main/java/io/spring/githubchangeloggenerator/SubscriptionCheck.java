/*
 * Copyright 2018-2024 the original author or authors.
 * Copyright 2026 StepSecurity
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.githubchangeloggenerator;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates the StepSecurity maintained-actions subscription before the action runs.
 *
 * @author StepSecurity Team
 */
public final class SubscriptionCheck {

	private static final String UPSTREAM = "steven-sheehy/github-changelog-generator";

	private static final String DOCS_URL = "https://docs.stepsecurity.io/actions/stepsecurity-maintained-actions";

	private SubscriptionCheck() {
	}

	public static void validate() {
		String eventPath = System.getenv("GITHUB_EVENT_PATH");
		Boolean repoPrivate = null;

		if (eventPath != null) {
			try {
				File eventFile = new File(eventPath);
				if (eventFile.exists()) {
					ObjectMapper mapper = new ObjectMapper();
					JsonNode root = mapper.readTree(eventFile);
					JsonNode privateNode = root.path("repository").path("private");
					if (!privateNode.isMissingNode() && !privateNode.isNull()) {
						repoPrivate = privateNode.asBoolean();
					}
				}
			}
			catch (Exception ignored) {
			}
		}

		String action = System.getenv("GITHUB_ACTION_REPOSITORY");
		if (action == null) {
			action = "";
		}

		System.out.println("");
		System.out.println("\u001b[1;36mStepSecurity Maintained Action\u001b[0m");
		System.out.println("Secure drop-in replacement for " + UPSTREAM);
		if (Boolean.FALSE.equals(repoPrivate)) {
			System.out.println("\u001b[32m✓ Free for public repositories\u001b[0m");
		}
		System.out.println("\u001b[36mLearn more:\u001b[0m " + DOCS_URL);
		System.out.println("");

		if (Boolean.FALSE.equals(repoPrivate)) {
			return;
		}

		String serverUrl = System.getenv("GITHUB_SERVER_URL");
		if (serverUrl == null || serverUrl.isEmpty()) {
			serverUrl = "https://github.com";
		}
		String repo = System.getenv("GITHUB_REPOSITORY");
		if (repo == null) {
			repo = "";
		}

		String body;
		if (!"https://github.com".equals(serverUrl)) {
			body = String.format("{\"action\":\"%s\",\"ghes_server\":\"%s\"}", action, serverUrl);
		}
		else {
			body = String.format("{\"action\":\"%s\"}", action);
		}

		String apiUrl = "https://agent.api.stepsecurity.io/v1/github/" + repo
				+ "/actions/maintained-actions-subscription";

		try {
			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.timeout(Duration.ofSeconds(3))
				.build();
			HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
			if (response.statusCode() == 403) {
				System.err.println(
						"::error::\u001b[1;31mThis action requires a StepSecurity subscription for private repositories.\u001b[0m");
				System.err.println("::error::\u001b[31mLearn how to enable a subscription: " + DOCS_URL + "\u001b[0m");
				System.exit(1);
			}
		}
		catch (Exception ex) {
			System.out.println("Timeout or API not reachable. Continuing to next step.");
		}
	}

}
