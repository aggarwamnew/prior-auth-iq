package com.priorauthiq.config;

import com.priorauthiq.matcher.DeterministicPolicyMatcher;
import com.priorauthiq.matcher.PolicyMatcher;
import com.priorauthiq.matcher.SpringAiPolicyMatcher;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the active {@link PolicyMatcher} from {@code priorauthiq.matcher}.
 *
 * <p>The rest of the application depends only on the {@code PolicyMatcher}
 * interface, so switching from the deterministic baseline to the Spring AI
 * matcher is a one-line config change and touches nothing else.
 *
 * <ul>
 *   <li>{@code deterministic} (default) — no model, no keys.</li>
 *   <li>{@code llm} — Spring AI {@link ChatClient}; requires a model-provider
 *       starter (e.g. {@code spring-ai-starter-model-openai}) and its config on
 *       the classpath to supply the {@link ChatClient.Builder}.</li>
 * </ul>
 */
@Configuration
public class MatcherConfig {

    @Bean
    @ConditionalOnProperty(name = "priorauthiq.matcher", havingValue = "deterministic", matchIfMissing = true)
    PolicyMatcher deterministicPolicyMatcher() {
        return new DeterministicPolicyMatcher();
    }

    @Bean
    @ConditionalOnProperty(name = "priorauthiq.matcher", havingValue = "llm")
    PolicyMatcher springAiPolicyMatcher(ChatClient.Builder chatClientBuilder) {
        return new SpringAiPolicyMatcher(chatClientBuilder.build());
    }
}
