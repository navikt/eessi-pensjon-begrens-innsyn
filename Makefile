OCKER  := docker
GRADLE  := ./gradlew
NAIS    := nais
GIT     := git
VERSION := $(shell cat ./VERSION)
REGISTRY:= repo.adeo.no:5443
MODULE := $(notdir $(shell pwd))

.PHONY: all build test docker docker-push release manifest

all: build test docker
release: tag docker-push

build:
	$(GRADLE) assemble

test:
	$(GRADLE) check

REPO := navikt/$(MODULE)
PROJECT := EP

whats-new: 		## Show diff between where you are and production specified by PROD_VERSION=xx
	@git fetch
	@echo "--------------------------------------------------------------------------------------------------"
	@echo
	@(git describe --all | awk -F "/" '{print "*ENDRINGER - $(PROJECT) - $(REPO) - " $$NF "*"}')
	@echo
	@git describe --dirty=" - *DU HAR FILER SOM IKKE ER COMMITET!!*"
	@echo
	@echo "Oppgaver påvirket:"
	@echo "\`\`\`"
	@git log tags/$(PROD_VERSION)..HEAD | tr '\n' ' '| egrep --o "$(PROJECT)-[[:digit:]]+" | sort | uniq | awk '{print "• https://jira.adeo.no/browse/" $$1 }'
	@echo "\`\`\`"
	@echo
	@echo "Endringer:"
	@echo "\`\`\`"
	@git log --oneline --reverse tags/$(PROD_VERSION)..HEAD | grep -v "\[skip ci\]" | grep -v "Merge pull request" | awk '{print "• https://github.com/$(REPO)/commit/" $$0}'
	@echo "\`\`\`"
	@echo
	@echo "Filer som kan påvirke produksjon:"
	@echo "\`\`\`"
	@git diff --name-only tags/$(PROD_VERSION)..HEAD\
	     | egrep -v "Makefile|\/test\/|VERSION|application-local.yml|.github|nais/dev"\
	     | sed "s/^/• /"
	@echo "\`\`\`"
	@echo
	@echo "--------------------------------------------------------------------------------------------------"
