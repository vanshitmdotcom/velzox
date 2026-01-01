package com.velzox.apimonitor.exception;

import org.springframework.http.HttpStatus;

/**
 * Plan Limit Exceeded Exception - Thrown when user exceeds their plan limits
 * 
 * Examples:
 * - Trying to add more endpoints than plan allows
 * - Trying to set check interval below plan minimum
 * - Trying to use Slack alerts on Free plan
 */
public class PlanLimitExceededException extends ApiException {

    public PlanLimitExceededException(String message) {
        super(message, HttpStatus.FORBIDDEN, "PLAN_LIMIT_EXCEEDED");
    }

    public PlanLimitExceededException(String limit, int current, int max) {
        super(String.format("%s limit exceeded: %d/%d. Please upgrade your plan.", 
              limit, current, max), HttpStatus.FORBIDDEN, "PLAN_LIMIT_EXCEEDED");
    }

    public static PlanLimitExceededException endpointLimit(int current, int max) {
        return new PlanLimitExceededException("Endpoint", current, max);
    }

    public static PlanLimitExceededException checkIntervalLimit(int requested, int minAllowed) {
        return new PlanLimitExceededException(
            String.format("Check interval of %d seconds not allowed. Minimum for your plan: %d seconds", 
                         requested, minAllowed));
    }

    public static PlanLimitExceededException featureNotAvailable(String feature) {
        return new PlanLimitExceededException(
            String.format("%s is not available on your plan. Please upgrade.", feature));
    }
}
