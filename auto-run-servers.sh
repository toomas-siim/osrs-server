#!/bin/bash

# Kill all Java processes
killall java

# Start Management-Server in a detached screen session named 'management-server'
screen -dmS management-server ./gradlew :Management-Server:run

# Start Server in a detached screen session named 'server'
screen -dmS server ./gradlew :Server:run
