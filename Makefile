COMPOSE_DB = -f compose.db.yaml
COMPOSE_UI   = -f compose.ui.yaml
COMPOSE_API  = -f compose.api.yaml

.PHONY: dev ui down build

dev:
	docker compose $(COMPOSE_DB) up -d
	set KROLL_PROFILE=dev && gradlew bootRun

api:
	docker compose $(COMPOSE_API) up

ui:
	docker compose $(COMPOSE_UI) up

down:
	docker compose $(COMPOSE_API) $(COMPOSE_UI) down

build:
	docker compose $(COMPOSE_DB) up -d
	gradlew bootJar && docker compose $(COMPOSE_API) build
