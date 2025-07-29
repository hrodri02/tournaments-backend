#!/bin/bash
# check if the java app is running
sudo lsof -i:8080
# check if the database is running
sudo lsof -i:5432
# check if mail server is running
sudo lsof -i:1080