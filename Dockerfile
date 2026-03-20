 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.7

# Application image

FROM hmctsprod.azurecr.io/base/java:17-distroless

# Change to non-root privilege
USER hmcts

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/ia-timed-event-service.jar /opt/app/

EXPOSE 8095
CMD [ "ia-timed-event-service.jar" ]
