FROM maven:3.9.9-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY demo-handler/pom.xml demo-handler/pom.xml
COPY demo-order/pom.xml demo-order/pom.xml
COPY demo-handler/src demo-handler/src
RUN mvn -q -pl demo-handler -am -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /opt/skywalking-agent \
  && curl -fL https://archive.apache.org/dist/skywalking/java-agent/8.9.0/apache-skywalking-java-agent-8.9.0.tgz -o /tmp/agent.tgz \
  && tar -xzf /tmp/agent.tgz -C /tmp \
  && cp -R /tmp/skywalking-agent/* /opt/skywalking-agent/ \
  && rm -rf /tmp/agent.tgz /tmp/skywalking-agent
COPY --from=build /workspace/demo-handler/target/demo-handler-1.0.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -javaagent:/opt/skywalking-agent/skywalking-agent.jar -Dskywalking.agent.service_name=${SW_AGENT_NAME:-demo-handler} -Dskywalking.collector.backend_service=${SW_OAP_ADDR:-oap:11800} -DQYWX_WEBHOOK_URL=${QYWX_WEBHOOK_URL:-} -jar /app/app.jar"]
