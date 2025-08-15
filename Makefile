config ?= compileClasspath

#
# Build the Nextflow plugin
#
compile:
	./gradlew assemble

#
# Clean and build the plugin
#
build:
	./gradlew build

#
# Clean build artifacts
#
clean:
	./gradlew clean

#
# Run tests
#
test:
	./gradlew test

#
# Run checks (test + lint)
#
check:
	./gradlew check

#
# Install plugin locally for testing (extract to development directory)
#
install: build
	cp build/distributions/*.zip ~/.nextflow/plugins/
	@echo "Also installing to development directory for testing with launch.sh..."
	rm -rf /Users/edmundmiller/.worktrees/nextflow/cli-extension/plugins/nf-wave-cli
	cd /Users/edmundmiller/.worktrees/nextflow/cli-extension/plugins && unzip -q ~/.nextflow/plugins/nf-wave-cli-*.zip -d nf-wave-cli
	cd /Users/edmundmiller/.worktrees/nextflow/cli-extension/plugins/nf-wave-cli && mkdir -p build/classes/main build/target/libs
	cd /Users/edmundmiller/.worktrees/nextflow/cli-extension/plugins/nf-wave-cli && cp -r classes/* build/classes/main/
	cd /Users/edmundmiller/.worktrees/nextflow/cli-extension/plugins/nf-wave-cli && mv lib build/target/libs
	@echo "Plugin installed for both runtime and development testing"

#
# Package the plugin for distribution
#
package:
	./gradlew packagePlugin

#
# Show dependencies
#
deps:
	./gradlew -q dependencies --configuration ${config}

#
# Show plugin information
#
info:
	@echo "Plugin: nf-wave-cli"
	@echo "Version: $(shell cat VERSION)"
	@echo "Built plugin: $(shell ls -1 build/distributions/*.zip 2>/dev/null || echo 'Not built yet')"

#
# Help target
#
help:
	@echo "Available targets:"
	@echo "  compile   - Build the plugin"
	@echo "  build     - Clean and build the plugin"
	@echo "  clean     - Clean build artifacts"
	@echo "  test      - Run tests"
	@echo "  check     - Run tests and checks"
	@echo "  install   - Build and install plugin locally"
	@echo "  package   - Package plugin for distribution"
	@echo "  deps      - Show dependencies"
	@echo "  info      - Show plugin information"
	@echo "  help      - Show this help message"
	@echo ""
	@echo "CURRENT STATUS:"
	@echo "This plugin is ready for first-class CLI integration but requires:"
	@echo "1. Nextflow version with CommandExtensionPoint support (cli-extension branch)"
	@echo "2. The CommandExtensionPoint interface to be available at compile time"
	@echo ""
	@echo "For now, the plugin provides traditional Wave CLI functionality via existing interfaces."

.PHONY: compile build clean test check install package deps info help
