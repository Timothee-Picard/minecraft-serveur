# Fichiers compose
COMPOSE_FILE := infra/docker-compose.yml

# Raccourci docker compose
DC := docker compose -f $(COMPOSE_FILE)

# Service docker compose du serveur MC
MC_SERVICE := minecraft

# Par défaut : affiche l’aide
.DEFAULT_GOAL := help

.PHONY: help
help: ## Affiche cette aide
	@echo "Commandes disponibles :"
	@echo "  make up                - Démarre les services en arrière-plan"
	@echo "  make down              - Stoppe et supprime les services"
	@echo "  make restart           - Redémarre les services"
	@echo "  make ps                - Liste l'état des services"
	@echo "  make logs              - Logs de tous les services (follow)"
	@echo "  make logs-mc           - Logs du serveur Minecraft (follow)"
	@echo "  make mc-attach         - Attache la console MC (Ctrl+P puis Ctrl+Q pour détacher)"
	@echo "  make mc-cmd <cmd...>   - Envoie une commande console MC (mc-send-to-console)"
	@echo "  make build             - (re)build si images locales"
	@echo "  make pull              - pull des images"
	@echo "  make prune             - Nettoie volumes/réseaux orphelins (dangereux)"
	@echo ""
	@echo "Variables utiles :"
	@echo "  COMPOSE_FILE=$(COMPOSE_FILE)"
	@echo "  MC_SERVICE=$(MC_SERVICE)"

.PHONY: up
up: ## Démarre les services en mode détaché
	$(DC) up -d

.PHONY: down
down: ## Stoppe & supprime les services, garde les volumes
	$(DC) down

.PHONY: restart
restart: ## Redémarre les services
	$(DC) down
	$(DC) up -d

.PHONY: ps
ps: ## Affiche l'état des services
	$(DC) ps

.PHONY: build
build: ## Rebuild les images locales (si Dockerfile)
	$(DC) build

.PHONY: pull
pull: ## Pull les images distantes
	$(DC) pull

.PHONY: logs
logs: ## Suivi des logs de tous les services
	$(DC) logs -f --tail=200

.PHONY: logs-mc
logs-mc: ## Suivi des logs du serveur Minecraft
	$(DC) logs -f --tail=200 $(MC_SERVICE)

# --- Console Minecraft ----------------------------------------

# Attache interactif (pour taper des commandes directement)
.PHONY: mc-attach
mc-attach: ## Attache la console Minecraft (Ctrl+P puis Ctrl+Q pour détacher)
	$(DC) attach $(MC_SERVICE)

# Envoi d'une commande à la console (ex: make mc-cmd list)
.PHONY: mc-cmd
mc-cmd:
	@cmd="$(wordlist 2, 99, $(MAKECMDGOALS))"; \
	if [ -z "$$cmd" ]; then \
	  echo "Usage: make mc-cmd <commande...>"; \
	  echo "Exemples: make mc-cmd list   |   make mc-cmd say Hello"; \
	  exit 1; \
	fi; \
	cid="$$( $(DC) ps -q $(MC_SERVICE) )"; \
	test -n "$$cid" || (echo "Conteneur $(MC_SERVICE) introuvable"; exit 1); \
	docker exec -u 1000 -i $$cid mc-send-to-console "$$cmd"

.PHONY: prune
prune: ## Nettoie volumes et réseaux non utilisés (dangereux)
	@echo "⚠️  Cette commande peut supprimer des volumes. Ctrl+C pour annuler."
	sleep 3
	docker system prune -f

# Règle attrape-tout (empêche make de croire que "list" est une vraie cible)
%:
	@:
