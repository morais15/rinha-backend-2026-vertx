FROM --platform=linux/amd64 ghcr.io/graalvm/native-image-community:25 AS builder

WORKDIR /app
COPY . .

RUN ./mvnw -Pnative -DskipTests package


FROM --platform=linux/amd64 debian:bookworm-slim

WORKDIR /app

COPY --from=builder /app/target/backend /app/backend

EXPOSE 8080

ENTRYPOINT ["/app/backend"]
