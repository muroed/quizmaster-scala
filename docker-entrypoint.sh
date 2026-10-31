#!/bin/sh
set -eu
if [ -n "${QUIZ_GIT_URL:-}" ]; then
  case "$QUIZ_GIT_URL" in git@github.com:*.git) ;; *) echo 'Unsupported quiz repository URL' >&2; exit 1;; esac
  QUIZ_GIT_DIR=${QUIZ_GIT_DIR:-/data/quiz}
  export QUIZ_GIT_DIR
  export GIT_TERMINAL_PROMPT=0
  if [ ! -d "$QUIZ_GIT_DIR/.git" ]; then
    mkdir -p "$(dirname "$QUIZ_GIT_DIR")"
    git clone --branch main -- "$QUIZ_GIT_URL" "$QUIZ_GIT_DIR"
  else
    git -C "$QUIZ_GIT_DIR" fetch origin main
    git -C "$QUIZ_GIT_DIR" merge --ff-only origin/main
  fi
  test -f "$QUIZ_GIT_DIR/quiz-config.yml"
  export QUIZ_CONFIG_PATH="$QUIZ_GIT_DIR/quiz-config.yml"
fi
exec "$@"
