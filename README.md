# Quizmaster

[![CI Status](https://github.com/nymanjens/quizmaster/actions/workflows/ci.yml/badge.svg)](https://github.com/nymanjens/quizmaster/actions)

A web-app for conducting a quiz, including a page for players to enter their answers. Lots of
question types are suported, which are configured in a YAML file.

## Screenshot

![screenshot](screenshot.png "Screenshot")

## Installation

### From prebuilt release in zip file (recommended)

- Install Java 11 (JDK 11) on your server
- Download "Binaries (compiled files) with demo configuration" from the [latest
  release](https://github.com/nymanjens/quizmaster/releases)
- Unpack the archive and open a terminal in the unpacked folder
- Run `bin/server` (UNIX) or `bin/server.bat` (Windows)
- Browse to http://localhost:9000

### Using a prebuilt docker image

- Run the following commands:

```
git clone https://github.com/nymanjens/quizmaster.git
cd quizmaster
docker-compose --file=docker-compose-prebuilt.yml up
```

- Browse to http://localhost:9000

Tip: After updating `quiz-config.yml`, you can force recreate the containers as
follows:

```
docker-compose --file=docker-compose-prebuilt.yml up --force-recreate
```

### Building and running your own release with Docker


- Run the following commands:

```
git clone https://github.com/nymanjens/quizmaster.git
cd quizmaster
docker-compose --file=docker-compose-build-locally.yml up
```
- Browse to http://localhost:9000

## Configuration

- `conf/quiz/quiz-config.yml`:<br>
  Configure your quiz here (questions, choices, answers, images, ...). The existing one in the
  release is a demo config that contains most of the options.


## Play

### How to set up

Follow these steps to host a quiz:

- Make your own quiz by editing `conf/quiz/quiz-config.yml`. You can test your quiz by starting a
  local server with it (see the installation section above)
- Host the server somewhere accessible to all players
- During a quiz, share the link to your server with all players. You
  can go to the same page and unlock the master controls via the padlock icon (if you configured a
  `masterSecret` in `quiz-config.yml`). The important pages during the quiz:
  - The player's answer submission page: This is what players use to input their answers.
  - The quiz page: This is the screen to show to all players. It shows the questions, player
    scores and plays audio and video.
  - The master page: This is a screen only for the quizmaster. It allows you to score player
    answers and generally control the quiz flow.

### Shortcuts

- **Quiz navigation**
  - `left/right`: Go to the previous/next step of the question
  - `alt + left/right`: Go to the previous/next question
  - `alt + shift + left/right`: Go to the previous/next round
- **Tools during question**
  - `spacebar`: Pause and resume the timer
  - `shift + r`: Play the current audio/video file from the beginning
  - `shift + -/o`: Subtract 30 seconds from the current timer
  - `shift + =/+/p`: Add 30 seconds from the current timer
  - `alt + enter`: Toggle enlarged image (if there is a visible image)
  - `a`: toggle the answer to be visible in the master view (http://localhost:9000/app/master)


## Git-backed quiz editor

The `/editor` page lets an editor open and save quiz YAML files and upload image or audio files into a dedicated private Git repository. Log in as `editor` with `QUIZ_EDITOR_PASSWORD`. Use the displayed media path in a quiz's `image` or `audio` field. Saving a quiz commits and pushes it; activating a quiz also restarts the application and clears the current game state.

The repository must have a `main` branch containing `quiz-config.yml`, a `quizzes/` directory of `.yml` files, and an `assets/` directory. `QUIZ_GIT_URL` must be a GitHub SSH URL, and the deployed SSH key must have write access only to this private repository. Set `QUIZ_EDITOR_ORIGIN` to the exact HTTPS origin, `QUIZ_MASTER_PASSWORD` to a distinct strong master password, and `APPLICATION_SECRET` to an independent strong random value. Supply credentials through the runtime secret store, not in the repository. If no Git URL is configured, the bundled demo configuration is used.

Quizmaster sends quiz questions and answers to participant browsers as part of normal operation. A private Git repository protects the stored files; it does not hide quiz answers from someone who inspects the application's client data.

To build and test locally, use `docker build -f Dockerfile.modernize -t quizmaster .`; the build runs the Scala test suite. The GitHub Actions workflow also tests team registration, master authentication, editor access, Git persistence, and media uploads with an isolated test repository.
