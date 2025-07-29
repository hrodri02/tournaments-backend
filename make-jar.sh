#!/bin/bash
# if database is not running
if ! systemctl is-active --quiet postgresql; then
    # start database
    sudo systemctl start postgresql
fi
# make jar file
mvn clean package