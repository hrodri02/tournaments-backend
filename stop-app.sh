#!/bin/bash
# stop the spring app
ID=$(sudo lsof -i:8080 | sed -n '2p' | awk '{print $2}')
sudo kill -9 $ID
# stop the database
sudo systemctl stop postgresql
# stop the mail server
ID=$(sudo lsof -i:1080 | sed -n '2p' | awk '{print $2}')
sudo kill -9 $ID