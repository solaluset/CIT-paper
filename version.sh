#!/bin/sh

root=$(realpath "$(dirname "$0")")
build_gradle=$root/build.gradle
plugin_yml=$root/src/main/resources/plugin.yml
cli_build_gradle_kts=$root/cli/build.gradle.kts

current_version=$(grep '^version:' "$plugin_yml" | sed -E "s/[^']*'(.*)'/\\1/")

case "$1" in
"set")
  if [ "$2" != "" ]; then
    echo "Updating version from $current_version to $2"
    sed -E -i 's/^version:.*/'"version: '$2'/" "$plugin_yml"
    sed -E -i 's/^version\s*=.*/'"version = '$2'/" "$build_gradle"
    sed -E -i -e 's/^version\s*=.*/'"version = \"$2\"/" \
      -e 's/^val citPaper\s*=.*/'"val citPaper = \"org.vinerdream:CIT-paper:$2\"/" "$cli_build_gradle_kts"
  else
    echo "Please specify the version"
  fi
  ;;
*)
  echo "Current version: $current_version"
  ;;
esac
