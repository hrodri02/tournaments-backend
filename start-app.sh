#!/bin/bash
# starts mail server in the background
maildev &
# if database is not running
if ! systemctl is-active --quiet postgresql; then
    # start database
    sudo systemctl start postgresql
fi
# starts tournaments backend app in the background
java -jar target/tournaments-backend-0.0.1-SNAPSHOT.jar &