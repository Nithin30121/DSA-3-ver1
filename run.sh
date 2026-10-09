#!/bin/sh
# SmartCityX launcher for macOS and Linux: compiles the sources, then starts the program.
#
#   ./run.sh              interactive menu
#   ./run.sh --all        run all nine demonstrations with the sample inputs
#   ./run.sh --demo 4     run one demonstration (1-9)
#   ./run.sh --test       run the automated tests
#   ./run.sh --data       print the sample city datasets
#   ./run.sh --help       list every option
cd "$(dirname "$0")" || exit 1

# If javac is missing (or is only the macOS placeholder), try the usual Homebrew JDK locations.
if ! javac -version >/dev/null 2>&1; then
    for jdk in /opt/homebrew/opt/openjdk@21 /opt/homebrew/opt/openjdk /usr/local/opt/openjdk@21 /usr/local/opt/openjdk; do
        if [ -x "$jdk/bin/javac" ]; then
            PATH="$jdk/bin:$PATH"
            break
        fi
    done
fi
if ! javac -version >/dev/null 2>&1; then
    echo "SmartCityX needs a JDK (Java 11 or newer), but 'javac' was not found." >&2
    echo "Install one, for example with 'brew install openjdk@21', then run this script again." >&2
    exit 1
fi

# Every source file is listed, so the build never depends on old .class files.
if ! javac -d out -sourcepath . $(find . -name '*.java' -not -path './out/*'); then
    echo "Compilation failed. See the messages above." >&2
    exit 1
fi

exec java -cp out Main "$@"
