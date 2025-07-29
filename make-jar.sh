#!/bin/bash
# check if database is running
if systemctl is-active --quiet postgresql; then
    echo "PostgreSQL is running."
else
    # start the database if it is not running
    echo "PostgreSQL is not running or failed to start."
fi