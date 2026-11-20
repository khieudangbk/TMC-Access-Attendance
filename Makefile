SHELL := /bin/bash
COMPOSE := docker compose -f infra/docker-compose.yml

ifeq (,$(wildcard .env))
else
include .env
export
endif

.PHONY: help setup up up-infra down reset-db logs ps db-shell backend edge web test build lint verify

help:
	@echo "TMC-AACS local commands"
	@echo "  make setup      Copy .env.example to .env when missing, install deps"
	@echo "  make up         Build & run all containers (API, Web, Edge, Postgres, Adminer, Mailpit)"
	@echo "  make up-infra   Start only infra containers (PostgreSQL, Adminer, Mailpit)"
	@echo "  make down       Stop local containers"
	@echo "  make reset-db   Destroy and recreate local database volumes"
	@echo "  make backend    Run central API on :8080 (on host)"
	@echo "  make edge       Run edge agent on :8081 (on host)"
	@echo "  make web        Run web portal on :5173 (on host)"
	@echo "  make test       Run backend and frontend tests"
	@echo "  make build      Build backend jars and frontend bundle"
	@echo "  make verify     Build + test + lint"

setup:
	@test -f .env || cp .env.example .env
	cd frontend && npm install
	cd backend && ./mvnw -B -ntp dependency:go-offline

up:
	$(COMPOSE) up -d --build

up-infra:
	$(COMPOSE) up -d postgres adminer mailpit

down:
	$(COMPOSE) down

reset-db:
	$(COMPOSE) down -v
	$(COMPOSE) up -d postgres

logs:
	$(COMPOSE) logs -f --tail=100

ps:
	$(COMPOSE) ps

db-shell:
	$(COMPOSE) exec postgres psql -U tmc -d tmc_central

backend:
	cd backend && ./mvnw -B -ntp -pl central-api spring-boot:run

edge:
	cd backend && ./mvnw -B -ntp -pl edge-agent spring-boot:run

web:
	cd frontend && npm run dev

test:
	cd backend && ./mvnw -B -ntp test
	cd frontend && npm test

build:
	cd backend && ./mvnw -B -ntp clean package
	cd frontend && npm run build

lint:
	cd frontend && npm run lint

verify: build test lint
