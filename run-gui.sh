#!/bin/bash
# Launch script for the GUI version with proper Java module access
java --add-exports java.desktop/sun.java2d=ALL-UNNAMED \
     -jar target/uberjar/a_game-standalone.jar "$@"
