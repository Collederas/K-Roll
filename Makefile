COMPOSE_CORE = -f compose.yaml
COMPOSE_UI   = -f compose.ui.yaml
COMPOSE_API  = -f compose.api.yaml

.PHONY: dev ui down build

dev:
	docker compose $(COMPOSE_CORE) up -d
	set KROLL_PROFILE=dev && gradlew bootRun

ui:
	docker compose $(COMPOSE_API) $(COMPOSE_UI) up

down:
	docker compose $(COMPOSE_API) $(COMPOSE_UI) down

build:
	docker compose $(COMPOSE_CORE) up -d
	gradlew buildContract bootJar && docker compose $(COMPOSE_API) build
